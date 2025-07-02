package com.webframework.security

import com.webframework.core.Context
import com.webframework.routing.Handler
import java.security.Principal
import java.util.Base64

/**
 * Authentication provider interface
 */
interface AuthenticationProvider {
    fun authenticate(credentials: Credentials): AuthenticationResult
    fun supports(credentialsType: Class<out Credentials>): Boolean
}

/**
 * Base class for credentials
 */
abstract class Credentials

/**
 * Username/password credentials
 */
data class UsernamePasswordCredentials(
    val username: String,
    val password: String
) : Credentials()

/**
 * Token-based credentials
 */
data class TokenCredentials(
    val token: String,
    val type: String = "Bearer"
) : Credentials()

/**
 * API Key credentials
 */
data class ApiKeyCredentials(
    val apiKey: String,
    val keyName: String = "X-API-Key"
) : Credentials()

/**
 * Authentication result
 */
sealed class AuthenticationResult {
    data class Success(
        val principal: Principal,
        val roles: Set<String> = emptySet(),
        val permissions: Set<String> = emptySet()
    ) : AuthenticationResult()
    
    data class Failure(
        val reason: String,
        val cause: Throwable? = null
    ) : AuthenticationResult()
}

/**
 * Simple in-memory authentication provider
 */
class InMemoryAuthenticationProvider : AuthenticationProvider {
    private val users = mutableMapOf<String, UserInfo>()
    
    data class UserInfo(
        val username: String,
        val passwordHash: String,
        val email: String?,
        val roles: Set<String>,
        val permissions: Set<String>
    )
    
    fun addUser(username: String, password: String, email: String? = null, roles: Set<String> = emptySet(), permissions: Set<String> = emptySet()) {
        users[username] = UserInfo(username, hashPassword(password), email, roles, permissions)
    }
    
    private fun hashPassword(password: String): String {
        // Simple hash for demo - use proper password hashing in production (bcrypt, scrypt, etc.)
        return Base64.getEncoder().encodeToString(password.toByteArray())
    }
    
    private fun verifyPassword(password: String, hash: String): Boolean {
        return hashPassword(password) == hash
    }
    
    override fun authenticate(credentials: Credentials): AuthenticationResult {
        return when (credentials) {
            is UsernamePasswordCredentials -> {
                val userInfo = users[credentials.username]
                if (userInfo != null && verifyPassword(credentials.password, userInfo.passwordHash)) {
                    AuthenticationResult.Success(
                        principal = SimplePrincipal(userInfo.username, userInfo.email, userInfo.username),
                        roles = userInfo.roles,
                        permissions = userInfo.permissions
                    )
                } else {
                    AuthenticationResult.Failure("Invalid username or password")
                }
            }
            else -> AuthenticationResult.Failure("Unsupported credentials type")
        }
    }
    
    override fun supports(credentialsType: Class<out Credentials>): Boolean {
        return UsernamePasswordCredentials::class.java.isAssignableFrom(credentialsType)
    }
}

/**
 * Authentication middleware
 */
class AuthenticationMiddleware(
    private val authenticationProvider: AuthenticationProvider,
    private val credentialsExtractor: CredentialsExtractor
) : Handler {
    
    override fun handle(ctx: Context) {
        val credentials = credentialsExtractor.extractCredentials(ctx)
        
        if (credentials != null && authenticationProvider.supports(credentials::class.java)) {
            when (val result = authenticationProvider.authenticate(credentials)) {
                is AuthenticationResult.Success -> {
                    val securityContext = SecurityContext.getContext(ctx)
                    securityContext.authenticate(result.principal, result.roles, result.permissions)
                }
                is AuthenticationResult.Failure -> {
                    // Authentication failed - security context remains unauthenticated
                }
            }
        }
    }
}

/**
 * Interface for extracting credentials from requests
 */
interface CredentialsExtractor {
    fun extractCredentials(ctx: Context): Credentials?
}

/**
 * Extracts credentials from Basic Authentication header
 */
class BasicAuthCredentialsExtractor : CredentialsExtractor {
    override fun extractCredentials(ctx: Context): Credentials? {
        val authHeader = ctx.header("Authorization") ?: return null
        
        if (!authHeader.startsWith("Basic ")) return null
        
        return try {
            val encodedCredentials = authHeader.substring(6)
            val decodedCredentials = String(Base64.getDecoder().decode(encodedCredentials))
            val parts = decodedCredentials.split(":", limit = 2)
            
            if (parts.size == 2) {
                UsernamePasswordCredentials(parts[0], parts[1])
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Extracts credentials from Bearer token header
 */
class BearerTokenCredentialsExtractor : CredentialsExtractor {
    override fun extractCredentials(ctx: Context): Credentials? {
        val authHeader = ctx.header("Authorization") ?: return null
        
        if (!authHeader.startsWith("Bearer ")) return null
        
        val token = authHeader.substring(7)
        return if (token.isNotBlank()) TokenCredentials(token) else null
    }
}

/**
 * Extracts credentials from API key header
 */
class ApiKeyCredentialsExtractor(
    private val headerName: String = "X-API-Key"
) : CredentialsExtractor {
    override fun extractCredentials(ctx: Context): Credentials? {
        val apiKey = ctx.header(headerName) ?: return null
        return if (apiKey.isNotBlank()) ApiKeyCredentials(apiKey, headerName) else null
    }
}

/**
 * Composite credentials extractor that tries multiple extractors
 */
class CompositeCredentialsExtractor(
    private val extractors: List<CredentialsExtractor>
) : CredentialsExtractor {
    
    constructor(vararg extractors: CredentialsExtractor) : this(extractors.toList())
    
    override fun extractCredentials(ctx: Context): Credentials? {
        for (extractor in extractors) {
            val credentials = extractor.extractCredentials(ctx)
            if (credentials != null) return credentials
        }
        return null
    }
}