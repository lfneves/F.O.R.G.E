package com.webframework.security

import com.webframework.core.Context
import com.webframework.core.WebFramework
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import java.time.Duration
import java.time.LocalDateTime
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Comprehensive security testing suite for the WebFramework security features
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityTestSuite {
    
    private lateinit var framework: WebFramework
    
    @BeforeAll
    fun setup() {
        framework = WebFramework()
    }
    
    @AfterAll
    fun teardown() {
        framework.stop()
    }
    
    @Nested
    @DisplayName("Authentication Tests")
    inner class AuthenticationTests {
        
        @Test
        @DisplayName("Should authenticate user with valid credentials")
        fun testValidAuthentication() {
            val provider = InMemoryAuthenticationProvider()
            provider.addUser("testuser", "password123", "test@example.com", setOf("USER"), setOf("READ"))
            
            val credentials = UsernamePasswordCredentials("testuser", "password123")
            val result = provider.authenticate(credentials)
            
            assertTrue(result is AuthenticationResult.Success)
            val success = result as AuthenticationResult.Success
            assertEquals("testuser", success.principal.name)
            assertTrue(success.roles.contains("USER"))
            assertTrue(success.permissions.contains("READ"))
        }
        
        @Test
        @DisplayName("Should reject invalid credentials")
        fun testInvalidAuthentication() {
            val provider = InMemoryAuthenticationProvider()
            provider.addUser("testuser", "password123")
            
            val credentials = UsernamePasswordCredentials("testuser", "wrongpassword")
            val result = provider.authenticate(credentials)
            
            assertTrue(result is AuthenticationResult.Failure)
        }
        
        @Test
        @DisplayName("Should extract Basic Auth credentials correctly")
        fun testBasicAuthExtraction() {
            val extractor = BasicAuthCredentialsExtractor()
            val mockRequest = mock(HttpServletRequest::class.java)
            val mockResponse = mock(HttpServletResponse::class.java)
            val ctx = Context(mockRequest, mockResponse)
            
            // Mock Basic Auth header
            `when`(mockRequest.getHeader("Authorization")).thenReturn("Basic dGVzdHVzZXI6cGFzc3dvcmQ=") // testuser:password
            
            val credentials = extractor.extractCredentials(ctx)
            
            assertTrue(credentials is UsernamePasswordCredentials)
            val userPassCreds = credentials as UsernamePasswordCredentials
            assertEquals("testuser", userPassCreds.username)
            assertEquals("password", userPassCreds.password)
        }
        
        @Test
        @DisplayName("Should handle authentication middleware correctly")
        fun testAuthenticationMiddleware() {
            val provider = InMemoryAuthenticationProvider()
            provider.addUser("testuser", "password123", roles = setOf("USER"))
            
            val extractor = BasicAuthCredentialsExtractor()
            val middleware = AuthenticationMiddleware(provider, extractor)
            
            val mockRequest = mock(HttpServletRequest::class.java)
            val mockResponse = mock(HttpServletResponse::class.java)
            val ctx = Context(mockRequest, mockResponse)
            
            `when`(mockRequest.getHeader("Authorization")).thenReturn("Basic dGVzdHVzZXI6cGFzc3dvcmQxMjM=") // testuser:password123
            
            middleware.handle(ctx)
            
            val securityContext = SecurityContext.getContext(ctx)
            assertTrue(securityContext.isAuthenticated)
            assertEquals("testuser", securityContext.principal?.name)
            assertTrue(securityContext.hasRole("USER"))
        }
    }
    
    @Nested
    @DisplayName("Authorization Tests")
    inner class AuthorizationTests {
        
        @Test
        @DisplayName("Should allow access with required role")
        fun testRoleBasedAuthorization() {
            val rule = RequireRoleRule("ADMIN")
            val mockRequest = mock(HttpServletRequest::class.java)
            val mockResponse = mock(HttpServletResponse::class.java)
            val ctx = Context(mockRequest, mockResponse)
            
            val securityContext = SecurityContext.getContext(ctx)
            securityContext.authenticate(SimplePrincipal("admin"), setOf("ADMIN"))
            
            val decision = rule.evaluate(ctx, securityContext)
            assertTrue(decision is AuthorizationDecision.Allow)
        }
        
        @Test
        @DisplayName("Should deny access without required role")
        fun testRoleBasedAuthorizationDenied() {
            val rule = RequireRoleRule("ADMIN")
            val mockRequest = mock(HttpServletRequest::class.java)
            val mockResponse = mock(HttpServletResponse::class.java)
            val ctx = Context(mockRequest, mockResponse)
            
            val securityContext = SecurityContext.getContext(ctx)
            securityContext.authenticate(SimplePrincipal("user"), setOf("USER"))
            
            val decision = rule.evaluate(ctx, securityContext)
            assertTrue(decision is AuthorizationDecision.Deny)
        }
        
        @Test
        @DisplayName("Should handle permission-based authorization")
        fun testPermissionBasedAuthorization() {
            val rule = RequirePermissionRule("READ", "WRITE")
            val mockRequest = mock(HttpServletRequest::class.java)
            val mockResponse = mock(HttpServletResponse::class.java)
            val ctx = Context(mockRequest, mockResponse)
            
            val securityContext = SecurityContext.getContext(ctx)
            securityContext.authenticate(SimplePrincipal("user"), emptySet(), setOf("READ", "WRITE", "DELETE"))
            
            val decision = rule.evaluate(ctx, securityContext)
            assertTrue(decision is AuthorizationDecision.Allow)
        }
        
        @Test
        @DisplayName("Should handle authorization middleware")
        fun testAuthorizationMiddleware() {
            val rules = listOf(RequireAuthenticationRule(), RequireRoleRule("USER"))
            val middleware = AuthorizationMiddleware(rules)
            
            val mockRequest = mock(HttpServletRequest::class.java)
            val mockResponse = mock(HttpServletResponse::class.java)
            val ctx = Context(mockRequest, mockResponse)
            
            val securityContext = SecurityContext.getContext(ctx)
            securityContext.authenticate(SimplePrincipal("testuser"), setOf("USER"))
            
            // Should not throw exception or set error status
            assertDoesNotThrow { middleware.handle(ctx) }
        }
    }
    
    @Nested
    @DisplayName("JWT Tests")
    inner class JWTTests {
        
        private val jwtConfig = JWTConfig(
            secret = "test-secret-key-for-jwt-testing",
            issuer = "test-issuer",
            expirationMinutes = 60
        )
        
        @Test
        @DisplayName("Should create and validate JWT token")
        fun testJWTCreationAndValidation() {
            val jwtService = JWTService(jwtConfig)
            
            val token = jwtService.createToken(
                subject = "testuser",
                roles = setOf("USER", "ADMIN"),
                permissions = setOf("READ", "WRITE")
            )
            
            assertNotNull(token.token)
            assertEquals("testuser", token.claims.subject)
            assertTrue(token.claims.roles.contains("USER"))
            assertTrue(token.claims.permissions.contains("READ"))
            
            val validationResult = jwtService.validateToken(token.token)
            assertTrue(validationResult is JWTValidationResult.Valid)
        }
        
        @Test
        @DisplayName("Should reject expired JWT token")
        fun testExpiredJWTToken() {
            val shortLivedConfig = jwtConfig.copy(expirationMinutes = 0) // Immediately expired
            val jwtService = JWTService(shortLivedConfig)
            
            val token = jwtService.createToken("testuser")
            
            // Wait a moment to ensure expiration
            Thread.sleep(1000)
            
            val validationResult = jwtService.validateToken(token.token)
            assertTrue(validationResult is JWTValidationResult.Invalid)
        }
        
        @Test
        @DisplayName("Should reject tampered JWT token")
        fun testTamperedJWTToken() {
            val jwtService = JWTService(jwtConfig)
            val token = jwtService.createToken("testuser")
            
            // Tamper with the token
            val tamperedToken = token.token.dropLast(5) + "XXXX"
            
            val validationResult = jwtService.validateToken(tamperedToken)
            assertTrue(validationResult is JWTValidationResult.Invalid)
        }
        
        @Test
        @DisplayName("Should authenticate with JWT provider")
        fun testJWTAuthenticationProvider() {
            val jwtService = JWTService(jwtConfig)
            val provider = JWTAuthenticationProvider(jwtService)
            
            val token = jwtService.createToken("testuser", setOf("USER"))
            val credentials = TokenCredentials(token.token)
            
            val result = provider.authenticate(credentials)
            assertTrue(result is AuthenticationResult.Success)
            
            val success = result as AuthenticationResult.Success
            assertEquals("testuser", success.principal.name)
            assertTrue(success.roles.contains("USER"))
        }
    }
    
    @Nested
    @DisplayName("CORS Tests")
    inner class CORSTests {
        
        @Test
        @DisplayName("Should handle simple CORS request")
        fun testSimpleCORSRequest() {
            val config = CORSConfig(
                allowedOrigins = setOf("https://example.com"),
                allowedMethods = setOf("GET", "POST")
            )
            val middleware = CORSMiddleware(config)
            
            val mockRequest = mock(HttpServletRequest::class.java)
            val mockResponse = mock(HttpServletResponse::class.java)
            val ctx = Context(mockRequest, mockResponse)
            
            `when`(mockRequest.getHeader("Origin")).thenReturn("https://example.com")
            `when`(mockRequest.method).thenReturn("GET")
            
            middleware.handle(ctx)
            
            verify(mockResponse).setHeader("Access-Control-Allow-Origin", "https://example.com")
        }
        
        @Test
        @DisplayName("Should handle preflight CORS request")
        fun testPreflightCORSRequest() {
            val config = CORSConfig(
                allowedOrigins = setOf("https://example.com"),
                allowedMethods = setOf("GET", "POST", "PUT")
            )
            val middleware = CORSMiddleware(config)
            
            val mockRequest = mock(HttpServletRequest::class.java)
            val mockResponse = mock(HttpServletResponse::class.java)
            val ctx = Context(mockRequest, mockResponse)
            
            `when`(mockRequest.getHeader("Origin")).thenReturn("https://example.com")
            `when`(mockRequest.method).thenReturn("OPTIONS")
            
            middleware.handle(ctx)
            
            verify(mockResponse).setHeader("Access-Control-Allow-Methods", "GET, POST, PUT")
            verify(mockResponse).status = 204
        }
        
        @Test
        @DisplayName("Should reject unauthorized origin")
        fun testUnauthorizedOrigin() {
            val config = CORSConfig(
                allowedOrigins = setOf("https://allowed.com")
            )
            val middleware = CORSMiddleware(config)
            
            val mockRequest = mock(HttpServletRequest::class.java)
            val mockResponse = mock(HttpServletResponse::class.java)
            val ctx = Context(mockRequest, mockResponse)
            
            `when`(mockRequest.getHeader("Origin")).thenReturn("https://malicious.com")
            
            middleware.handle(ctx)
            
            verify(mockResponse, never()).setHeader(eq("Access-Control-Allow-Origin"), any())
        }
    }
    
    @Nested
    @DisplayName("Rate Limiting Tests")
    inner class RateLimitingTests {
        
        @Test
        @DisplayName("Should allow requests within rate limit")
        fun testRateLimitAllow() {
            val config = RateLimitConfig(
                strategy = RateLimitStrategy.FIXED_WINDOW,
                requestsPerWindow = 5,
                windowDuration = Duration.ofMinutes(1)
            )
            val rateLimiter = RateLimiterFactory.create(config)
            
            // Make 5 requests - all should be allowed
            repeat(5) {
                assertTrue(rateLimiter.isAllowed("test-key"))
            }
        }
        
        @Test
        @DisplayName("Should block requests exceeding rate limit")
        fun testRateLimitBlock() {
            val config = RateLimitConfig(
                strategy = RateLimitStrategy.FIXED_WINDOW,
                requestsPerWindow = 3,
                windowDuration = Duration.ofMinutes(1)
            )
            val rateLimiter = RateLimiterFactory.create(config)
            
            // Make 3 requests - should be allowed
            repeat(3) {
                assertTrue(rateLimiter.isAllowed("test-key"))
            }
            
            // 4th request should be blocked
            assertFalse(rateLimiter.isAllowed("test-key"))
        }
        
        @Test
        @DisplayName("Should handle sliding window rate limiting")
        fun testSlidingWindowRateLimit() {
            val config = RateLimitConfig(
                strategy = RateLimitStrategy.SLIDING_WINDOW,
                requestsPerWindow = 2,
                windowDuration = Duration.ofSeconds(1)
            )
            val rateLimiter = RateLimiterFactory.create(config)
            
            assertTrue(rateLimiter.isAllowed("test-key"))
            assertTrue(rateLimiter.isAllowed("test-key"))
            assertFalse(rateLimiter.isAllowed("test-key"))
            
            // Wait for window to slide
            Thread.sleep(1100)
            assertTrue(rateLimiter.isAllowed("test-key"))
        }
        
        @Test
        @DisplayName("Should handle token bucket rate limiting")
        fun testTokenBucketRateLimit() {
            val config = RateLimitConfig(
                strategy = RateLimitStrategy.TOKEN_BUCKET,
                requestsPerWindow = 3
            )
            val rateLimiter = RateLimiterFactory.create(config)
            
            // Initial bucket should have 3 tokens
            assertTrue(rateLimiter.isAllowed("test-key"))
            assertTrue(rateLimiter.isAllowed("test-key"))
            assertTrue(rateLimiter.isAllowed("test-key"))
            assertFalse(rateLimiter.isAllowed("test-key"))
        }
    }
    
    @Nested
    @DisplayName("Request Validation Tests")
    inner class RequestValidationTests {
        
        @Test
        @DisplayName("Should detect XSS attempts")
        fun testXSSDetection() {
            val rule = XSSProtectionRule()
            
            val maliciousInputs = listOf(
                "<script>alert('xss')</script>",
                "javascript:alert('xss')",
                "<img src=x onerror=alert('xss')>",
                "<iframe src=\"javascript:alert('xss')\"></iframe>"
            )
            
            maliciousInputs.forEach { input ->
                val result = rule.validate(input)
                assertTrue(result is ValidationResult.Invalid, "Should detect XSS in: $input")
            }
        }
        
        @Test
        @DisplayName("Should detect SQL injection attempts")
        fun testSQLInjectionDetection() {
            val rule = SQLInjectionProtectionRule()
            
            val maliciousInputs = listOf(
                "1' OR '1'='1",
                "admin'--",
                "1; DROP TABLE users;",
                "UNION SELECT * FROM passwords"
            )
            
            maliciousInputs.forEach { input ->
                val result = rule.validate(input)
                assertTrue(result is ValidationResult.Invalid, "Should detect SQL injection in: $input")
            }
        }
        
        @Test
        @DisplayName("Should detect path traversal attempts")
        fun testPathTraversalDetection() {
            val rule = PathTraversalProtectionRule()
            
            val maliciousInputs = listOf(
                "../../../etc/passwd",
                "..\\..\\windows\\system32",
                "%2e%2e%2f",
                "....//....//etc/passwd"
            )
            
            maliciousInputs.forEach { input ->
                val result = rule.validate(input)
                assertTrue(result is ValidationResult.Invalid, "Should detect path traversal in: $input")
            }
        }
        
        @Test
        @DisplayName("Should validate file extensions")
        fun testFileExtensionValidation() {
            val rule = FileExtensionValidationRule(
                allowedExtensions = setOf("jpg", "png", "pdf"),
                deniedExtensions = setOf("exe", "bat", "js")
            )
            
            assertTrue(rule.validate("document.pdf") is ValidationResult.Valid)
            assertTrue(rule.validate("image.jpg") is ValidationResult.Valid)
            assertTrue(rule.validate("malware.exe") is ValidationResult.Invalid)
            assertTrue(rule.validate("script.js") is ValidationResult.Invalid)
        }
        
        @Test
        @DisplayName("Should sanitize HTML content")
        fun testHTMLSanitization() {
            val maliciousHtml = """
                <div>Safe content</div>
                <script>alert('xss')</script>
                <p onclick="maliciousCode()">Paragraph</p>
                <iframe src="javascript:alert('xss')"></iframe>
            """.trimIndent()
            
            val sanitized = HTMLSanitizer.sanitize(maliciousHtml)
            
            assertTrue(sanitized.contains("<div>Safe content</div>"))
            assertFalse(sanitized.contains("<script>"))
            assertFalse(sanitized.contains("onclick"))
            assertFalse(sanitized.contains("<iframe>"))
        }
    }
    
    @Nested
    @DisplayName("Security Headers Tests")
    inner class SecurityHeadersTests {
        
        @Test
        @DisplayName("Should set security headers")
        fun testSecurityHeaders() {
            val config = SecurityHeadersConfig(
                contentSecurityPolicy = "default-src 'self'",
                xFrameOptions = "DENY",
                strictTransportSecurity = "max-age=31536000"
            )
            val middleware = SecurityHeadersMiddleware(config)
            
            val mockRequest = mock(HttpServletRequest::class.java)
            val mockResponse = mock(HttpServletResponse::class.java)
            val ctx = Context(mockRequest, mockResponse)
            
            `when`(mockRequest.scheme).thenReturn("https")
            `when`(mockRequest.isSecure).thenReturn(true)
            
            middleware.handle(ctx)
            
            verify(mockResponse).setHeader("Content-Security-Policy", "default-src 'self'")
            verify(mockResponse).setHeader("X-Frame-Options", "DENY")
            verify(mockResponse).setHeader("Strict-Transport-Security", "max-age=31536000")
        }
        
        @Test
        @DisplayName("Should build CSP correctly")
        fun testCSPBuilder() {
            val csp = CSPBuilder()
                .defaultSrc(CSPBuilder.SELF)
                .scriptSrc(CSPBuilder.SELF, "https://trusted.com")
                .styleSrc(CSPBuilder.SELF, CSPBuilder.UNSAFE_INLINE)
                .imgSrc(CSPBuilder.SELF, CSPBuilder.DATA)
                .build()
            
            assertTrue(csp.contains("default-src 'self'"))
            assertTrue(csp.contains("script-src 'self' https://trusted.com"))
            assertTrue(csp.contains("style-src 'self' 'unsafe-inline'"))
            assertTrue(csp.contains("img-src 'self' data:"))
        }
    }
    
    @Nested
    @DisplayName("Session Management Tests")
    inner class SessionManagementTests {
        
        @Test
        @DisplayName("Should create and retrieve session")
        fun testSessionCreation() {
            val sessionManager = InMemorySessionManager(30)
            val principal = SimplePrincipal("testuser")
            
            val session = sessionManager.createSession(principal, setOf("USER"), setOf("READ"))
            
            assertNotNull(session.id)
            assertEquals("testuser", session.principal.name)
            assertTrue(session.roles.contains("USER"))
            assertTrue(session.permissions.contains("READ"))
            
            val retrievedSession = sessionManager.getSession(session.id)
            assertNotNull(retrievedSession)
            assertEquals(session.id, retrievedSession?.id)
        }
        
        @Test
        @DisplayName("Should handle session expiration")
        fun testSessionExpiration() {
            val sessionManager = InMemorySessionManager(0) // Immediate expiration
            val principal = SimplePrincipal("testuser")
            
            val session = sessionManager.createSession(principal, setOf("USER"), setOf("READ"))
            
            // Session should be expired immediately
            Thread.sleep(100)
            val retrievedSession = sessionManager.getSession(session.id)
            assertNull(retrievedSession)
        }
        
        @Test
        @DisplayName("Should invalidate session")
        fun testSessionInvalidation() {
            val sessionManager = InMemorySessionManager(30)
            val principal = SimplePrincipal("testuser")
            
            val session = sessionManager.createSession(principal, setOf("USER"), setOf("READ"))
            sessionManager.invalidateSession(session.id)
            
            val retrievedSession = sessionManager.getSession(session.id)
            assertNull(retrievedSession)
        }
        
        @Test
        @DisplayName("Should touch session on access")
        fun testSessionTouch() {
            val sessionManager = InMemorySessionManager(30)
            val principal = SimplePrincipal("testuser")
            
            val session = sessionManager.createSession(principal, setOf("USER"), setOf("READ"))
            val originalExpiry = session.expiresAt
            
            Thread.sleep(100)
            
            val retrievedSession = sessionManager.getSession(session.id)
            assertNotNull(retrievedSession)
            assertTrue(retrievedSession!!.expiresAt.isAfter(originalExpiry))
        }
    }
    
    @Nested
    @DisplayName("Integration Tests")
    inner class IntegrationTests {
        
        @Test
        @DisplayName("Should handle complete security flow")
        fun testCompleteSecurityFlow() {
            val jwtConfig = JWTConfig(
                secret = "integration-test-secret",
                issuer = "test-framework"
            )
            
            val sessionManager = InMemorySessionManager()
            
            val config = jwtSessionConfig {
                jwtConfig(jwtConfig)
                inMemorySessions()
            }
            
            val providers = config.createAuthenticationProviders()
            val extractors = config.createCredentialsExtractors()
            
            assertTrue(providers.isNotEmpty())
            assertTrue(extractors.isNotEmpty())
        }
    }
}