package com.webframework.security

import com.webframework.core.Context
import com.webframework.routing.Handler
import java.nio.charset.StandardCharsets
import java.security.Principal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * JWT Token configuration
 */
data class JWTConfig(
    val secret: String,
    val issuer: String = "webframework",
    val expirationMinutes: Long = 60,
    val algorithm: String = "HS256",
    val clockSkewSeconds: Long = 30,
    val tokenPrefix: String = "Bearer ",
    val headerName: String = "Authorization"
)

/**
 * JWT Claims
 */
data class JWTClaims(
    val subject: String,
    val issuer: String,
    val audience: String? = null,
    val issuedAt: Long,
    val expiresAt: Long,
    val notBefore: Long? = null,
    val jwtId: String? = null,
    val roles: Set<String> = emptySet(),
    val permissions: Set<String> = emptySet(),
    val customClaims: Map<String, Any> = emptyMap()
)

/**
 * JWT Token
 */
data class JWTToken(
    val token: String,
    val claims: JWTClaims
) {
    fun isExpired(clockSkewSeconds: Long = 0): Boolean {
        val now = Instant.now().epochSecond
        return now > (claims.expiresAt + clockSkewSeconds)
    }
    
    fun isValidAt(time: Instant, clockSkewSeconds: Long = 0): Boolean {
        val timestamp = time.epochSecond
        val isAfterIssuedAt = timestamp >= (claims.issuedAt - clockSkewSeconds)
        val isBeforeExpiresAt = timestamp <= (claims.expiresAt + clockSkewSeconds)
        val isAfterNotBefore = claims.notBefore?.let { timestamp >= (it - clockSkewSeconds) } ?: true
        
        return isAfterIssuedAt && isBeforeExpiresAt && isAfterNotBefore
    }
}

/**
 * JWT Service for creating and validating tokens
 */
class JWTService(private val config: JWTConfig) {
    
    private val objectMapper = jacksonObjectMapper()
    
    /**
     * Create a JWT token
     */
    fun createToken(
        subject: String,
        roles: Set<String> = emptySet(),
        permissions: Set<String> = emptySet(),
        customClaims: Map<String, Any> = emptyMap(),
        audience: String? = null,
        expirationMinutes: Long? = null
    ): JWTToken {
        val now = Instant.now()
        val expiration = now.plusSeconds((expirationMinutes ?: config.expirationMinutes) * 60)
        
        val claims = JWTClaims(
            subject = subject,
            issuer = config.issuer,
            audience = audience,
            issuedAt = now.epochSecond,
            expiresAt = expiration.epochSecond,
            jwtId = UUID.randomUUID().toString(),
            roles = roles,
            permissions = permissions,
            customClaims = customClaims
        )
        
        val token = generateToken(claims)
        return JWTToken(token, claims)
    }
    
    /**
     * Validate and parse a JWT token
     */
    fun validateToken(token: String): JWTValidationResult {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) {
                return JWTValidationResult.Invalid("Invalid token format")
            }
            
            val (header, payload, signature) = parts
            
            // Verify signature
            val expectedSignature = createSignature("$header.$payload")
            if (signature != expectedSignature) {
                return JWTValidationResult.Invalid("Invalid signature")
            }
            
            // Parse claims
            val claimsJson = String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8)
            val claimsMap = objectMapper.readValue<Map<String, Any>>(claimsJson)
            
            val claims = parseClaimsFromMap(claimsMap)
            val jwtToken = JWTToken(token, claims)
            
            // Validate timing
            if (!jwtToken.isValidAt(Instant.now(), config.clockSkewSeconds)) {
                return JWTValidationResult.Invalid("Token expired or not yet valid")
            }
            
            // Validate issuer
            if (claims.issuer != config.issuer) {
                return JWTValidationResult.Invalid("Invalid issuer")
            }
            
            JWTValidationResult.Valid(jwtToken)
        } catch (e: Exception) {
            JWTValidationResult.Invalid("Token validation failed: ${e.message}")
        }
    }
    
    private fun generateToken(claims: JWTClaims): String {
        val header = mapOf(
            "alg" to config.algorithm,
            "typ" to "JWT"
        )
        
        val payload = mapOf(
            "sub" to claims.subject,
            "iss" to claims.issuer,
            "aud" to claims.audience,
            "iat" to claims.issuedAt,
            "exp" to claims.expiresAt,
            "nbf" to claims.notBefore,
            "jti" to claims.jwtId,
            "roles" to claims.roles.toList(),
            "permissions" to claims.permissions.toList()
        ) + claims.customClaims
        
        val encodedHeader = encodeBase64Url(objectMapper.writeValueAsString(header))
        val encodedPayload = encodeBase64Url(objectMapper.writeValueAsString(payload))
        val signature = createSignature("$encodedHeader.$encodedPayload")
        
        return "$encodedHeader.$encodedPayload.$signature"
    }
    
    private fun createSignature(data: String): String {
        return when (config.algorithm) {
            "HS256" -> {
                val mac = Mac.getInstance("HmacSHA256")
                val secretKeySpec = SecretKeySpec(config.secret.toByteArray(), "HmacSHA256")
                mac.init(secretKeySpec)
                val signature = mac.doFinal(data.toByteArray())
                encodeBase64Url(signature)
            }
            else -> throw UnsupportedOperationException("Algorithm ${config.algorithm} not supported")
        }
    }
    
    private fun encodeBase64Url(data: String): String {
        return encodeBase64Url(data.toByteArray(StandardCharsets.UTF_8))
    }
    
    private fun encodeBase64Url(data: ByteArray): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data)
    }
    
    private fun parseClaimsFromMap(map: Map<String, Any>): JWTClaims {
        return JWTClaims(
            subject = map["sub"] as String,
            issuer = map["iss"] as String,
            audience = map["aud"] as? String,
            issuedAt = (map["iat"] as Number).toLong(),
            expiresAt = (map["exp"] as Number).toLong(),
            notBefore = (map["nbf"] as? Number)?.toLong(),
            jwtId = map["jti"] as? String,
            roles = ((map["roles"] as? List<*>) ?: emptyList()).filterIsInstance<String>().toSet(),
            permissions = ((map["permissions"] as? List<*>) ?: emptyList()).filterIsInstance<String>().toSet(),
            customClaims = map.filterKeys { it !in setOf("sub", "iss", "aud", "iat", "exp", "nbf", "jti", "roles", "permissions") }
        )
    }
}

/**
 * JWT Validation result
 */
sealed class JWTValidationResult {
    data class Valid(val token: JWTToken) : JWTValidationResult()
    data class Invalid(val reason: String) : JWTValidationResult()
}

/**
 * JWT Authentication provider
 */
class JWTAuthenticationProvider(
    private val jwtService: JWTService
) : AuthenticationProvider {
    
    override fun authenticate(credentials: Credentials): AuthenticationResult {
        return when (credentials) {
            is TokenCredentials -> {
                when (val result = jwtService.validateToken(credentials.token)) {
                    is JWTValidationResult.Valid -> {
                        val claims = result.token.claims
                        AuthenticationResult.Success(
                            principal = SimplePrincipal(claims.subject),
                            roles = claims.roles,
                            permissions = claims.permissions
                        )
                    }
                    is JWTValidationResult.Invalid -> {
                        AuthenticationResult.Failure(result.reason)
                    }
                }
            }
            else -> AuthenticationResult.Failure("Unsupported credentials type")
        }
    }
    
    override fun supports(credentialsType: Class<out Credentials>): Boolean {
        return TokenCredentials::class.java.isAssignableFrom(credentialsType)
    }
}

/**
 * JWT Credentials extractor
 */
class JWTCredentialsExtractor(
    private val config: JWTConfig
) : CredentialsExtractor {
    
    override fun extractCredentials(ctx: Context): Credentials? {
        val authHeader = ctx.header(config.headerName) ?: return null
        
        if (!authHeader.startsWith(config.tokenPrefix)) return null
        
        val token = authHeader.substring(config.tokenPrefix.length)
        return if (token.isNotBlank()) TokenCredentials(token) else null
    }
}

/**
 * Session management interface
 */
interface SessionManager {
    fun createSession(principal: Principal, roles: Set<String>, permissions: Set<String>): Session
    fun getSession(sessionId: String): Session?
    fun invalidateSession(sessionId: String)
    fun cleanupExpiredSessions()
}

/**
 * Session data
 */
data class Session(
    val id: String,
    val principal: Principal,
    val roles: Set<String>,
    val permissions: Set<String>,
    val createdAt: LocalDateTime,
    val lastAccessedAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val attributes: MutableMap<String, Any> = mutableMapOf()
) {
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)
    
    fun touch(sessionTimeoutMinutes: Long = 30): Session {
        return copy(
            lastAccessedAt = LocalDateTime.now(),
            expiresAt = LocalDateTime.now().plusMinutes(sessionTimeoutMinutes)
        )
    }
    
    fun setAttribute(key: String, value: Any) {
        attributes[key] = value
    }
    
    fun getAttribute(key: String): Any? = attributes[key]
    
    fun <T> getAttribute(key: String, type: Class<T>): T? {
        return attributes[key]?.let { value ->
            if (type.isInstance(value)) type.cast(value) else null
        }
    }
}

/**
 * In-memory session manager
 */
class InMemorySessionManager(
    private val sessionTimeoutMinutes: Long = 30
) : SessionManager {
    
    private val sessions = mutableMapOf<String, Session>()
    
    override fun createSession(principal: Principal, roles: Set<String>, permissions: Set<String>): Session {
        val sessionId = UUID.randomUUID().toString()
        val now = LocalDateTime.now()
        val session = Session(
            id = sessionId,
            principal = principal,
            roles = roles,
            permissions = permissions,
            createdAt = now,
            lastAccessedAt = now,
            expiresAt = now.plusMinutes(sessionTimeoutMinutes)
        )
        
        sessions[sessionId] = session
        return session
    }
    
    override fun getSession(sessionId: String): Session? {
        val session = sessions[sessionId]
        return if (session != null && !session.isExpired()) {
            val touchedSession = session.touch(sessionTimeoutMinutes)
            sessions[sessionId] = touchedSession
            touchedSession
        } else {
            sessions.remove(sessionId)
            null
        }
    }
    
    override fun invalidateSession(sessionId: String) {
        sessions.remove(sessionId)
    }
    
    override fun cleanupExpiredSessions() {
        val expiredSessionIds = sessions.filterValues { it.isExpired() }.keys
        expiredSessionIds.forEach { sessions.remove(it) }
    }
}

/**
 * Session-based authentication provider
 */
class SessionAuthenticationProvider(
    private val sessionManager: SessionManager
) : AuthenticationProvider {
    
    override fun authenticate(credentials: Credentials): AuthenticationResult {
        return when (credentials) {
            is SessionCredentials -> {
                val session = sessionManager.getSession(credentials.sessionId)
                if (session != null) {
                    AuthenticationResult.Success(
                        principal = session.principal,
                        roles = session.roles,
                        permissions = session.permissions
                    )
                } else {
                    AuthenticationResult.Failure("Invalid or expired session")
                }
            }
            else -> AuthenticationResult.Failure("Unsupported credentials type")
        }
    }
    
    override fun supports(credentialsType: Class<out Credentials>): Boolean {
        return SessionCredentials::class.java.isAssignableFrom(credentialsType)
    }
}

/**
 * Session credentials
 */
data class SessionCredentials(
    val sessionId: String
) : Credentials()

/**
 * Session credentials extractor
 */
class SessionCredentialsExtractor(
    private val cookieName: String = "JSESSIONID"
) : CredentialsExtractor {
    
    override fun extractCredentials(ctx: Context): Credentials? {
        // Try to get session ID from cookie
        val cookies = ctx.req().cookies
        if (cookies != null) {
            for (cookie in cookies) {
                if (cookie.name == cookieName) {
                    return SessionCredentials(cookie.value)
                }
            }
        }
        
        // Try to get session ID from header
        val sessionHeader = ctx.header("X-Session-ID")
        if (sessionHeader != null) {
            return SessionCredentials(sessionHeader)
        }
        
        return null
    }
}

/**
 * JWT and Session configuration builder
 */
class JWTSessionConfigBuilder {
    private var jwtConfig: JWTConfig? = null
    private var sessionManager: SessionManager? = null
    private var enableJWT: Boolean = true
    private var enableSessions: Boolean = true
    
    fun jwtConfig(config: JWTConfig): JWTSessionConfigBuilder {
        jwtConfig = config
        return this
    }
    
    fun jwtConfig(block: JWTConfigBuilder.() -> Unit): JWTSessionConfigBuilder {
        jwtConfig = JWTConfigBuilder().apply(block).build()
        return this
    }
    
    fun sessionManager(manager: SessionManager): JWTSessionConfigBuilder {
        sessionManager = manager
        return this
    }
    
    fun inMemorySessions(timeoutMinutes: Long = 30): JWTSessionConfigBuilder {
        sessionManager = InMemorySessionManager(timeoutMinutes)
        return this
    }
    
    fun enableJWT(enable: Boolean = true): JWTSessionConfigBuilder {
        enableJWT = enable
        return this
    }
    
    fun enableSessions(enable: Boolean = true): JWTSessionConfigBuilder {
        enableSessions = enable
        return this
    }
    
    fun build(): JWTSessionConfig {
        require(jwtConfig != null || !enableJWT) { "JWT config is required when JWT is enabled" }
        require(sessionManager != null || !enableSessions) { "Session manager is required when sessions are enabled" }
        
        return JWTSessionConfig(
            jwtConfig = jwtConfig,
            sessionManager = sessionManager,
            enableJWT = enableJWT,
            enableSessions = enableSessions
        )
    }
}

/**
 * JWT and Session configuration
 */
data class JWTSessionConfig(
    val jwtConfig: JWTConfig?,
    val sessionManager: SessionManager?,
    val enableJWT: Boolean,
    val enableSessions: Boolean
) {
    
    fun createAuthenticationProviders(): List<AuthenticationProvider> {
        val providers = mutableListOf<AuthenticationProvider>()
        
        if (enableJWT && jwtConfig != null) {
            providers.add(JWTAuthenticationProvider(JWTService(jwtConfig)))
        }
        
        if (enableSessions && sessionManager != null) {
            providers.add(SessionAuthenticationProvider(sessionManager))
        }
        
        return providers
    }
    
    fun createCredentialsExtractors(): List<CredentialsExtractor> {
        val extractors = mutableListOf<CredentialsExtractor>()
        
        if (enableJWT && jwtConfig != null) {
            extractors.add(JWTCredentialsExtractor(jwtConfig))
        }
        
        if (enableSessions) {
            extractors.add(SessionCredentialsExtractor())
        }
        
        return extractors
    }
}

/**
 * JWT Config builder
 */
class JWTConfigBuilder {
    private var secret: String = "default-secret-change-in-production"
    private var issuer: String = "webframework"
    private var expirationMinutes: Long = 60
    private var algorithm: String = "HS256"
    private var clockSkewSeconds: Long = 30
    private var tokenPrefix: String = "Bearer "
    private var headerName: String = "Authorization"
    
    fun secret(secret: String): JWTConfigBuilder {
        this.secret = secret
        return this
    }
    
    fun issuer(issuer: String): JWTConfigBuilder {
        this.issuer = issuer
        return this
    }
    
    fun expirationMinutes(minutes: Long): JWTConfigBuilder {
        this.expirationMinutes = minutes
        return this
    }
    
    fun algorithm(algorithm: String): JWTConfigBuilder {
        this.algorithm = algorithm
        return this
    }
    
    fun clockSkewSeconds(seconds: Long): JWTConfigBuilder {
        this.clockSkewSeconds = seconds
        return this
    }
    
    fun tokenPrefix(prefix: String): JWTConfigBuilder {
        this.tokenPrefix = prefix
        return this
    }
    
    fun headerName(name: String): JWTConfigBuilder {
        this.headerName = name
        return this
    }
    
    fun build(): JWTConfig {
        return JWTConfig(
            secret = secret,
            issuer = issuer,
            expirationMinutes = expirationMinutes,
            algorithm = algorithm,
            clockSkewSeconds = clockSkewSeconds,
            tokenPrefix = tokenPrefix,
            headerName = headerName
        )
    }
}

/**
 * DSL for creating JWT configurations
 */
fun jwtConfig(block: JWTConfigBuilder.() -> Unit): JWTConfig {
    return JWTConfigBuilder().apply(block).build()
}

/**
 * DSL for creating JWT and Session configurations
 */
fun jwtSessionConfig(block: JWTSessionConfigBuilder.() -> Unit): JWTSessionConfig {
    return JWTSessionConfigBuilder().apply(block).build()
}