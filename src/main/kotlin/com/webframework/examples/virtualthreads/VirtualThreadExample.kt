package com.webframework.examples.virtualthreads

import com.webframework.core.WebFramework
import com.webframework.config.VirtualThreadConfig
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

fun main() {
    val config = VirtualThreadConfig.builder()
        .enabled(true)
        .threadNamePrefix("example-vt")
        .enableMetrics(true)
        .build()
    
    val framework = WebFramework.create(config)
    
    framework.get("/") { ctx ->
        ctx.json(mapOf("message" to "Virtual Threads Web Framework", "timestamp" to LocalDateTime.now()))
    }
    
    framework.get("/sleep/:seconds") { ctx ->
        val seconds = ctx.pathParam("seconds")?.toLongOrNull() ?: 1L
        
        Thread.sleep(seconds * 1000)
        
        ctx.json(mapOf(
            "message" to "Slept for $seconds seconds",
            "thread" to Thread.currentThread().toString(),
            "timestamp" to LocalDateTime.now()
        ))
    }
    
    framework.get("/concurrent-processing") { ctx ->
        val tasks = (1..10).map { taskId ->
            CompletableFuture.supplyAsync {
                val processingTime = Random.nextLong(100, 1000)
                Thread.sleep(processingTime)
                mapOf(
                    "taskId" to taskId,
                    "processingTime" to processingTime,
                    "thread" to Thread.currentThread().toString()
                )
            }
        }
        
        val results = CompletableFuture.allOf(*tasks.toTypedArray())
            .thenApply { tasks.map { it.get() } }
            .get()
        
        ctx.json(mapOf(
            "results" to results,
            "totalTasks" to 10,
            "timestamp" to LocalDateTime.now()
        ))
    }
    
    framework.get("/blocking-io-simulation") { ctx ->
        val simulateDbCall = {
            Thread.sleep(500)
            mapOf("data" to "Database result", "latency" to "500ms")
        }
        
        val simulateApiCall = {
            Thread.sleep(300)
            mapOf("data" to "API response", "latency" to "300ms")
        }
        
        val simulateFileRead = {
            Thread.sleep(200)
            mapOf("data" to "File content", "latency" to "200ms")
        }
        
        val startTime = System.currentTimeMillis()
        
        val dbResult = CompletableFuture.supplyAsync(simulateDbCall)
        val apiResult = CompletableFuture.supplyAsync(simulateApiCall)
        val fileResult = CompletableFuture.supplyAsync(simulateFileRead)
        
        val allResults = CompletableFuture.allOf(dbResult, apiResult, fileResult)
            .thenApply {
                mapOf(
                    "database" to dbResult.get(),
                    "api" to apiResult.get(),
                    "file" to fileResult.get()
                )
            }.get()
        
        val totalTime = System.currentTimeMillis() - startTime
        
        ctx.json(mapOf(
            "results" to allResults,
            "totalExecutionTime" to "${totalTime}ms",
            "thread" to Thread.currentThread().toString(),
            "timestamp" to LocalDateTime.now()
        ))
    }
    
    framework.get("/thread-info") { ctx ->
        val currentThread = Thread.currentThread()
        ctx.json(mapOf(
            "threadName" to currentThread.name,
            "threadId" to currentThread.threadId(),
            "isVirtual" to currentThread.isVirtual,
            "threadGroup" to currentThread.threadGroup?.name,
            "activeCount" to Thread.activeCount(),
            "timestamp" to LocalDateTime.now()
        ))
    }
    
    framework.get("/load-test") { ctx ->
        val iterations = ctx.queryParam("iterations")?.toIntOrNull() ?: 100
        val delay = ctx.queryParam("delay")?.toLongOrNull() ?: 10L
        
        val startTime = System.currentTimeMillis()
        val tasks = (1..iterations).map { i ->
            CompletableFuture.supplyAsync {
                Thread.sleep(delay)
                "Task $i completed on ${Thread.currentThread().name}"
            }
        }
        
        val results = CompletableFuture.allOf(*tasks.toTypedArray())
            .thenApply { tasks.map { it.get() } }
            .get()
        
        val totalTime = System.currentTimeMillis() - startTime
        
        ctx.json(mapOf(
            "completedTasks" to results.size,
            "totalExecutionTime" to "${totalTime}ms",
            "averageTimePerTask" to "${totalTime / iterations}ms",
            "virtualThreadsUsed" to results.count { it.contains("example-vt-") },
            "timestamp" to LocalDateTime.now()
        ))
    }
    
    println("Starting Virtual Threads Web Framework on port 8080...")
    println("Try these endpoints:")
    println("  GET /                           - Basic info")
    println("  GET /sleep/3                    - Sleep for 3 seconds")
    println("  GET /concurrent-processing      - Concurrent task processing")
    println("  GET /blocking-io-simulation     - Simulate blocking I/O operations")
    println("  GET /thread-info                - Current thread information")
    println("  GET /load-test?iterations=1000  - Load test with virtual threads")
    
    framework.start(8080)
}