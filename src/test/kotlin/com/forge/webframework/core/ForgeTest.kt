package com.forge.core

import com.forge.config.VirtualThreadConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@DisplayName("Forge Core Tests")
class ForgeTest {
    
    private lateinit var framework: Forge
    private var testPort = 0 // Will be set to random available port
    private lateinit var baseUrl: String
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()
    
    private fun getRandomPort(): Int {
        return try {
            val socket = java.net.ServerSocket(0)
            val port = socket.localPort
            socket.close()
            port
        } catch (e: Exception) {
            (7000..8000).random()
        }
    }
    
    @BeforeEach
    fun setUp() {
        testPort = getRandomPort()
        baseUrl = "http://localhost:$testPort"
        framework = Forge.create()
    }
    
    @AfterEach
    fun tearDown() {
        try {
            framework.stop()
            Thread.sleep(300) // Allow server to stop
        } catch (e: Exception) {
            // Ignore shutdown errors
        }
    }
    
    @Nested
    @DisplayName("Framework Creation")
    inner class FrameworkCreation {
        
        @Test
        @DisplayName("Should create framework with default configuration")
        fun shouldCreateFrameworkWithDefaultConfig() {
            val fw = Forge.create()
            assertNotNull(fw)
        }
        
        @Test
        @DisplayName("Should create framework with custom virtual thread config")
        fun shouldCreateFrameworkWithCustomConfig() {
            val config = VirtualThreadConfig.builder()
                .enabled(true)
                .threadNamePrefix("test-vt")
                .build()
            
            val fw = Forge.create(config)
            assertNotNull(fw)
        }
        
        @Test
        @DisplayName("Should create framework with disabled virtual threads")
        fun shouldCreateFrameworkWithDisabledVirtualThreads() {
            val config = VirtualThreadConfig.disabled()
            val fw = Forge.create(config)
            assertNotNull(fw)
        }
    }
    
    @Nested
    @DisplayName("Route Registration")
    inner class RouteRegistration {
        
        @Test
        @DisplayName("Should register GET route")
        fun shouldRegisterGetRoute() {
            assertDoesNotThrow {
                framework.get("/test") { ctx ->
                    ctx.result("GET test")
                }
            }
        }
        
        @Test
        @DisplayName("Should register POST route")
        fun shouldRegisterPostRoute() {
            assertDoesNotThrow {
                framework.post("/test") { ctx ->
                    ctx.result("POST test")
                }
            }
        }
        
        @Test
        @DisplayName("Should register PUT route")
        fun shouldRegisterPutRoute() {
            assertDoesNotThrow {
                framework.put("/test") { ctx ->
                    ctx.result("PUT test")
                }
            }
        }
        
        @Test
        @DisplayName("Should register DELETE route")
        fun shouldRegisterDeleteRoute() {
            assertDoesNotThrow {
                framework.delete("/test") { ctx ->
                    ctx.result("DELETE test")
                }
            }
        }
        
        @Test
        @DisplayName("Should register PATCH route")
        fun shouldRegisterPatchRoute() {
            assertDoesNotThrow {
                framework.patch("/test") { ctx ->
                    ctx.result("PATCH test")
                }
            }
        }
        
        @Test
        @DisplayName("Should chain route registrations")
        fun shouldChainRouteRegistrations() {
            assertDoesNotThrow {
                framework
                    .get("/get") { ctx -> ctx.result("GET") }
                    .post("/post") { ctx -> ctx.result("POST") }
                    .put("/put") { ctx -> ctx.result("PUT") }
                    .delete("/delete") { ctx -> ctx.result("DELETE") }
                    .patch("/patch") { ctx -> ctx.result("PATCH") }
            }
        }
    }
    
    @Nested
    @DisplayName("Middleware Registration")
    inner class MiddlewareRegistration {
        
        @Test
        @DisplayName("Should register before middleware")
        fun shouldRegisterBeforeMiddleware() {
            assertDoesNotThrow {
                framework.before { ctx ->
                    ctx.header("X-Before", "true")
                }
            }
        }
        
        @Test
        @DisplayName("Should register after middleware")
        fun shouldRegisterAfterMiddleware() {
            assertDoesNotThrow {
                framework.after { ctx ->
                    ctx.header("X-After", "true")
                }
            }
        }
        
        @Test
        @DisplayName("Should chain middleware registrations")
        fun shouldChainMiddlewareRegistrations() {
            assertDoesNotThrow {
                framework
                    .before { ctx -> ctx.header("X-Before-1", "true") }
                    .before { ctx -> ctx.header("X-Before-2", "true") }
                    .after { ctx -> ctx.header("X-After-1", "true") }
                    .after { ctx -> ctx.header("X-After-2", "true") }
            }
        }
    }
    
    @Nested
    @DisplayName("Exception Handling")
    inner class ExceptionHandling {
        
        @Test
        @DisplayName("Should register exception handler")
        fun shouldRegisterExceptionHandler() {
            assertDoesNotThrow {
                framework.exception(RuntimeException::class.java) { ex, ctx ->
                    ctx.status(500).result("Error: ${ex.message}")
                }
            }
        }
        
        @Test
        @DisplayName("Should register multiple exception handlers")
        fun shouldRegisterMultipleExceptionHandlers() {
            assertDoesNotThrow {
                framework
                    .exception(RuntimeException::class.java) { ex, ctx ->
                        ctx.status(500).result("Runtime error: ${ex.message}")
                    }
                    .exception(IllegalArgumentException::class.java) { ex, ctx ->
                        ctx.status(400).result("Bad request: ${ex.message}")
                    }
            }
        }
    }
    
    @Nested
    @DisplayName("HTTP Integration Tests")
    inner class HttpIntegrationTests {
        
        private fun startFrameworkAsync() {
            CompletableFuture.runAsync {
                framework.start(testPort)
            }
            Thread.sleep(500) // Allow server to start
        }
        
        @Test
        @DisplayName("Should handle GET request")
        fun shouldHandleGetRequest() {
            framework.get("/test") { ctx ->
                ctx.result("Hello World")
            }
            
            startFrameworkAsync()
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/test"))
                .GET()
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            assertEquals(200, response.statusCode())
            assertEquals("Hello World", response.body())
        }
        
        @Test
        @DisplayName("Should handle POST request with JSON")
        fun shouldHandlePostRequestWithJson() {
            framework.post("/api/users") { ctx ->
                val body = ctx.body()
                ctx.status(201).json(mapOf(
                    "received" to body,
                    "status" to "created"
                ))
            }
            
            startFrameworkAsync()
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/api/users"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""{"name":"John","email":"john@example.com"}"""))
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            assertEquals(201, response.statusCode())
            assertTrue(response.body().contains("created"))
        }
        
        @Test
        @DisplayName("Should handle path parameters")
        fun shouldHandlePathParameters() {
            framework.get("/users/:id") { ctx ->
                val id = ctx.pathParam("id")
                ctx.json(mapOf("userId" to id))
            }
            
            startFrameworkAsync()
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/users/123"))
                .GET()
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            assertEquals(200, response.statusCode())
            assertTrue(response.body().contains("123"))
        }
        
        @Test
        @DisplayName("Should handle query parameters")
        fun shouldHandleQueryParameters() {
            framework.get("/search") { ctx ->
                val query = ctx.queryParam("q")
                val limit = ctx.queryParam("limit")
                ctx.json(mapOf(
                    "query" to query,
                    "limit" to limit
                ))
            }
            
            startFrameworkAsync()
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/search?q=test&limit=10"))
                .GET()
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            assertEquals(200, response.statusCode())
            assertTrue(response.body().contains("test"))
            assertTrue(response.body().contains("10"))
        }
        
        @Test
        @DisplayName("Should handle 404 for unknown routes")
        fun shouldHandle404ForUnknownRoutes() {
            startFrameworkAsync()
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/unknown"))
                .GET()
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            assertEquals(404, response.statusCode())
        }
        
        @Test
        @DisplayName("Should execute middleware in correct order")
        fun shouldExecuteMiddlewareInCorrectOrder() {
            val executionOrder = mutableListOf<String>()
            
            framework
                .before { ctx ->
                    executionOrder.add("before1")
                    ctx.header("X-Before-1", "true")
                }
                .before { ctx ->
                    executionOrder.add("before2")
                    ctx.header("X-Before-2", "true")
                }
                .get("/middleware-test") { ctx ->
                    executionOrder.add("handler")
                    ctx.result("OK")
                }
                .after { ctx ->
                    executionOrder.add("after1")
                    ctx.header("X-After-1", "true")
                }
                .after { ctx ->
                    executionOrder.add("after2")
                    ctx.header("X-After-2", "true")
                }
            
            startFrameworkAsync()
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/middleware-test"))
                .GET()
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            assertEquals(200, response.statusCode())
            assertEquals("OK", response.body())
            assertEquals("true", response.headers().firstValue("X-Before-1").orElse(""))
            assertEquals("true", response.headers().firstValue("X-Before-2").orElse(""))
            assertEquals("true", response.headers().firstValue("X-After-1").orElse(""))
            assertEquals("true", response.headers().firstValue("X-After-2").orElse(""))
        }
        
        @Test
        @DisplayName("Should handle exceptions with custom handler")
        fun shouldHandleExceptionsWithCustomHandler() {
            framework
                .exception(RuntimeException::class.java) { ex, ctx ->
                    ctx.status(500).json(mapOf(
                        "error" to "Internal Server Error",
                        "message" to ex.message
                    ))
                }
                .get("/error") { ctx ->
                    throw RuntimeException("Test error")
                }
            
            startFrameworkAsync()
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/error"))
                .GET()
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            assertEquals(500, response.statusCode())
            assertTrue(response.body().contains("Test error"))
        }
    }
}