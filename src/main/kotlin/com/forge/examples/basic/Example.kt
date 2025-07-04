package com.forge.examples.basic

import com.forge.core.WebFramework

data class User(val id: Int, val name: String, val email: String)

fun main() {
    val users = mutableListOf(
        User(1, "John Doe", "john@example.com"),
        User(2, "Jane Smith", "jane@example.com")
    )
    
    WebFramework.create()
        .before { ctx ->
            println("Request: ${ctx.req().method} ${ctx.req().requestURI}")
        }
        .get("/") { ctx ->
            ctx.result("Hello, WebFramework!")
        }
        .get("/users") { ctx ->
            ctx.json(users)
        }
        .get("/users/:id") { ctx ->
            val id = ctx.pathParam("id")?.toIntOrNull()
            val user = users.find { it.id == id }
            if (user != null) {
                ctx.json(user)
            } else {
                ctx.status(404).result("User not found")
            }
        }
        .post("/users") { ctx ->
            val user = ctx.bodyAsClass(User::class.java)
            users.add(user)
            ctx.status(201).json(user)
        }
        .exception(NumberFormatException::class.java) { _, ctx ->
            ctx.status(400).result("Invalid number format")
        }
        .after { ctx ->
            println("Response status: ${ctx.res().status}")
        }
        .start(8080)
}