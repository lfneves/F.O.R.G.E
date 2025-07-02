package com.webframework.examples.basic

import com.webframework.core.WebFramework
import com.webframework.config.VirtualThreadConfig

fun main() {
    val framework = WebFramework.create()
    
    framework.get("/") { ctx ->
        ctx.json(mapOf("message" to "Hello, WebFramework!"))
    }
    
    framework.get("/hello/:name") { ctx ->
        val name = ctx.pathParam("name") ?: "World"
        ctx.json(mapOf("greeting" to "Hello, $name!"))
    }
    
    framework.get("/echo") { ctx ->
        val message = ctx.queryParam("message") ?: "No message provided"
        ctx.json(mapOf("echo" to message))
    }
    
    println("Starting basic WebFramework example on port 8080...")
    println("Try: http://localhost:8080/")
    println("Try: http://localhost:8080/hello/John")
    println("Try: http://localhost:8080/echo?message=Hello%20World")
    
    framework.start(8080)
}