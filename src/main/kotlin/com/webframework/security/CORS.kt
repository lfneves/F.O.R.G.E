package com.webframework.security

import com.webframework.core.Context
import com.webframework.routing.Handler

/**
 * CORS (Cross-Origin Resource Sharing) configuration
 */
data class CORSConfig(
    val allowedOrigins: Set<String> = setOf("*"),
    val allowedMethods: Set<String> = setOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"),
    val allowedHeaders: Set<String> = setOf("Content-Type", "Authorization", "X-Requested-With"),
    val exposedHeaders: Set<String> = emptySet(),
    val allowCredentials: Boolean = false,
    val maxAge: Long = 3600, // 1 hour in seconds
    val preflightContinue: Boolean = false
) {
    
    companion object {
        fun permissive() = CORSConfig(
            allowedOrigins = setOf("*"),
            allowedMethods = setOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"),
            allowedHeaders = setOf("*"),
            allowCredentials = false
        )
        
        fun restrictive(allowedOrigins: Set<String>) = CORSConfig(
            allowedOrigins = allowedOrigins,
            allowedMethods = setOf("GET", "POST"),
            allowedHeaders = setOf("Content-Type", "Authorization"),
            allowCredentials = true
        )
        
        fun development() = CORSConfig(
            allowedOrigins = setOf("http://localhost:3000", "http://localhost:8080", "http://localhost:8081"),
            allowedMethods = setOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"),
            allowedHeaders = setOf("Content-Type", "Authorization", "X-Requested-With", "X-API-Key"),
            allowCredentials = true
        )
    }
}

/**
 * CORS middleware that handles Cross-Origin Resource Sharing
 */
class CORSMiddleware(private val config: CORSConfig) : Handler {
    
    override fun handle(ctx: Context) {
        val request = ctx.req()
        val response = ctx.res()
        val origin = request.getHeader("Origin")
        
        // Check if origin is allowed
        if (origin != null && isOriginAllowed(origin)) {
            // Set CORS headers
            if (config.allowedOrigins.contains("*") && !config.allowCredentials) {
                response.setHeader("Access-Control-Allow-Origin", "*")
            } else {
                response.setHeader("Access-Control-Allow-Origin", origin)
            }
            
            if (config.allowCredentials) {
                response.setHeader("Access-Control-Allow-Credentials", "true")
            }
            
            if (config.exposedHeaders.isNotEmpty()) {
                response.setHeader("Access-Control-Expose-Headers", config.exposedHeaders.joinToString(", "))
            }
        }
        
        // Handle preflight requests
        if (request.method.equals("OPTIONS", ignoreCase = true)) {
            handlePreflightRequest(ctx)
            return
        }
    }
    
    private fun isOriginAllowed(origin: String): Boolean {
        if (config.allowedOrigins.contains("*")) {
            return true
        }
        
        return config.allowedOrigins.any { allowedOrigin ->
            when {
                allowedOrigin == origin -> true
                allowedOrigin.startsWith("*.") -> {
                    val domain = allowedOrigin.substring(2)
                    origin.endsWith(".$domain") || origin == domain
                }
                else -> false
            }
        }
    }
    
    private fun handlePreflightRequest(ctx: Context) {
        val request = ctx.req()
        val response = ctx.res()
        
        // Set allowed methods
        response.setHeader("Access-Control-Allow-Methods", config.allowedMethods.joinToString(", "))
        
        // Handle requested headers
        val requestedHeaders = request.getHeader("Access-Control-Request-Headers")
        if (requestedHeaders != null) {
            val requestedHeadersList = requestedHeaders.split(",").map { it.trim() }
            val allowedRequestedHeaders = if (config.allowedHeaders.contains("*")) {
                requestedHeadersList
            } else {
                requestedHeadersList.filter { header ->
                    config.allowedHeaders.any { it.equals(header, ignoreCase = true) }
                }
            }
            
            if (allowedRequestedHeaders.isNotEmpty()) {
                response.setHeader("Access-Control-Allow-Headers", allowedRequestedHeaders.joinToString(", "))
            }
        } else if (config.allowedHeaders.isNotEmpty() && !config.allowedHeaders.contains("*")) {
            response.setHeader("Access-Control-Allow-Headers", config.allowedHeaders.joinToString(", "))
        }
        
        // Set max age for preflight cache
        response.setHeader("Access-Control-Max-Age", config.maxAge.toString())
        
        // Set response status
        response.status = 204
        
        // End the response for preflight requests unless configured to continue
        if (!config.preflightContinue) {
            response.writer.flush()
        }
    }
}

/**
 * CORS configuration builder
 */
class CORSConfigBuilder {
    private var allowedOrigins: MutableSet<String> = mutableSetOf("*")
    private var allowedMethods: MutableSet<String> = mutableSetOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
    private var allowedHeaders: MutableSet<String> = mutableSetOf("Content-Type", "Authorization", "X-Requested-With")
    private var exposedHeaders: MutableSet<String> = mutableSetOf()
    private var allowCredentials: Boolean = false
    private var maxAge: Long = 3600
    private var preflightContinue: Boolean = false
    
    fun allowOrigin(origin: String): CORSConfigBuilder {
        if (allowedOrigins.contains("*")) {
            allowedOrigins.clear()
        }
        allowedOrigins.add(origin)
        return this
    }
    
    fun allowOrigins(vararg origins: String): CORSConfigBuilder {
        if (allowedOrigins.contains("*")) {
            allowedOrigins.clear()
        }
        allowedOrigins.addAll(origins)
        return this
    }
    
    fun allowAnyOrigin(): CORSConfigBuilder {
        allowedOrigins.clear()
        allowedOrigins.add("*")
        allowCredentials = false // Credentials cannot be used with wildcard origin
        return this
    }
    
    fun allowMethod(method: String): CORSConfigBuilder {
        allowedMethods.add(method.uppercase())
        return this
    }
    
    fun allowMethods(vararg methods: String): CORSConfigBuilder {
        allowedMethods.addAll(methods.map { it.uppercase() })
        return this
    }
    
    fun allowHeader(header: String): CORSConfigBuilder {
        allowedHeaders.add(header)
        return this
    }
    
    fun allowHeaders(vararg headers: String): CORSConfigBuilder {
        allowedHeaders.addAll(headers)
        return this
    }
    
    fun allowAnyHeader(): CORSConfigBuilder {
        allowedHeaders.clear()
        allowedHeaders.add("*")
        return this
    }
    
    fun exposeHeader(header: String): CORSConfigBuilder {
        exposedHeaders.add(header)
        return this
    }
    
    fun exposeHeaders(vararg headers: String): CORSConfigBuilder {
        exposedHeaders.addAll(headers)
        return this
    }
    
    fun allowCredentials(allow: Boolean = true): CORSConfigBuilder {
        allowCredentials = allow
        if (allow && allowedOrigins.contains("*")) {
            throw IllegalArgumentException("Cannot allow credentials with wildcard origin")
        }
        return this
    }
    
    fun maxAge(seconds: Long): CORSConfigBuilder {
        maxAge = seconds
        return this
    }
    
    fun preflightContinue(continueValue: Boolean = true): CORSConfigBuilder {
        preflightContinue = continueValue
        return this
    }
    
    fun build(): CORSConfig {
        return CORSConfig(
            allowedOrigins = allowedOrigins.toSet(),
            allowedMethods = allowedMethods.toSet(),
            allowedHeaders = allowedHeaders.toSet(),
            exposedHeaders = exposedHeaders.toSet(),
            allowCredentials = allowCredentials,
            maxAge = maxAge,
            preflightContinue = preflightContinue
        )
    }
}

/**
 * DSL for creating CORS configurations
 */
fun corsConfig(block: CORSConfigBuilder.() -> Unit): CORSConfig {
    return CORSConfigBuilder().apply(block).build()
}