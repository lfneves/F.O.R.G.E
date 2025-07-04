package com.forge.spring

import com.forge.core.Forge
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import jakarta.annotation.PreDestroy

@Component
class ForgeStarter(
    private val forge: Forge,
    private val properties: ForgeProperties
) : ApplicationListener<ContextRefreshedEvent> {
    
    private val logger = LoggerFactory.getLogger(ForgeStarter::class.java)
    private var started = false
    
    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        if (!started) {
            startForge()
            started = true
        }
    }
    
    private fun startForge() {
        try {
            logger.info("Starting FORGE on port ${properties.port}")
            Thread {
                forge.start(properties.port)
            }.start()
        } catch (e: Exception) {
            logger.error("Failed to start FORGE", e)
            throw e
        }
    }
    
    @PreDestroy
    fun stopForge() {
        if (started) {
            logger.info("Stopping FORGE")
            forge.stop()
        }
    }
}