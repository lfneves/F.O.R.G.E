package com.webframework.spring.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringBootWebFrameworkApplication

fun main(args: Array<String>) {
    runApplication<SpringBootWebFrameworkApplication>(*args)
}