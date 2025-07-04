package com.forge.examples.config

import com.forge.core.Forge
import com.forge.config.VirtualThreadConfig

// Helper function to check if thread is virtual using reflection
fun isVirtualThread(thread: Thread): Boolean {
    return try {
        thread::class.java.getMethod("isVirtual").invoke(thread) as Boolean
    } catch (e: Exception) {
        false
    }
}

fun main() {
    val customConfig = VirtualThreadConfig.builder()
        .enabled(true)
        .threadNamePrefix("my-app-vt")
        .enableMetrics(true)
        .shutdownTimeoutMs(10000)
        .build()
    
    val framework = Forge.create(customConfig)
    
    framework.get("/config-example") { ctx ->
        ctx.json(mapOf(
            "message" to "Using custom virtual thread configuration",
            "threadInfo" to mapOf(
                "name" to Thread.currentThread().name,
                "isVirtual" to isVirtualThread(Thread.currentThread()),
                "id" to Thread.currentThread().getId()
            )
        ))
    }
    
    val disabledConfig = VirtualThreadConfig.disabled()
    val traditionalFramework = Forge.create(disabledConfig)
    
    traditionalFramework.get("/traditional") { ctx ->
        ctx.json(mapOf(
            "message" to "Using traditional platform threads",
            "threadInfo" to mapOf(
                "name" to Thread.currentThread().name,
                "isVirtual" to isVirtualThread(Thread.currentThread()),
                "id" to Thread.currentThread().getId()
            )
        ))
    }
    
    println("Configuration examples:")
    println("Virtual Threads Enabled: ${customConfig.enabled}")
    println("Thread Name Prefix: ${customConfig.threadNamePrefix}")
    println("Metrics Enabled: ${customConfig.enableMetrics}")
    println("Shutdown Timeout: ${customConfig.shutdownTimeoutMs}ms")
    
    println("\nStarting framework with custom configuration on port 8080...")
    framework.start(8080)
}