package com.webframework.spring.example

import com.webframework.core.Context
import com.webframework.spring.annotations.*
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

// Helper function to check if thread is virtual using reflection
fun isVirtualThread(thread: Thread): Boolean {
    return try {
        thread::class.java.getMethod("isVirtual").invoke(thread) as Boolean
    } catch (e: Exception) {
        false
    }
}

@WebFrameworkController
class ExampleController(private val userService: UserService) {
    
    @GetMapping("/spring-boot")
    fun welcome(ctx: Context) {
        ctx.json(mapOf(
            "message" to "Welcome to WebFramework with Spring Boot!",
            "timestamp" to LocalDateTime.now(),
            "thread" to Thread.currentThread().toString(),
            "isVirtual" to isVirtualThread(Thread.currentThread())
        ))
    }
    
    @GetMapping("/users")
    fun getUsers(ctx: Context) {
        val users = userService.getAllUsers()
        ctx.json(mapOf(
            "users" to users,
            "count" to users.size,
            "timestamp" to LocalDateTime.now()
        ))
    }
    
    @GetMapping("/users/:id")
    fun getUser(ctx: Context) {
        val userId = ctx.pathParam("id")?.toLongOrNull()
        if (userId == null) {
            ctx.status(400).json(mapOf("error" to "Invalid user ID"))
            return
        }
        
        val user = userService.getUserById(userId)
        if (user != null) {
            ctx.json(user)
        } else {
            ctx.status(404).json(mapOf("error" to "User not found"))
        }
    }
    
    @PostMapping("/users")
    fun createUser(ctx: Context) {
        val requestBody = ctx.bodyAsClass(CreateUserRequest::class.java)
        val user = userService.createUser(requestBody.name, requestBody.email)
        ctx.status(201).json(user)
    }
    
    @GetMapping("/async-processing")
    fun asyncProcessing(ctx: Context) {
        val tasks = (1..5).map { taskId ->
            CompletableFuture.supplyAsync {
                val processingTime = Random.nextLong(100, 500)
                Thread.sleep(processingTime)
                mapOf(
                    "taskId" to taskId,
                    "processingTime" to processingTime,
                    "thread" to Thread.currentThread().toString(),
                    "isVirtual" to isVirtualThread(Thread.currentThread())
                )
            }
        }
        
        val results = CompletableFuture.allOf(*tasks.toTypedArray())
            .thenApply { tasks.map { it.get() } }
            .get()
        
        ctx.json(mapOf(
            "results" to results,
            "totalTasks" to tasks.size,
            "timestamp" to LocalDateTime.now()
        ))
    }
    
    @GetMapping("/health")
    fun health(ctx: Context) {
        ctx.json(mapOf(
            "status" to "UP",
            "timestamp" to LocalDateTime.now(),
            "framework" to "WebFramework with Spring Boot",
            "virtualThreads" to isVirtualThread(Thread.currentThread())
        ))
    }
}

data class CreateUserRequest(
    val name: String,
    val email: String
)

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Service
class UserService {
    private val users = mutableMapOf<Long, User>()
    private var nextId = 1L
    
    init {
        users[1] = User(1, "John Doe", "john@example.com")
        users[2] = User(2, "Jane Smith", "jane@example.com")
        nextId = 3
    }
    
    fun getAllUsers(): List<User> = users.values.toList()
    
    fun getUserById(id: Long): User? = users[id]
    
    fun createUser(name: String, email: String): User {
        val user = User(nextId++, name, email)
        users[user.id] = user
        return user
    }
}