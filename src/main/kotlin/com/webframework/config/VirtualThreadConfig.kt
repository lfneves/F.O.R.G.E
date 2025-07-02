package com.webframework.config

data class VirtualThreadConfig(
    val enabled: Boolean = true,
    val threadNamePrefix: String = "vt-webframework",
    val maxConcurrentTasks: Int = -1,
    val enableMetrics: Boolean = false,
    val shutdownTimeoutMs: Long = 5000
) {
    companion object {
        fun builder(): VirtualThreadConfigBuilder = VirtualThreadConfigBuilder()
        
        fun default(): VirtualThreadConfig = VirtualThreadConfig()
        
        fun disabled(): VirtualThreadConfig = VirtualThreadConfig(enabled = false)
    }
}

class VirtualThreadConfigBuilder {
    private var enabled: Boolean = true
    private var threadNamePrefix: String = "vt-webframework"
    private var maxConcurrentTasks: Int = -1
    private var enableMetrics: Boolean = false
    private var shutdownTimeoutMs: Long = 5000
    
    fun enabled(enabled: Boolean): VirtualThreadConfigBuilder {
        this.enabled = enabled
        return this
    }
    
    fun threadNamePrefix(prefix: String): VirtualThreadConfigBuilder {
        this.threadNamePrefix = prefix
        return this
    }
    
    fun maxConcurrentTasks(max: Int): VirtualThreadConfigBuilder {
        this.maxConcurrentTasks = max
        return this
    }
    
    fun enableMetrics(enable: Boolean): VirtualThreadConfigBuilder {
        this.enableMetrics = enable
        return this
    }
    
    fun shutdownTimeoutMs(timeout: Long): VirtualThreadConfigBuilder {
        this.shutdownTimeoutMs = timeout
        return this
    }
    
    fun build(): VirtualThreadConfig {
        return VirtualThreadConfig(
            enabled = enabled,
            threadNamePrefix = threadNamePrefix,
            maxConcurrentTasks = maxConcurrentTasks,
            enableMetrics = enableMetrics,
            shutdownTimeoutMs = shutdownTimeoutMs
        )
    }
}