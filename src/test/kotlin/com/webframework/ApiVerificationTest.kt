package com.webframework

import com.webframework.core.WebFramework
import com.webframework.config.VirtualThreadConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

@DisplayName("API Verification and End-to-End Tests")
class ApiVerificationTest {
    
    private lateinit var framework: WebFramework
    private val testPort = 8083
    private val baseUrl = "http://localhost:$testPort"
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    
    @BeforeEach
    fun setUp() {
        framework = WebFramework.create()
    }
    
    @AfterEach
    fun tearDown() {
        framework.stop()
        Thread.sleep(200) // Allow server to stop completely
    }
    
    private fun startFrameworkAsync() {
        CompletableFuture.runAsync {
            framework.start(testPort)
        }
        Thread.sleep(1000) // Allow server to start
    }
    
    @Nested
    @DisplayName("Complete API Workflow")
    inner class CompleteApiWorkflow {
        
        @Test
        @DisplayName("Should handle complete REST API workflow")
        fun shouldHandleCompleteRestApiWorkflow() {
            val users = mutableListOf<Map<String, Any>>()
            var nextId = 1
            
            // Setup complete REST API
            framework
                .get("/api/users") { ctx ->
                    ctx.json(mapOf(
                        "users" to users,
                        "count" to users.size,
                        "timestamp" to System.currentTimeMillis()
                    ))
                }
                .post("/api/users") { ctx ->
                    val userData = ctx.bodyAsClass(Map::class.java)
                    val user = mutableMapOf<String, Any>(
                        "id" to nextId++,
                        "name" to (userData["name"] ?: "Unknown"),
                        "email" to (userData["email"] ?: "unknown@example.com"),
                        "createdAt" to System.currentTimeMillis()
                    )
                    users.add(user)
                    ctx.status(201).json(user)
                }
                .get("/api/users/:id") { ctx ->
                    val id = ctx.pathParam("id")?.toIntOrNull()
                    val user = users.find { (it["id"] as Int) == id }
                    if (user != null) {
                        ctx.json(user)
                    } else {
                        ctx.status(404).json(mapOf("error" to "User not found"))
                    }
                }
                .put("/api/users/:id") { ctx ->
                    val id = ctx.pathParam("id")?.toIntOrNull()
                    val userIndex = users.indexOfFirst { (it["id"] as Int) == id }
                    if (userIndex != -1) {
                        val updateData = ctx.bodyAsClass(Map::class.java)
                        val existingUser = users[userIndex].toMutableMap()
                        existingUser["name"] = updateData["name"] ?: existingUser["name"]
                        existingUser["email"] = updateData["email"] ?: existingUser["email"]
                        existingUser["updatedAt"] = System.currentTimeMillis()
                        users[userIndex] = existingUser
                        ctx.json(existingUser)
                    } else {
                        ctx.status(404).json(mapOf("error" to "User not found"))
                    }
                }
                .delete("/api/users/:id") { ctx ->
                    val id = ctx.pathParam("id")?.toIntOrNull()
                    val removed = users.removeIf { (it["id"] as Int) == id }
                    if (removed) {
                        ctx.status(204).result("")
                    } else {
                        ctx.status(404).json(mapOf("error" to "User not found"))
                    }
                }
            
            startFrameworkAsync()
            
            // Test CREATE
            val createRequest = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/api/users"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""{"name":"John Doe","email":"john@example.com"}"""))
                .build()
            
            val createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString())
            assertEquals(201, createResponse.statusCode())
            assertTrue(createResponse.body().contains("John Doe"))
            
            // Test READ ALL
            val getAllRequest = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/api/users"))
                .GET()
                .build()
            
            val getAllResponse = httpClient.send(getAllRequest, HttpResponse.BodyHandlers.ofString())
            assertEquals(200, getAllResponse.statusCode())
            assertTrue(getAllResponse.body().contains("count"))
            
            // Test READ ONE
            val getOneRequest = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/api/users/1"))
                .GET()
                .build()
            
            val getOneResponse = httpClient.send(getOneRequest, HttpResponse.BodyHandlers.ofString())
            assertEquals(200, getOneResponse.statusCode())
            assertTrue(getOneResponse.body().contains("John Doe"))
            
            // Test UPDATE
            val updateRequest = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/api/users/1"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("""{"name":"John Smith","email":"john.smith@example.com"}"""))
                .build()
            
            val updateResponse = httpClient.send(updateRequest, HttpResponse.BodyHandlers.ofString())
            assertEquals(200, updateResponse.statusCode())
            assertTrue(updateResponse.body().contains("John Smith"))
            
            // Test DELETE
            val deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/api/users/1"))
                .DELETE()
                .build()
            
            val deleteResponse = httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString())
            assertEquals(204, deleteResponse.statusCode())
            
            // Verify DELETE worked
            val verifyDeleteRequest = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/api/users/1"))
                .GET()
                .build()
            
            val verifyDeleteResponse = httpClient.send(verifyDeleteRequest, HttpResponse.BodyHandlers.ofString())
            assertEquals(404, verifyDeleteResponse.statusCode())
        }
    }
    
    @Nested
    @DisplayName("Virtual Threads Performance Verification")
    inner class VirtualThreadsPerformanceVerification {
        
        @Test
        @DisplayName("Should demonstrate virtual threads performance benefits")
        fun shouldDemonstrateVirtualThreadsPerformanceBenefits() {
            val concurrentRequests = 100
            val requestDelay = 100L // milliseconds
            
            framework.get("/performance-test") { ctx ->
                Thread.sleep(requestDelay) // Simulate I/O
                ctx.json(mapOf(
                    "thread" to Thread.currentThread().name,
                    "isVirtual" to Thread.currentThread().isVirtual,
                    "timestamp" to System.currentTimeMillis()
                ))
            }
            
            startFrameworkAsync()
            
            val startTime = System.currentTimeMillis()
            
            // Make concurrent requests
            val futures = (1..concurrentRequests).map { requestId ->
                CompletableFuture.supplyAsync {
                    val request = HttpRequest.newBuilder()
                        .uri(URI.create("$baseUrl/performance-test"))
                        .GET()
                        .build()
                    
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                }
            }
            
            // Wait for all requests to complete
            val responses = futures.map { it.get() }
            val endTime = System.currentTimeMillis()
            
            val totalTime = endTime - startTime
            val expectedSequentialTime = concurrentRequests * requestDelay
            
            // All requests should be successful
            responses.forEach { response ->
                assertEquals(200, response.statusCode())
                assertTrue(response.body().contains("isVirtual"))
            }
            
            // Performance should be significantly better than sequential
            val speedupRatio = expectedSequentialTime.toDouble() / totalTime.toDouble()
            assertTrue(speedupRatio > 5.0, "Virtual threads should provide significant speedup. Actual ratio: $speedupRatio")
            
            println("Performance Test Results:")
            println("  Concurrent requests: $concurrentRequests")
            println("  Request delay: ${requestDelay}ms")
            println("  Total time: ${totalTime}ms")
            println("  Expected sequential time: ${expectedSequentialTime}ms")
            println("  Speedup ratio: ${String.format("%.2f", speedupRatio)}x")
        }
        
        @Test
        @DisplayName("Should handle massive concurrent load")
        fun shouldHandleMassiveConcurrentLoad() {
            val concurrentRequests = 1000
            val completedRequests = AtomicInteger(0)
            
            framework.get("/load-test") { ctx ->
                val requestNumber = completedRequests.incrementAndGet()
                ctx.json(mapOf(
                    "requestNumber" to requestNumber,
                    "thread" to Thread.currentThread().name,
                    "isVirtual" to Thread.currentThread().isVirtual
                ))
            }
            
            startFrameworkAsync()
            
            val totalTime = measureTimeMillis {
                val futures = (1..concurrentRequests).map { requestId ->
                    CompletableFuture.supplyAsync {
                        try {
                            val request = HttpRequest.newBuilder()
                                .uri(URI.create("$baseUrl/load-test"))
                                .timeout(Duration.ofSeconds(30))
                                .GET()
                                .build()
                            
                            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                
                // Wait for all requests to complete
                val responses = futures.mapNotNull { it.get() }
                
                // Verify most requests succeeded
                val successfulRequests = responses.count { it?.statusCode() == 200 }
                assertTrue(successfulRequests > concurrentRequests * 0.95, 
                    "At least 95% of requests should succeed. Actual: $successfulRequests/$concurrentRequests")
            }
            
            val throughput = (concurrentRequests * 1000) / totalTime // requests per second
            assertTrue(throughput > 100, "Should achieve reasonable throughput: $throughput req/sec")
            
            println("Load Test Results:")
            println("  Total requests: $concurrentRequests")
            println("  Total time: ${totalTime}ms")
            println("  Throughput: $throughput requests/second")
            println("  Completed requests: ${completedRequests.get()}")
        }
    }
    
    @Nested
    @DisplayName("Configuration Verification")
    inner class ConfigurationVerification {
        
        @Test
        @DisplayName("Should work with custom virtual thread configuration")
        fun shouldWorkWithCustomVirtualThreadConfiguration() {
            val customConfig = VirtualThreadConfig.builder()
                .enabled(true)
                .threadNamePrefix("api-test-vt")
                .enableMetrics(true)
                .build()
            
            val customFramework = WebFramework.create(customConfig)
            
            customFramework.get("/config-test") { ctx ->
                ctx.json(mapOf(
                    "thread" to Thread.currentThread().name,
                    "isVirtual" to Thread.currentThread().isVirtual,
                    "config" to "custom"
                ))
            }
            
            CompletableFuture.runAsync {
                customFramework.start(8084)
            }
            Thread.sleep(1000)
            
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8084/config-test"))
                    .GET()
                    .build()
                
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                
                assertEquals(200, response.statusCode())
                assertTrue(response.body().contains("api-test-vt"))
                assertTrue(response.body().contains("isVirtual"))
            } finally {
                customFramework.stop()
            }
        }
        
        @Test
        @DisplayName("Should work with disabled virtual threads")
        fun shouldWorkWithDisabledVirtualThreads() {
            val disabledConfig = VirtualThreadConfig.disabled()
            val traditionalFramework = WebFramework.create(disabledConfig)
            
            traditionalFramework.get("/traditional-test") { ctx ->
                ctx.json(mapOf(
                    "thread" to Thread.currentThread().name,
                    "isVirtual" to Thread.currentThread().isVirtual,
                    "mode" to "traditional"
                ))
            }
            
            CompletableFuture.runAsync {
                traditionalFramework.start(8085)
            }
            Thread.sleep(1000)
            
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8085/traditional-test"))
                    .GET()
                    .build()
                
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                
                assertEquals(200, response.statusCode())
                assertTrue(response.body().contains("traditional"))
                // In traditional mode, threads should not be virtual
                assertTrue(response.body().contains("\"isVirtual\":false"))
            } finally {
                traditionalFramework.stop()
            }
        }
    }
    
    @Nested
    @DisplayName("Error Handling Verification")
    inner class ErrorHandlingVerification {
        
        @Test
        @DisplayName("Should handle application errors gracefully")
        fun shouldHandleApplicationErrorsGracefully() {
            framework
                .exception(IllegalArgumentException::class.java) { ex, ctx ->
                    ctx.status(400).json(mapOf(
                        "error" to "Bad Request",
                        "message" to ex.message,
                        "type" to "IllegalArgumentException"
                    ))
                }
                .exception(RuntimeException::class.java) { ex, ctx ->
                    ctx.status(500).json(mapOf(
                        "error" to "Internal Server Error",
                        "message" to ex.message,
                        "type" to "RuntimeException"
                    ))
                }
                .get("/error/bad-request") { ctx ->
                    throw IllegalArgumentException("Invalid parameter")
                }
                .get("/error/server-error") { ctx ->
                    throw RuntimeException("Something went wrong")
                }
                .get("/error/unhandled") { ctx ->
                    throw Exception("Unhandled exception type")
                }
            
            startFrameworkAsync()
            
            // Test handled IllegalArgumentException
            val badRequestResponse = httpClient.send(
                HttpRequest.newBuilder().uri(URI.create("$baseUrl/error/bad-request")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            )
            assertEquals(400, badRequestResponse.statusCode())
            assertTrue(badRequestResponse.body().contains("Invalid parameter"))
            
            // Test handled RuntimeException
            val serverErrorResponse = httpClient.send(
                HttpRequest.newBuilder().uri(URI.create("$baseUrl/error/server-error")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            )
            assertEquals(500, serverErrorResponse.statusCode())
            assertTrue(serverErrorResponse.body().contains("Something went wrong"))
            
            // Test unhandled exception (should get default handling)
            val unhandledResponse = httpClient.send(
                HttpRequest.newBuilder().uri(URI.create("$baseUrl/error/unhandled")).GET().build(),
                HttpResponse.BodyHandlers.ofString()
            )
            assertEquals(500, unhandledResponse.statusCode())
        }
    }
    
    @Nested
    @DisplayName("Middleware Chain Verification")
    inner class MiddlewareChainVerification {
        
        @Test
        @DisplayName("Should execute complete middleware chain correctly")
        fun shouldExecuteCompleteMiddlewareChainCorrectly() {
            val executionOrder = mutableListOf<String>()
            
            framework
                .before { ctx ->
                    executionOrder.add("before-1")
                    ctx.header("X-Before-1", "executed")
                }
                .before { ctx ->
                    executionOrder.add("before-2")
                    ctx.header("X-Before-2", "executed")
                }
                .get("/middleware-chain") { ctx ->
                    executionOrder.add("handler")
                    ctx.json(mapOf(
                        "message" to "Middleware chain test",
                        "executionOrder" to executionOrder.toList()
                    ))
                }
                .after { ctx ->
                    executionOrder.add("after-1")
                    ctx.header("X-After-1", "executed")
                }
                .after { ctx ->
                    executionOrder.add("after-2")
                    ctx.header("X-After-2", "executed")
                }
            
            startFrameworkAsync()
            
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/middleware-chain"))
                .GET()
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            assertEquals(200, response.statusCode())
            
            // Verify headers were set by middleware
            assertEquals("executed", response.headers().firstValue("X-Before-1").orElse(""))
            assertEquals("executed", response.headers().firstValue("X-Before-2").orElse(""))
            assertEquals("executed", response.headers().firstValue("X-After-1").orElse(""))
            assertEquals("executed", response.headers().firstValue("X-After-2").orElse(""))
            
            // Verify execution order in response
            val responseBody = response.body()
            assertTrue(responseBody.contains("before-1"))
            assertTrue(responseBody.contains("before-2"))
            assertTrue(responseBody.contains("handler"))
            assertTrue(responseBody.contains("after-1"))
            assertTrue(responseBody.contains("after-2"))
        }
    }
}