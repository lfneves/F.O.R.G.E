package com.webframework.spring

import com.webframework.config.VirtualThreadConfig
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "webframework")
data class WebFrameworkProperties(
    var port: Int = 8080,
    var contextPath: String = "/",
    var virtualThreads: VirtualThreadProperties = VirtualThreadProperties()
) {
    data class VirtualThreadProperties(
        var enabled: Boolean = true,
        var threadNamePrefix: String = "vt-webframework",
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