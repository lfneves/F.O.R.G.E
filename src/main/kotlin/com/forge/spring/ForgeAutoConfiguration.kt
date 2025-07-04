package com.forge.spring

import com.forge.core.Forge
import com.forge.config.VirtualThreadConfig
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@AutoConfiguration
@ConditionalOnClass(Forge::class)
@EnableConfigurationProperties(ForgeProperties::class)
@ComponentScan(basePackages = ["com.forge.spring"])
class ForgeAutoConfiguration(
    private val properties: ForgeProperties
) {
    
    private val logger = LoggerFactory.getLogger(ForgeAutoConfiguration::class.java)
    
    @Bean
    @ConditionalOnMissingBean
    fun virtualThreadConfig(): VirtualThreadConfig {
        logger.info("Creating VirtualThreadConfig with properties: ${properties.virtualThreads}")
        return properties.toVirtualThreadConfig()
    }
    
    @Bean
    @ConditionalOnMissingBean
    fun forge(virtualThreadConfig: VirtualThreadConfig): Forge {
        logger.info("Creating Forge with port: ${properties.port}, context path: ${properties.contextPath}")
        return Forge.create(virtualThreadConfig)
    }
    
    @Bean
    fun forgeStarter(forge: Forge): ForgeStarter {
        return ForgeStarter(forge, properties)
    }
    
    @Bean
    fun forgeControllerProcessor(forge: Forge, applicationContext: org.springframework.context.ApplicationContext): ForgeControllerProcessor {
        return ForgeControllerProcessor(forge, applicationContext)
    }
}