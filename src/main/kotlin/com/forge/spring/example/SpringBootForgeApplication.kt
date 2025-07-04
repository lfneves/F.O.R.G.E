package com.forge.spring.example

import com.forge.spring.ForgeProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(ForgeProperties::class)
class SpringBootForgeApplication

fun main(args: Array<String>) {
    runApplication<SpringBootForgeApplication>(*args)
}