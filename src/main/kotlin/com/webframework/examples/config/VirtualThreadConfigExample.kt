package com.webframework.examples.config

import com.webframework.core.WebFramework
import com.webframework.config.VirtualThreadConfig

fun main() {
    val customConfig = VirtualThreadConfig.builder()
        .enabled(true)
        .threadNamePrefix("my-app-vt")
        .enableMetrics(true)
        .shutdownTimeoutMs(10000)
        .build()
    
    val framework = WebFramework.create(customConfig)
    
    framework.get("/config-example") { ctx ->
        ctx.json(mapOf(
            "message" to "Using custom virtual thread configuration",
            "threadInfo" to mapOf(
                "name" to Thread.currentThread().name,
                "isVirtual" to false, // Thread.currentThread().isVirtual // Java 21 feature commented out
                "id" to Thread.currentThread().id // Java 17 compatible
            )
        ))
    }
    
    val disabledConfig = VirtualThreadConfig.disabled()
    val traditionalFramework = WebFramework.create(disabledConfig)
    
    traditionalFramework.get("/traditional") { ctx ->
        ctx.json(mapOf(
            "message" to "Using traditional platform threads",
            "threadInfo" to mapOf(
                "name" to Thread.currentThread().name,
                "isVirtual" to false, // Thread.currentThread().isVirtual // Java 21 feature commented out
                "id" to Thread.currentThread().id // Java 17 compatible
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