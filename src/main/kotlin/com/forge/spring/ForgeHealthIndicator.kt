package com.forge.spring

// TODO: Spring Boot Actuator health indicator commented out for dependency compatibility
// This can be re-enabled when Spring Boot Actuator dependencies are properly configured

/*
import com.forge.core.Forge
import org.springframework.boot.actuator.health.Health
import org.springframework.boot.actuator.health.HealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.stereotype.Component

@Component
@ConditionalOnClass(name = ["org.springframework.boot.actuator.health.HealthIndicator"])
class ForgeHealthIndicator(
    private val forge: Forge,
    private val properties: ForgeProperties
) : HealthIndicator {
    
    override fun health(): Health {
        return try {
            val details = mapOf(
                "port" to properties.port,
                "virtualThreadsEnabled" to properties.virtualThreads.enabled,
                "threadNamePrefix" to properties.virtualThreads.threadNamePrefix,
                "contextPath" to properties.contextPath
            )
            
            Health.up()
                .withDetails(details)
                .build()
        } catch (e: Exception) {
            Health.down()
                .withException(e)
                .build()
        }
    }
}
*/