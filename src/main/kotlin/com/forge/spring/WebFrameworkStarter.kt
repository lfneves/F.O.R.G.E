package com.forge.spring

import com.forge.core.WebFramework
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import jakarta.annotation.PreDestroy

@Component
class WebFrameworkStarter(
    private val webFramework: WebFramework,
    private val properties: WebFrameworkProperties
) : ApplicationListener<ContextRefreshedEvent> {
    
    private val logger = LoggerFactory.getLogger(WebFrameworkStarter::class.java)
    private var started = false
    
    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        if (!started) {
            startWebFramework()
            started = true
        }
    }
    
    private fun startWebFramework() {
        try {
            logger.info("Starting FORGE on port ${properties.port}")
            Thread {
                webFramework.start(properties.port)
            }.start()
        } catch (e: Exception) {
            logger.error("Failed to start FORGE", e)
            throw e
        }
    }
    
    @PreDestroy
    fun stopWebFramework() {
        if (started) {
            logger.info("Stopping FORGE")
            webFramework.stop()
        }
    }
}