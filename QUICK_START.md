# F.O.R.G.E Quick Start Guide

## 🚀 Get Started in 5 Minutes

### Prerequisites
- **JDK 21+** (required for virtual threads)
- **Gradle 8.0+** or **Maven 3.8+**

### 1. Installation

#### Gradle (Kotlin DSL)
```kotlin
dependencies {
    implementation("com.forge:forge:1.0.0")
}
```

#### Maven
```xml
<dependency>
    <groupId>com.forge</groupId>
    <artifactId>forge</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Basic Application

```kotlin
import com.forge.core.Forge

fun main() {
    val app = Forge.create()
    
    app.get("/") { ctx ->
        ctx.json(mapOf(
            "message" to "Hello F.O.R.G.E!",
            "framework" to "F.O.R.G.E v1.0.0",
            "threads" to "Virtual Threads (JDK 21)"
        ))
    }
    
    app.get("/health") { ctx ->
        ctx.json(mapOf(
            "status" to "UP",
            "timestamp" to System.currentTimeMillis(),
            "version" to "1.0.0"
        ))
    }
    
    println("🚀 Starting F.O.R.G.E server on http://localhost:8080")
    app.start(8080)
}
```

### 3. Test Your Application

```bash
# Health check
curl http://localhost:8080/health

# Basic endpoint
curl http://localhost:8080/

# Expected response:
# {"message":"Hello F.O.R.G.E!","framework":"F.O.R.G.E v1.0.0","threads":"Virtual Threads (JDK 21)"}
```

## 🔧 Environment Configuration

### 1. Quick Setup
```bash
# Copy environment template
cp .env.example .env

# Edit configuration
nano .env
```

### 2. Basic Configuration
```bash
# .env
FORGE_PORT=8080
SPRING_PROFILES_ACTIVE=dev
FORGE_VT_ENABLED=true
FORGE_VT_MAX_TASKS=1000
LOG_LEVEL_FORGE=INFO
```

### 3. Run with Environment
```bash
# Development mode
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun

# Production mode
SPRING_PROFILES_ACTIVE=prod java -jar forge-1.0.0.jar

# Custom port
FORGE_PORT=9090 ./gradlew bootRun
```

## 🐳 Docker Quick Start

### 1. Basic Docker
```bash
# Build and run
docker-compose up -d

# View logs
docker-compose logs -f forge-app

# Stop
docker-compose down
```

### 2. With Monitoring
```bash
# Start with Prometheus + Grafana
docker-compose --profile monitoring up -d

# Access services:
# - Application: http://localhost:8080
# - Prometheus: http://localhost:9090  
# - Grafana: http://localhost:3000 (admin/forge123)
```

### 3. Custom Configuration
```bash
# Custom environment
FORGE_PORT=9090 SPRING_PROFILES_ACTIVE=prod docker-compose up -d

# View running containers
docker ps
```

## 🌱 Spring Boot Integration

### 1. Spring Boot Application
```kotlin
@SpringBootApplication
@EnableConfigurationProperties(ForgeProperties::class)
class MyForgeApplication

fun main(args: Array<String>) {
    runApplication<MyForgeApplication>(*args)
}

@ForgeController
class UserController {
    
    @ForgeRoute(method = "GET", path = "/api/users")
    fun getUsers(): List<User> {
        return listOf(
            User("1", "John Doe", "john@example.com"),
            User("2", "Jane Smith", "jane@example.com")
        )
    }
    
    @ForgeRoute(method = "GET", path = "/api/users/:id")
    fun getUser(@PathParam("id") id: String): User {
        return User(id, "User $id", "user$id@example.com")
    }
}

data class User(val id: String, val name: String, val email: String)
```

### 2. Configuration
```yaml
# application.yml
forge:
  port: 8080
  virtual-threads:
    enabled: true
    max-concurrent-tasks: 5000

spring:
  application:
    name: my-forge-app
  main:
    web-application-type: none  # Required
```

### 3. Run Spring Boot App
```bash
./gradlew bootRun

# Test endpoints
curl http://localhost:8080/api/users
curl http://localhost:8080/api/users/1
```

## 🧪 Testing

### 1. Run Tests
```bash
# All tests
./gradlew test

# Specific tests
./gradlew test --tests "*VirtualThread*"
./gradlew test --tests "*Security*"

# With coverage
./gradlew test jacocoTestReport
```

### 2. Performance Test
```bash
# Run performance benchmarks
./gradlew test --tests "*PerformanceTest*"

# Load testing
./gradlew test --tests "*LoadTest*"
```

## 📊 Monitoring

### 1. Health Endpoints
```bash
# Application health
curl http://localhost:8080/actuator/health

# Application info
curl http://localhost:8080/actuator/info

# Metrics
curl http://localhost:8080/actuator/metrics
```

### 2. Virtual Thread Metrics
```bash
# Enable metrics in configuration
FORGE_VT_METRICS=true ./gradlew bootRun

# View thread metrics
curl http://localhost:8080/actuator/metrics/forge.virtual.threads
```

## 🚨 Troubleshooting

### Common Issues

#### 1. Virtual Threads Not Working
```bash
# Check JDK version (must be 21+)
java -version

# Enable debug logging
LOG_LEVEL_FORGE=DEBUG ./gradlew bootRun
```

#### 2. Port Already in Use
```bash
# Use random port
FORGE_PORT=0 ./gradlew bootRun

# Or find the process using the port
lsof -i :8080
```

#### 3. Spring Boot Issues
```bash
# Check active profile
./gradlew bootRun --args='--debug --spring.profiles.active=dev'

# Verify configuration
./gradlew bootRun --args='--spring.config.location=classpath:/application.yml'
```

### Performance Tips

#### 1. JVM Options
```bash
# Recommended for production
export JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication"
java $JAVA_OPTS -jar forge-1.0.0.jar
```

#### 2. Virtual Thread Configuration
```yaml
forge:
  virtual-threads:
    max-concurrent-tasks: 10000  # Adjust based on load
    enable-metrics: false        # Disable in production
    shutdown-timeout-ms: 30000   # Graceful shutdown
```

## 📚 Next Steps

1. **Read Documentation**: [README.md](README.md) and [Configuration Guide](docs/CONFIGURATION.md)
2. **Explore Examples**: Check `src/main/kotlin/com/forge/examples/`
3. **Join Community**: [GitHub Discussions](https://github.com/lfneves/F.O.R.G.E/discussions)
4. **Report Issues**: [GitHub Issues](https://github.com/lfneves/F.O.R.G.E/issues)

---

**Welcome to F.O.R.G.E - Where performance meets reliability!** 🚀