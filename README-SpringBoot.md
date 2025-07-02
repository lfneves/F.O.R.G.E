# WebFramework Spring Boot Integration

This document describes how to use the WebFramework with Spring Boot integration.

## Features

- **Auto-configuration**: Automatic WebFramework setup with Spring Boot
- **Configuration Properties**: YAML/Properties based configuration
- **Annotation-based Controllers**: Use familiar Spring-style annotations
- **Dependency Injection**: Full Spring IoC container support
- **Health Indicators**: Built-in health checks for monitoring
- **Profile Support**: Environment-specific configurations

## Quick Start

### 1. Add Dependencies

The framework automatically includes Spring Boot dependencies. Just add your application dependencies.

### 2. Create a Spring Boot Application

```kotlin
@SpringBootApplication
class MyApplication

fun main(args: Array<String>) {
    runApplication<MyApplication>(*args)
}
```

### 3. Create Controllers

```kotlin
@WebFrameworkController
class ApiController(private val userService: UserService) {
    
    @GetMapping("/api/users")
    fun getUsers(ctx: Context) {
        val users = userService.getAllUsers()
        ctx.json(users)
    }
    
    @PostMapping("/api/users")
    fun createUser(ctx: Context) {
        val request = ctx.bodyAsClass(CreateUserRequest::class.java)
        val user = userService.createUser(request.name, request.email)
        ctx.status(201).json(user)
    }
}
```

### 4. Configure Application

**application.yml:**
```yaml
webframework:
  port: 8080
  context-path: "/"
  virtual-threads:
    enabled: true
    thread-name-prefix: "my-app-vt"
    enable-metrics: true
```

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `webframework.port` | 8080 | Server port |
| `webframework.context-path` | "/" | Application context path |
| `webframework.virtual-threads.enabled` | true | Enable virtual threads |
| `webframework.virtual-threads.thread-name-prefix` | "vt-webframework" | Thread name prefix |
| `webframework.virtual-threads.max-concurrent-tasks` | -1 | Max concurrent tasks (-1 = unlimited) |
| `webframework.virtual-threads.enable-metrics` | false | Enable thread metrics |
| `webframework.virtual-threads.shutdown-timeout-ms` | 5000 | Shutdown timeout |

## Annotations

### @WebFrameworkController
Marks a class as a WebFramework controller. Must be used with Spring's component scanning.

### HTTP Method Annotations
- `@GetMapping(path)` - Handle GET requests
- `@PostMapping(path)` - Handle POST requests  
- `@PutMapping(path)` - Handle PUT requests
- `@DeleteMapping(path)` - Handle DELETE requests
- `@PatchMapping(path)` - Handle PATCH requests

## Environment Profiles

The framework supports Spring Boot profiles:

- `application.yml` - Default configuration
- `application-dev.yml` - Development settings
- `application-prod.yml` - Production settings

## Example Usage

```kotlin
@WebFrameworkController
class ProductController(
    private val productService: ProductService,
    private val logger: Logger
) {
    
    @GetMapping("/products")
    fun getAllProducts(ctx: Context) {
        logger.info("Fetching all products on thread: ${Thread.currentThread().name}")
        val products = productService.findAll()
        ctx.json(mapOf(
            "products" to products,
            "thread" to Thread.currentThread().toString(),
            "isVirtual" to Thread.currentThread().isVirtual
        ))
    }
    
    @GetMapping("/products/:id")
    fun getProduct(ctx: Context) {
        val id = ctx.pathParam("id")?.toLongOrNull()
            ?: return ctx.status(400).json(mapOf("error" to "Invalid ID"))
        
        val product = productService.findById(id)
            ?: return ctx.status(404).json(mapOf("error" to "Product not found"))
        
        ctx.json(product)
    }
}
```

## Running the Application

```bash
# Development mode
./gradlew bootRun --args='--spring.profiles.active=dev'

# Production mode  
./gradlew bootRun --args='--spring.profiles.active=prod'
```

## Testing

The framework includes test utilities for Spring Boot integration testing:

```kotlin
@SpringBootTest
@ActiveProfiles("test")
class ApplicationTest {
    
    @Test
    fun `application starts successfully`() {
        // Test application startup
    }
}
```

## Benefits

1. **Virtual Threads**: Automatic JDK 21 virtual thread support for high concurrency
2. **Spring Integration**: Full Spring ecosystem compatibility
3. **Configuration Management**: Externalized configuration with profiles
4. **Dependency Injection**: Use Spring's powerful IoC container
5. **Monitoring**: Built-in health indicators and metrics
6. **Testing**: Comprehensive test support with Spring Boot Test