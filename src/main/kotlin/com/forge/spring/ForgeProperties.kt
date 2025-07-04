package com.forge.spring

import com.forge.config.VirtualThreadConfig
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "forge")
data class ForgeProperties(
    var port: Int = 8080,
    var contextPath: String = "/",
    var virtualThreads: VirtualThreadProperties = VirtualThreadProperties()
) {
    data class VirtualThreadProperties(
        var enabled: Boolean = true,
        var threadNamePrefix: String = "vt-forge",
        var maxConcurrentTasks: Int = -1,
        var enableMetrics: Boolean = false,
        var shutdownTimeoutMs: Long = 5000
    )
    
    fun toVirtualThreadConfig(): VirtualThreadConfig {
        return VirtualThreadConfig(
            enabled = virtualThreads.enabled,
            threadNamePrefix = virtualThreads.threadNamePrefix,
            maxConcurrentTasks = virtualThreads.maxConcurrentTasks,
            enableMetrics = virtualThreads.enableMetrics,
            shutdownTimeoutMs = virtualThreads.shutdownTimeoutMs
        )
    }
}