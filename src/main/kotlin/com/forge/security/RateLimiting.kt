package com.forge.security

import com.forge.core.Context
import com.forge.routing.Handler
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Rate limiting strategy
 */
enum class RateLimitStrategy {
    FIXED_WINDOW,    // Fixed time window
    SLIDING_WINDOW,  // Sliding time window
    TOKEN_BUCKET     // Token bucket algorithm
}

/**
 * Rate limit configuration
 */
data class RateLimitConfig(
    val strategy: RateLimitStrategy = RateLimitStrategy.FIXED_WINDOW,
    val requestsPerWindow: Int = 100,
    val windowDuration: Duration = Duration.ofMinutes(1),
    val keyExtractor: (Context) -> String = { ctx -> ctx.req().remoteAddr ?: "unknown" },
    val onRateLimitExceeded: ((Context, RateLimitInfo) -> Unit)? = null
)

/**
 * Rate limit information
 */
data class RateLimitInfo(
    val key: String,
    val requestsRemaining: Int,
    val resetTime: Instant,
    val retryAfter: Duration?
)

/**
 * Rate limiter interface
 */
interface RateLimiter {
    fun isAllowed(key: String): Boolean
    fun getRateLimitInfo(key: String): RateLimitInfo
    fun reset(key: String)
    fun cleanup()
}

/**
 * Fixed window rate limiter
 */
class FixedWindowRateLimiter(
    private val requestsPerWindow: Int,
    private val windowDuration: Duration
) : RateLimiter {
    
    private data class WindowData(
        val windowStart: Instant,
        val requestCount: AtomicInteger
    )
    
    private val windows = ConcurrentHashMap<String, WindowData>()
    
    override fun isAllowed(key: String): Boolean {
        val now = Instant.now()
        val window = windows.compute(key) { _, existing ->
            if (existing == null || isWindowExpired(existing.windowStart, now)) {
                WindowData(now, AtomicInteger(1))
            } else {
                existing.requestCount.incrementAndGet()
                existing
            }
        }!!
        
        return window.requestCount.get() <= requestsPerWindow
    }
    
    override fun getRateLimitInfo(key: String): RateLimitInfo {
        val now = Instant.now()
        val window = windows[key]
        
        return if (window == null || isWindowExpired(window.windowStart, now)) {
            RateLimitInfo(
                key = key,
                requestsRemaining = requestsPerWindow,
                resetTime = now.plus(windowDuration),
                retryAfter = null
            )
        } else {
            val remaining = maxOf(0, requestsPerWindow - window.requestCount.get())
            val resetTime = window.windowStart.plus(windowDuration)
            val retryAfter = if (remaining <= 0) Duration.between(now, resetTime) else null
            
            RateLimitInfo(
                key = key,
                requestsRemaining = remaining,
                resetTime = resetTime,
                retryAfter = retryAfter
            )
        }
    }
    
    override fun reset(key: String) {
        windows.remove(key)
    }
    
    override fun cleanup() {
        val now = Instant.now()
        windows.entries.removeIf { (_, window) ->
            isWindowExpired(window.windowStart, now)
        }
    }
    
    private fun isWindowExpired(windowStart: Instant, now: Instant): Boolean {
        return Duration.between(windowStart, now) >= windowDuration
    }
}

/**
 * Sliding window rate limiter
 */
class SlidingWindowRateLimiter(
    private val requestsPerWindow: Int,
    private val windowDuration: Duration
) : RateLimiter {
    
    private data class RequestRecord(val timestamp: Instant)
    
    private val requestHistory = ConcurrentHashMap<String, MutableList<RequestRecord>>()
    
    override fun isAllowed(key: String): Boolean {
        val now = Instant.now()
        val history = requestHistory.computeIfAbsent(key) { mutableListOf() }
        
        synchronized(history) {
            // Remove expired requests
            val cutoff = now.minus(windowDuration)
            history.removeIf { it.timestamp.isBefore(cutoff) }
            
            return if (history.size < requestsPerWindow) {
                history.add(RequestRecord(now))
                true
            } else {
                false
            }
        }
    }
    
    override fun getRateLimitInfo(key: String): RateLimitInfo {
        val now = Instant.now()
        val history = requestHistory[key] ?: mutableListOf()
        
        synchronized(history) {
            val cutoff = now.minus(windowDuration)
            history.removeIf { it.timestamp.isBefore(cutoff) }
            
            val remaining = maxOf(0, requestsPerWindow - history.size)
            val oldestRequest = history.minByOrNull { it.timestamp }
            val resetTime = oldestRequest?.timestamp?.plus(windowDuration) ?: now
            val retryAfter = if (remaining <= 0 && oldestRequest != null) {
                Duration.between(now, oldestRequest.timestamp.plus(windowDuration))
            } else null
            
            return RateLimitInfo(
                key = key,
                requestsRemaining = remaining,
                resetTime = resetTime,
                retryAfter = retryAfter
            )
        }
    }
    
    override fun reset(key: String) {
        requestHistory.remove(key)
    }
    
    override fun cleanup() {
        val now = Instant.now()
        val cutoff = now.minus(windowDuration)
        
        requestHistory.values.forEach { history ->
            synchronized(history) {
                history.removeIf { it.timestamp.isBefore(cutoff) }
            }
        }
        
        requestHistory.entries.removeIf { (_, history) ->
            synchronized(history) { history.isEmpty() }
        }
    }
}

/**
 * Token bucket rate limiter
 */
class TokenBucketRateLimiter(
    private val capacity: Int,
    private val refillRate: Int, // tokens per second
    private val windowDuration: Duration = Duration.ofMinutes(1)
) : RateLimiter {
    
    private data class Bucket(
        val tokens: AtomicLong,
        val lastRefill: AtomicLong // timestamp in milliseconds
    )
    
    private val buckets = ConcurrentHashMap<String, Bucket>()
    
    override fun isAllowed(key: String): Boolean {
        val now = System.currentTimeMillis()
        val bucket = buckets.computeIfAbsent(key) {
            Bucket(AtomicLong(capacity.toLong()), AtomicLong(now))
        }
        
        refillBucket(bucket, now)
        
        return bucket.tokens.get() > 0 && bucket.tokens.decrementAndGet() >= 0
    }
    
    override fun getRateLimitInfo(key: String): RateLimitInfo {
        val now = System.currentTimeMillis()
        val bucket = buckets[key]
        
        return if (bucket == null) {
            RateLimitInfo(
                key = key,
                requestsRemaining = capacity,
                resetTime = Instant.ofEpochMilli(now).plus(windowDuration),
                retryAfter = null
            )
        } else {
            refillBucket(bucket, now)
            val remaining = bucket.tokens.get().toInt()
            val nextRefillTime = if (remaining <= 0) {
                val tokensNeeded = 1
                val secondsToWait = (tokensNeeded.toDouble() / refillRate).toLong()
                Instant.ofEpochMilli(now + secondsToWait * 1000)
            } else {
                Instant.ofEpochMilli(now).plus(windowDuration)
            }
            
            val retryAfter = if (remaining <= 0) {
                Duration.ofSeconds((1.0 / refillRate).toLong())
            } else null
            
            RateLimitInfo(
                key = key,
                requestsRemaining = remaining,
                resetTime = nextRefillTime,
                retryAfter = retryAfter
            )
        }
    }
    
    override fun reset(key: String) {
        buckets.remove(key)
    }
    
    override fun cleanup() {
        val now = System.currentTimeMillis()
        val cutoff = now - windowDuration.toMillis() * 2
        
        buckets.entries.removeIf { (_, bucket) ->
            bucket.lastRefill.get() < cutoff
        }
    }
    
    private fun refillBucket(bucket: Bucket, now: Long) {
        val lastRefill = bucket.lastRefill.get()
        val timePassed = now - lastRefill
        
        if (timePassed > 0) {
            val tokensToAdd = (timePassed * refillRate / 1000).toLong()
            if (tokensToAdd > 0) {
                val currentTokens = bucket.tokens.get()
                val newTokens = minOf(capacity.toLong(), currentTokens + tokensToAdd)
                bucket.tokens.set(newTokens)
                bucket.lastRefill.set(now)
            }
        }
    }
}

/**
 * Rate limiting middleware
 */
class RateLimitingMiddleware(
    private val rateLimiter: RateLimiter,
    private val keyExtractor: (Context) -> String = { ctx -> ctx.req().remoteAddr ?: "unknown" },
    private val onRateLimitExceeded: ((Context, RateLimitInfo) -> Unit)? = null
) : Handler {
    
    override fun handle(ctx: Context) {
        val key = keyExtractor(ctx)
        val rateLimitInfo = rateLimiter.getRateLimitInfo(key)
        
        // Add rate limit headers
        ctx.header("X-RateLimit-Limit", rateLimitInfo.requestsRemaining.toString())
        ctx.header("X-RateLimit-Remaining", rateLimitInfo.requestsRemaining.toString())
        ctx.header("X-RateLimit-Reset", rateLimitInfo.resetTime.epochSecond.toString())
        
        if (!rateLimiter.isAllowed(key)) {
            handleRateLimitExceeded(ctx, rateLimitInfo)
        }
    }
    
    private fun handleRateLimitExceeded(ctx: Context, rateLimitInfo: RateLimitInfo) {
        if (onRateLimitExceeded != null) {
            onRateLimitExceeded.invoke(ctx, rateLimitInfo)
        } else {
            if (rateLimitInfo.retryAfter != null) {
                ctx.header("Retry-After", rateLimitInfo.retryAfter.seconds.toString())
            }
            
            ctx.status(429).json(mapOf(
                "error" to "Too Many Requests",
                "message" to "Rate limit exceeded",
                "retryAfter" to rateLimitInfo.retryAfter?.seconds
            ))
        }
    }
}

/**
 * Rate limit configuration builder
 */
class RateLimitConfigBuilder {
    private var strategy: RateLimitStrategy = RateLimitStrategy.FIXED_WINDOW
    private var requestsPerWindow: Int = 100
    private var windowDuration: Duration = Duration.ofMinutes(1)
    private var keyExtractor: (Context) -> String = { ctx -> ctx.req().remoteAddr ?: "unknown" }
    private var onRateLimitExceeded: ((Context, RateLimitInfo) -> Unit)? = null
    
    fun strategy(strategy: RateLimitStrategy): RateLimitConfigBuilder {
        this.strategy = strategy
        return this
    }
    
    fun requestsPerWindow(requests: Int): RateLimitConfigBuilder {
        this.requestsPerWindow = requests
        return this
    }
    
    fun windowDuration(duration: Duration): RateLimitConfigBuilder {
        this.windowDuration = duration
        return this
    }
    
    fun keyExtractor(extractor: (Context) -> String): RateLimitConfigBuilder {
        this.keyExtractor = extractor
        return this
    }
    
    fun perIPAddress(): RateLimitConfigBuilder {
        this.keyExtractor = { ctx -> ctx.req().remoteAddr ?: "unknown" }
        return this
    }
    
    fun perUser(): RateLimitConfigBuilder {
        this.keyExtractor = { ctx ->
            val securityContext = SecurityContext.getContext(ctx)
            securityContext.principal?.name ?: ctx.req().remoteAddr ?: "unknown"
        }
        return this
    }
    
    fun perApiKey(): RateLimitConfigBuilder {
        this.keyExtractor = { ctx ->
            ctx.header("X-API-Key") ?: ctx.req().remoteAddr ?: "unknown"
        }
        return this
    }
    
    fun onRateLimitExceeded(handler: (Context, RateLimitInfo) -> Unit): RateLimitConfigBuilder {
        this.onRateLimitExceeded = handler
        return this
    }
    
    fun build(): RateLimitConfig {
        return RateLimitConfig(
            strategy = strategy,
            requestsPerWindow = requestsPerWindow,
            windowDuration = windowDuration,
            keyExtractor = keyExtractor,
            onRateLimitExceeded = onRateLimitExceeded
        )
    }
}

/**
 * Factory for creating rate limiters
 */
object RateLimiterFactory {
    fun create(config: RateLimitConfig): RateLimiter {
        return when (config.strategy) {
            RateLimitStrategy.FIXED_WINDOW -> FixedWindowRateLimiter(
                config.requestsPerWindow,
                config.windowDuration
            )
            RateLimitStrategy.SLIDING_WINDOW -> SlidingWindowRateLimiter(
                config.requestsPerWindow,
                config.windowDuration
            )
            RateLimitStrategy.TOKEN_BUCKET -> TokenBucketRateLimiter(
                capacity = config.requestsPerWindow,
                refillRate = config.requestsPerWindow / 60, // per second
                windowDuration = config.windowDuration
            )
        }
    }
}

/**
 * DSL for creating rate limit configurations
 */
fun rateLimitConfig(block: RateLimitConfigBuilder.() -> Unit): RateLimitConfig {
    return RateLimitConfigBuilder().apply(block).build()
}