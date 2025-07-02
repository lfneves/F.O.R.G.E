package com.webframework.security

import com.webframework.core.Context
import com.webframework.routing.Handler

/**
 * Security headers configuration
 */
data class SecurityHeadersConfig(
    // Content Security Policy
    val contentSecurityPolicy: String? = "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; media-src 'self'; object-src 'none'; child-src 'self'; frame-ancestors 'none'; form-action 'self'; base-uri 'self'",
    
    // X-Frame-Options
    val xFrameOptions: String = "DENY",
    
    // X-Content-Type-Options
    val xContentTypeOptions: String = "nosniff",
    
    // X-XSS-Protection
    val xXSSProtection: String = "1; mode=block",
    
    // Strict-Transport-Security
    val strictTransportSecurity: String? = "max-age=31536000; includeSubDomains; preload",
    
    // Referrer-Policy
    val referrerPolicy: String = "strict-origin-when-cross-origin",
    
    // Permissions-Policy (formerly Feature-Policy)
    val permissionsPolicy: String? = "geolocation=(), microphone=(), camera=()",
    
    // Cross-Origin-Embedder-Policy
    val crossOriginEmbedderPolicy: String? = "require-corp",
    
    // Cross-Origin-Opener-Policy
    val crossOriginOpenerPolicy: String? = "same-origin",
    
    // Cross-Origin-Resource-Policy
    val crossOriginResourcePolicy: String? = "same-origin",
    
    // Cache-Control for security-sensitive endpoints
    val cacheControl: String? = "no-cache, no-store, must-revalidate",
    
    // Pragma for HTTP/1.0 compatibility
    val pragma: String? = "no-cache",
    
    // Expires header
    val expires: String? = "0",
    
    // Custom headers
    val customHeaders: Map<String, String> = emptyMap(),
    
    // Whether to enable HTTPS-only mode
    val httpsOnly: Boolean = false,
    
    // Whether to remove server information
    val removeServerHeader: Boolean = true
)

/**
 * Security headers middleware
 */
class SecurityHeadersMiddleware(
    private val config: SecurityHeadersConfig
) : Handler {
    
    override fun handle(ctx: Context) {
        val response = ctx.res()
        
        // Content Security Policy
        config.contentSecurityPolicy?.let { csp ->
            response.setHeader("Content-Security-Policy", csp)
        }
        
        // X-Frame-Options
        response.setHeader("X-Frame-Options", config.xFrameOptions)
        
        // X-Content-Type-Options
        response.setHeader("X-Content-Type-Options", config.xContentTypeOptions)
        
        // X-XSS-Protection
        response.setHeader("X-XSS-Protection", config.xXSSProtection)
        
        // Strict-Transport-Security (only for HTTPS)
        if (isHTTPS(ctx) && config.strictTransportSecurity != null) {
            response.setHeader("Strict-Transport-Security", config.strictTransportSecurity)
        }
        
        // Referrer-Policy
        response.setHeader("Referrer-Policy", config.referrerPolicy)
        
        // Permissions-Policy
        config.permissionsPolicy?.let { policy ->
            response.setHeader("Permissions-Policy", policy)
        }
        
        // Cross-Origin-Embedder-Policy
        config.crossOriginEmbedderPolicy?.let { policy ->
            response.setHeader("Cross-Origin-Embedder-Policy", policy)
        }
        
        // Cross-Origin-Opener-Policy
        config.crossOriginOpenerPolicy?.let { policy ->
            response.setHeader("Cross-Origin-Opener-Policy", policy)
        }
        
        // Cross-Origin-Resource-Policy
        config.crossOriginResourcePolicy?.let { policy ->
            response.setHeader("Cross-Origin-Resource-Policy", policy)
        }
        
        // Cache control headers
        config.cacheControl?.let { cacheControl ->
            response.setHeader("Cache-Control", cacheControl)
        }
        
        config.pragma?.let { pragma ->
            response.setHeader("Pragma", pragma)
        }
        
        config.expires?.let { expires ->
            response.setHeader("Expires", expires)
        }
        
        // Custom headers
        config.customHeaders.forEach { (name, value) ->
            response.setHeader(name, value)
        }
        
        // Remove server header if configured
        if (config.removeServerHeader) {
            response.setHeader("Server", "")
        }
        
        // HTTPS-only enforcement
        if (config.httpsOnly && !isHTTPS(ctx)) {
            val httpsUrl = "https://${ctx.req().serverName}:${getHTTPSPort(ctx)}${ctx.req().requestURI}"
            if (ctx.req().queryString != null) {
                httpsUrl + "?" + ctx.req().queryString
            }
            response.sendRedirect(httpsUrl)
            return
        }
    }
    
    private fun isHTTPS(ctx: Context): Boolean {
        val request = ctx.req()
        return request.scheme.equals("https", ignoreCase = true) ||
                request.isSecure ||
                request.getHeader("X-Forwarded-Proto")?.equals("https", ignoreCase = true) == true ||
                request.getHeader("X-Forwarded-SSL")?.equals("on", ignoreCase = true) == true
    }
    
    private fun getHTTPSPort(ctx: Context): Int {
        val forwardedPort = ctx.req().getHeader("X-Forwarded-Port")
        return forwardedPort?.toIntOrNull() ?: 443
    }
}

/**
 * Content Security Policy builder
 */
class CSPBuilder {
    private val directives = mutableMapOf<String, MutableSet<String>>()
    
    fun defaultSrc(vararg sources: String): CSPBuilder {
        addDirective("default-src", *sources)
        return this
    }
    
    fun scriptSrc(vararg sources: String): CSPBuilder {
        addDirective("script-src", *sources)
        return this
    }
    
    fun styleSrc(vararg sources: String): CSPBuilder {
        addDirective("style-src", *sources)
        return this
    }
    
    fun imgSrc(vararg sources: String): CSPBuilder {
        addDirective("img-src", *sources)
        return this
    }
    
    fun fontSrc(vararg sources: String): CSPBuilder {
        addDirective("font-src", *sources)
        return this
    }
    
    fun connectSrc(vararg sources: String): CSPBuilder {
        addDirective("connect-src", *sources)
        return this
    }
    
    fun mediaSrc(vararg sources: String): CSPBuilder {
        addDirective("media-src", *sources)
        return this
    }
    
    fun objectSrc(vararg sources: String): CSPBuilder {
        addDirective("object-src", *sources)
        return this
    }
    
    fun childSrc(vararg sources: String): CSPBuilder {
        addDirective("child-src", *sources)
        return this
    }
    
    fun frameAncestors(vararg sources: String): CSPBuilder {
        addDirective("frame-ancestors", *sources)
        return this
    }
    
    fun formAction(vararg sources: String): CSPBuilder {
        addDirective("form-action", *sources)
        return this
    }
    
    fun baseUri(vararg sources: String): CSPBuilder {
        addDirective("base-uri", *sources)
        return this
    }
    
    fun upgradeInsecureRequests(): CSPBuilder {
        addDirective("upgrade-insecure-requests")
        return this
    }
    
    fun blockAllMixedContent(): CSPBuilder {
        addDirective("block-all-mixed-content")
        return this
    }
    
    fun reportUri(uri: String): CSPBuilder {
        addDirective("report-uri", uri)
        return this
    }
    
    fun reportTo(group: String): CSPBuilder {
        addDirective("report-to", group)
        return this
    }
    
    private fun addDirective(directive: String, vararg sources: String) {
        val directiveSet = directives.computeIfAbsent(directive) { mutableSetOf() }
        directiveSet.addAll(sources)
    }
    
    fun build(): String {
        return directives.entries.joinToString("; ") { (directive, sources) ->
            if (sources.isEmpty()) {
                directive
            } else {
                "$directive ${sources.joinToString(" ")}"
            }
        }
    }
    
    companion object {
        const val SELF = "'self'"
        const val NONE = "'none'"
        const val UNSAFE_INLINE = "'unsafe-inline'"
        const val UNSAFE_EVAL = "'unsafe-eval'"
        const val STRICT_DYNAMIC = "'strict-dynamic'"
        const val DATA = "data:"
        const val HTTPS = "https:"
        const val HTTP = "http:"
        const val BLOB = "blob:"
        const val FILESYSTEM = "filesystem:"
    }
}

/**
 * Security headers configuration builder
 */
class SecurityHeadersConfigBuilder {
    private var contentSecurityPolicy: String? = null
    private var xFrameOptions: String = "DENY"
    private var xContentTypeOptions: String = "nosniff"
    private var xXSSProtection: String = "1; mode=block"
    private var strictTransportSecurity: String? = null
    private var referrerPolicy: String = "strict-origin-when-cross-origin"
    private var permissionsPolicy: String? = null
    private var crossOriginEmbedderPolicy: String? = null
    private var crossOriginOpenerPolicy: String? = null
    private var crossOriginResourcePolicy: String? = null
    private var cacheControl: String? = null
    private var pragma: String? = null
    private var expires: String? = null
    private var customHeaders: MutableMap<String, String> = mutableMapOf()
    private var httpsOnly: Boolean = false
    private var removeServerHeader: Boolean = true
    
    fun contentSecurityPolicy(csp: String): SecurityHeadersConfigBuilder {
        contentSecurityPolicy = csp
        return this
    }
    
    fun contentSecurityPolicy(block: CSPBuilder.() -> Unit): SecurityHeadersConfigBuilder {
        contentSecurityPolicy = CSPBuilder().apply(block).build()
        return this
    }
    
    fun xFrameOptions(value: String): SecurityHeadersConfigBuilder {
        xFrameOptions = value
        return this
    }
    
    fun denyFraming(): SecurityHeadersConfigBuilder {
        xFrameOptions = "DENY"
        return this
    }
    
    fun sameOriginFraming(): SecurityHeadersConfigBuilder {
        xFrameOptions = "SAMEORIGIN"
        return this
    }
    
    fun allowFromOrigin(origin: String): SecurityHeadersConfigBuilder {
        xFrameOptions = "ALLOW-FROM $origin"
        return this
    }
    
    fun xContentTypeOptions(value: String): SecurityHeadersConfigBuilder {
        xContentTypeOptions = value
        return this
    }
    
    fun xXSSProtection(value: String): SecurityHeadersConfigBuilder {
        xXSSProtection = value
        return this
    }
    
    fun strictTransportSecurity(value: String): SecurityHeadersConfigBuilder {
        strictTransportSecurity = value
        return this
    }
    
    fun hstsMaxAge(seconds: Long, includeSubDomains: Boolean = true, preload: Boolean = false): SecurityHeadersConfigBuilder {
        val hstsValue = buildString {
            append("max-age=$seconds")
            if (includeSubDomains) append("; includeSubDomains")
            if (preload) append("; preload")
        }
        strictTransportSecurity = hstsValue
        return this
    }
    
    fun referrerPolicy(value: String): SecurityHeadersConfigBuilder {
        referrerPolicy = value
        return this
    }
    
    fun permissionsPolicy(value: String): SecurityHeadersConfigBuilder {
        permissionsPolicy = value
        return this
    }
    
    fun crossOriginEmbedderPolicy(value: String): SecurityHeadersConfigBuilder {
        crossOriginEmbedderPolicy = value
        return this
    }
    
    fun crossOriginOpenerPolicy(value: String): SecurityHeadersConfigBuilder {
        crossOriginOpenerPolicy = value
        return this
    }
    
    fun crossOriginResourcePolicy(value: String): SecurityHeadersConfigBuilder {
        crossOriginResourcePolicy = value
        return this
    }
    
    fun cacheControl(value: String): SecurityHeadersConfigBuilder {
        cacheControl = value
        return this
    }
    
    fun noCache(): SecurityHeadersConfigBuilder {
        cacheControl = "no-cache, no-store, must-revalidate"
        pragma = "no-cache"
        expires = "0"
        return this
    }
    
    fun addCustomHeader(name: String, value: String): SecurityHeadersConfigBuilder {
        customHeaders[name] = value
        return this
    }
    
    fun httpsOnly(enable: Boolean = true): SecurityHeadersConfigBuilder {
        httpsOnly = enable
        return this
    }
    
    fun removeServerHeader(remove: Boolean = true): SecurityHeadersConfigBuilder {
        removeServerHeader = remove
        return this
    }
    
    fun build(): SecurityHeadersConfig {
        return SecurityHeadersConfig(
            contentSecurityPolicy = contentSecurityPolicy,
            xFrameOptions = xFrameOptions,
            xContentTypeOptions = xContentTypeOptions,
            xXSSProtection = xXSSProtection,
            strictTransportSecurity = strictTransportSecurity,
            referrerPolicy = referrerPolicy,
            permissionsPolicy = permissionsPolicy,
            crossOriginEmbedderPolicy = crossOriginEmbedderPolicy,
            crossOriginOpenerPolicy = crossOriginOpenerPolicy,
            crossOriginResourcePolicy = crossOriginResourcePolicy,
            cacheControl = cacheControl,
            pragma = pragma,
            expires = expires,
            customHeaders = customHeaders.toMap(),
            httpsOnly = httpsOnly,
            removeServerHeader = removeServerHeader
        )
    }
}

/**
 * Predefined security header configurations
 */
object SecurityHeadersPresets {
    
    fun strict(): SecurityHeadersConfig {
        return SecurityHeadersConfigBuilder()
            .contentSecurityPolicy {
                defaultSrc(CSPBuilder.SELF)
                scriptSrc(CSPBuilder.SELF)
                styleSrc(CSPBuilder.SELF, CSPBuilder.UNSAFE_INLINE)
                imgSrc(CSPBuilder.SELF, CSPBuilder.DATA)
                fontSrc(CSPBuilder.SELF)
                connectSrc(CSPBuilder.SELF)
                mediaSrc(CSPBuilder.SELF)
                objectSrc(CSPBuilder.NONE)
                childSrc(CSPBuilder.SELF)
                frameAncestors(CSPBuilder.NONE)
                formAction(CSPBuilder.SELF)
                baseUri(CSPBuilder.SELF)
                upgradeInsecureRequests()
            }
            .denyFraming()
            .hstsMaxAge(31536000, includeSubDomains = true, preload = true)
            .referrerPolicy("strict-origin-when-cross-origin")
            .permissionsPolicy("geolocation=(), microphone=(), camera=()")
            .crossOriginEmbedderPolicy("require-corp")
            .crossOriginOpenerPolicy("same-origin")
            .crossOriginResourcePolicy("same-origin")
            .noCache()
            .httpsOnly(true)
            .build()
    }
    
    fun moderate(): SecurityHeadersConfig {
        return SecurityHeadersConfigBuilder()
            .contentSecurityPolicy {
                defaultSrc(CSPBuilder.SELF)
                scriptSrc(CSPBuilder.SELF, CSPBuilder.UNSAFE_INLINE)
                styleSrc(CSPBuilder.SELF, CSPBuilder.UNSAFE_INLINE)
                imgSrc(CSPBuilder.SELF, CSPBuilder.DATA, CSPBuilder.HTTPS)
                fontSrc(CSPBuilder.SELF, CSPBuilder.HTTPS)
                connectSrc(CSPBuilder.SELF)
                mediaSrc(CSPBuilder.SELF)
                objectSrc(CSPBuilder.NONE)
                frameAncestors(CSPBuilder.NONE)
                formAction(CSPBuilder.SELF)
            }
            .sameOriginFraming()
            .hstsMaxAge(31536000, includeSubDomains = true)
            .referrerPolicy("strict-origin-when-cross-origin")
            .build()
    }
    
    fun permissive(): SecurityHeadersConfig {
        return SecurityHeadersConfigBuilder()
            .contentSecurityPolicy {
                defaultSrc(CSPBuilder.SELF, CSPBuilder.UNSAFE_INLINE, CSPBuilder.UNSAFE_EVAL)
                imgSrc("*", CSPBuilder.DATA)
                fontSrc("*")
                connectSrc("*")
            }
            .sameOriginFraming()
            .hstsMaxAge(3600)
            .build()
    }
    
    fun development(): SecurityHeadersConfig {
        return SecurityHeadersConfigBuilder()
            .contentSecurityPolicy {
                defaultSrc(CSPBuilder.SELF, CSPBuilder.UNSAFE_INLINE, CSPBuilder.UNSAFE_EVAL)
                scriptSrc(CSPBuilder.SELF, CSPBuilder.UNSAFE_INLINE, CSPBuilder.UNSAFE_EVAL, "localhost:*", "127.0.0.1:*")
                styleSrc(CSPBuilder.SELF, CSPBuilder.UNSAFE_INLINE)
                imgSrc("*", CSPBuilder.DATA)
                fontSrc("*")
                connectSrc("*")
            }
            .sameOriginFraming()
            .removeServerHeader(false)
            .build()
    }
}

/**
 * DSL for creating security headers configurations
 */
fun securityHeadersConfig(block: SecurityHeadersConfigBuilder.() -> Unit): SecurityHeadersConfig {
    return SecurityHeadersConfigBuilder().apply(block).build()
}

/**
 * DSL for creating Content Security Policy
 */
fun contentSecurityPolicy(block: CSPBuilder.() -> Unit): String {
    return CSPBuilder().apply(block).build()
}