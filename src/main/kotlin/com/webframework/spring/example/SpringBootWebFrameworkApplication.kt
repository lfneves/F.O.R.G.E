package com.webframework.spring.example

import com.webframework.spring.WebFrameworkProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(WebFrameworkProperties::class)
class SpringBootWebFrameworkApplication

fun main(args: Array<String>) {
    runApplication<SpringBootWebFrameworkApplication>(*args)
}