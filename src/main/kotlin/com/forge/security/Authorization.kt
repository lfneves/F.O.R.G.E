package com.forge.security

import com.forge.core.Context
import com.forge.routing.Handler

/**
 * Authorization annotations for securing endpoints
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequireAuth

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequireRole(vararg val roles: String)

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequirePermission(vararg val permissions: String)

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AllowAnonymous

/**
 * Authorization decision
 */
sealed class AuthorizationDecision {
    object Allow : AuthorizationDecision()
    data class Deny(val reason: String) : AuthorizationDecision()
}

/**
 * Authorization rule interface
 */
interface AuthorizationRule {
    fun evaluate(ctx: Context, securityContext: SecurityContext): AuthorizationDecision
}

/**
 * Requires authentication
 */
class RequireAuthenticationRule : AuthorizationRule {
    override fun evaluate(ctx: Context, securityContext: SecurityContext): AuthorizationDecision {
        return if (securityContext.isAuthenticated) {
            AuthorizationDecision.Allow
        } else {
            AuthorizationDecision.Deny("Authentication required")
        }
    }
}

/**
 * Requires specific roles
 */
class RequireRoleRule(private val requiredRoles: Set<String>) : AuthorizationRule {
    constructor(vararg roles: String) : this(roles.toSet())
    
    override fun evaluate(ctx: Context, securityContext: SecurityContext): AuthorizationDecision {
        if (!securityContext.isAuthenticated) {
            return AuthorizationDecision.Deny("Authentication required")
        }
        
        return if (securityContext.hasAnyRole(*requiredRoles.toTypedArray())) {
            AuthorizationDecision.Allow
        } else {
            AuthorizationDecision.Deny("Required roles: ${requiredRoles.joinToString(", ")}")
        }
    }
}

/**
 * Requires specific permissions
 */
class RequirePermissionRule(private val requiredPermissions: Set<String>) : AuthorizationRule {
    constructor(vararg permissions: String) : this(permissions.toSet())
    
    override fun evaluate(ctx: Context, securityContext: SecurityContext): AuthorizationDecision {
        if (!securityContext.isAuthenticated) {
            return AuthorizationDecision.Deny("Authentication required")
        }
        
        return if (securityContext.hasAnyPermission(*requiredPermissions.toTypedArray())) {
            AuthorizationDecision.Allow
        } else {
            AuthorizationDecision.Deny("Required permissions: ${requiredPermissions.joinToString(", ")}")
        }
    }
}

/**
 * Custom authorization rule based on a lambda
 */
class CustomAuthorizationRule(
    private val evaluator: (Context, SecurityContext) -> AuthorizationDecision
) : AuthorizationRule {
    override fun evaluate(ctx: Context, securityContext: SecurityContext): AuthorizationDecision {
        return evaluator(ctx, securityContext)
    }
}

/**
 * Authorization middleware
 */
class AuthorizationMiddleware(
    private val rules: List<AuthorizationRule>,
    private val onUnauthorized: ((Context, String) -> Unit)? = null
) : Handler {
    
    constructor(vararg rules: AuthorizationRule) : this(rules.toList())
    
    override fun handle(ctx: Context) {
        val securityContext = SecurityContext.getContext(ctx)
        
        for (rule in rules) {
            when (val decision = rule.evaluate(ctx, securityContext)) {
                is AuthorizationDecision.Allow -> continue
                is AuthorizationDecision.Deny -> {
                    handleUnauthorized(ctx, decision.reason)
                    return
                }
            }
        }
    }
    
    private fun handleUnauthorized(ctx: Context, reason: String) {
        if (onUnauthorized != null) {
            onUnauthorized.invoke(ctx, reason)
        } else {
            val securityContext = SecurityContext.getContext(ctx)
            if (securityContext.isAuthenticated) {
                ctx.status(403).json(mapOf(
                    "error" to "Forbidden",
                    "message" to reason
                ))
            } else {
                ctx.status(401).json(mapOf(
                    "error" to "Unauthorized",
                    "message" to "Authentication required"
                ))
            }
        }
    }
}

/**
 * Security configuration builder
 */
class SecurityConfigBuilder {
    private val authenticationProviders = mutableListOf<AuthenticationProvider>()
    private var credentialsExtractor: CredentialsExtractor? = null
    private val globalAuthorizationRules = mutableListOf<AuthorizationRule>()
    private var onUnauthorized: ((Context, String) -> Unit)? = null
    
    fun addAuthenticationProvider(provider: AuthenticationProvider): SecurityConfigBuilder {
        authenticationProviders.add(provider)
        return this
    }
    
    fun setCredentialsExtractor(extractor: CredentialsExtractor): SecurityConfigBuilder {
        credentialsExtractor = extractor
        return this
    }
    
    fun requireAuthentication(): SecurityConfigBuilder {
        globalAuthorizationRules.add(RequireAuthenticationRule())
        return this
    }
    
    fun requireRole(vararg roles: String): SecurityConfigBuilder {
        globalAuthorizationRules.add(RequireRoleRule(*roles))
        return this
    }
    
    fun requirePermission(vararg permissions: String): SecurityConfigBuilder {
        globalAuthorizationRules.add(RequirePermissionRule(*permissions))
        return this
    }
    
    fun addAuthorizationRule(rule: AuthorizationRule): SecurityConfigBuilder {
        globalAuthorizationRules.add(rule)
        return this
    }
    
    fun onUnauthorized(handler: (Context, String) -> Unit): SecurityConfigBuilder {
        onUnauthorized = handler
        return this
    }
    
    fun build(): SecurityConfig {
        require(authenticationProviders.isNotEmpty()) { "At least one authentication provider must be configured" }
        require(credentialsExtractor != null) { "Credentials extractor must be configured" }
        
        return SecurityConfig(
            authenticationProviders = authenticationProviders,
            credentialsExtractor = credentialsExtractor!!,
            globalAuthorizationRules = globalAuthorizationRules,
            onUnauthorized = onUnauthorized
        )
    }
}

/**
 * Security configuration
 */
data class SecurityConfig(
    val authenticationProviders: List<AuthenticationProvider>,
    val credentialsExtractor: CredentialsExtractor,
    val globalAuthorizationRules: List<AuthorizationRule>,
    val onUnauthorized: ((Context, String) -> Unit)?
) {
    
    fun createAuthenticationMiddleware(): AuthenticationMiddleware {
        val compositeProvider = CompositeAuthenticationProvider(authenticationProviders)
        return AuthenticationMiddleware(compositeProvider, credentialsExtractor)
    }
    
    fun createAuthorizationMiddleware(): AuthorizationMiddleware {
        return AuthorizationMiddleware(globalAuthorizationRules, onUnauthorized)
    }
}

/**
 * Composite authentication provider that tries multiple providers
 */
class CompositeAuthenticationProvider(
    private val providers: List<AuthenticationProvider>
) : AuthenticationProvider {
    
    override fun authenticate(credentials: Credentials): AuthenticationResult {
        for (provider in providers) {
            if (provider.supports(credentials::class.java)) {
                val result = provider.authenticate(credentials)
                if (result is AuthenticationResult.Success) {
                    return result
                }
            }
        }
        return AuthenticationResult.Failure("Authentication failed")
    }
    
    override fun supports(credentialsType: Class<out Credentials>): Boolean {
        return providers.any { it.supports(credentialsType) }
    }
}

/**
 * DSL for creating security configurations
 */
fun securityConfig(block: SecurityConfigBuilder.() -> Unit): SecurityConfig {
    return SecurityConfigBuilder().apply(block).build()
}