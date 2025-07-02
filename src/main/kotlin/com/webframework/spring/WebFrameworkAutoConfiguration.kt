package com.webframework.spring

import com.webframework.core.WebFramework
import com.webframework.config.VirtualThreadConfig
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@AutoConfiguration
@ConditionalOnClass(WebFramework::class)
@EnableConfigurationProperties(WebFrameworkProperties::class)
@ComponentScan(basePackages = ["com.webframework.spring"])
class WebFrameworkAutoConfiguration(
    private val properties: WebFrameworkProperties
) {
    
    private val logger = LoggerFactory.getLogger(WebFrameworkAutoConfiguration::class.java)
    
    @Bean
    @ConditionalOnMissingBean
    fun virtualThreadConfig(): VirtualThreadConfig {
        logger.info("Creating VirtualThreadConfig with properties: ${properties.virtualThreads}")
        return properties.toVirtualThreadConfig()
    }
    
    @Bean
    @ConditionalOnMissingBean
    fun webFramework(virtualThreadConfig: VirtualThreadConfig): WebFramework {
        logger.info("Creating WebFramework with port: ${properties.port}, context path: ${properties.contextPath}")
        return WebFramework.create(virtualThreadConfig)
    }
    
    @Bean
    fun webFrameworkStarter(webFramework: WebFramework): WebFrameworkStarter {
        return WebFrameworkStarter(webFramework, properties)
    }
}