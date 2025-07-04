package com.forge.security

import com.forge.core.Context
import java.security.Principal
import java.time.LocalDateTime

/**
 * Security context that holds authentication and authorization information for a request
 */
class SecurityContext {
    var principal: Principal? = null
        private set
    
    var isAuthenticated: Boolean = false
        private set
    
    var roles: Set<String> = emptySet()
        private set
    
    var permissions: Set<String> = emptySet()
        private set
    
    var authenticationTime: LocalDateTime? = null
        private set
    
    var sessionId: String? = null
        private set
    
    var attributes: MutableMap<String, Any> = mutableMapOf()
        private set
    
    fun authenticate(principal: Principal, roles: Set<String> = emptySet(), permissions: Set<String> = emptySet()) {
        this.principal = principal
        this.isAuthenticated = true
        this.roles = roles
        this.permissions = permissions
        this.authenticationTime = LocalDateTime.now()
    }
    
    fun setSession(sessionId: String) {
        this.sessionId = sessionId
    }
    
    fun hasRole(role: String): Boolean = roles.contains(role)
    
    fun hasAnyRole(vararg roles: String): Boolean = roles.any { this.roles.contains(it) }
    
    fun hasPermission(permission: String): Boolean = permissions.contains(permission)
    
    fun hasAnyPermission(vararg permissions: String): Boolean = permissions.any { this.permissions.contains(it) }
    
    fun setAttribute(key: String, value: Any) {
        attributes[key] = value
    }
    
    fun getAttribute(key: String): Any? = attributes[key]
    
    fun <T> getAttribute(key: String, type: Class<T>): T? {
        return attributes[key]?.let { value ->
            if (type.isInstance(value)) type.cast(value) else null
        }
    }
    
    fun logout() {
        principal = null
        isAuthenticated = false
        roles = emptySet()
        permissions = emptySet()
        authenticationTime = null
        sessionId = null
        attributes.clear()
    }
    
    companion object {
        private const val SECURITY_CONTEXT_KEY = "forge.security.context"
        
        fun getContext(ctx: Context): SecurityContext {
            return ctx.req().getAttribute(SECURITY_CONTEXT_KEY) as? SecurityContext
                ?: SecurityContext().also { ctx.req().setAttribute(SECURITY_CONTEXT_KEY, it) }
        }
        
        fun setContext(ctx: Context, securityContext: SecurityContext) {
            ctx.req().setAttribute(SECURITY_CONTEXT_KEY, securityContext)
        }
    }
}

/**
 * Simple implementation of Principal interface
 */
data class SimplePrincipal(
    private val name: String,
    val email: String? = null,
    val id: String? = null
) : Principal {
    override fun getName(): String = name
    
    override fun toString(): String = "SimplePrincipal(name='$name', email='$email', id='$id')"
}