# F.O.R.G.E Configuration Guide

## Table of Contents
- [Environment Profiles](#environment-profiles)
- [Environment Variables](#environment-variables)
- [Configuration Properties](#configuration-properties)
- [Docker Configuration](#docker-configuration)
- [Testing Configuration](#testing-configuration)
- [Production Deployment](#production-deployment)

## Environment Profiles

F.O.R.G.E supports multiple environment profiles for different deployment scenarios:

### Development (`dev`)
```bash
# Activate development profile
export SPRING_PROFILES_ACTIVE=dev
# OR
java -jar forge-1.0.0.jar --spring.profiles.active=dev
```

**Features:**
- Enhanced logging and debugging
- Colored console output
- Development-friendly settings
- Port: 8081 (to avoid conflicts)
- Fast shutdown for quick restarts

### Staging (`staging`)
```bash
export SPRING_PROFILES_ACTIVE=staging
```

**Features:**
- Production-like configuration
- Enhanced monitoring and metrics
- Balanced logging
- All management endpoints enabled
- Suitable for pre-production testing

### Production (`prod`)
```bash
export SPRING_PROFILES_ACTIVE=prod
```

**Features:**
- Optimized for performance
- Minimal logging
- Security-focused
- Graceful shutdown
- Log rotation and archiving

### Test (`test`)
```bash
# Automatically used during testing
./gradlew test
```

**Features:**
- Fast startup and shutdown
- Minimal resource usage
- Random port allocation
- Disabled metrics and monitoring

## Environment Variables

### Quick Setup
1. Copy the environment template:
```bash
cp .env.example .env
```

2. Edit `.env` with your settings:
```bash
nano .env
```

### Core Configuration Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `FORGE_PORT` | `8080` | Server port |
| `FORGE_CONTEXT_PATH` | `/` | Application context path |
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring profile |

### Virtual Threads Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `FORGE_VT_ENABLED` | `true` | Enable virtual threads (requires JDK 21+) |
| `FORGE_VT_PREFIX` | `vt-forge` | Thread name prefix |
| `FORGE_VT_MAX_TASKS` | `-1` | Max concurrent tasks (-1 = unlimited) |
| `FORGE_VT_METRICS` | `true` | Enable virtual thread metrics |
| `FORGE_VT_SHUTDOWN_TIMEOUT` | `5000` | Shutdown timeout in milliseconds |

### Logging Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `LOG_LEVEL_ROOT` | `INFO` | Root logging level |
| `LOG_LEVEL_FORGE` | `INFO` | F.O.R.G.E framework logging level |
| `LOG_LEVEL_SPRING` | `WARN` | Spring framework logging level |
| `LOG_FILE` | `logs/forge.log` | Log file path |

### Management & Monitoring

| Variable | Default | Description |
|----------|---------|-------------|
| `MANAGEMENT_ENDPOINTS_ENABLED` | `true` | Enable management endpoints |
| `MANAGEMENT_HEALTH_ENABLED` | `true` | Enable health endpoint |
| `MANAGEMENT_METRICS_ENABLED` | `true` | Enable metrics endpoint |
| `MANAGEMENT_PROMETHEUS_ENABLED` | `false` | Enable Prometheus metrics |

## Configuration Properties

### Basic Application Configuration
```yaml
forge:
  port: 8080
  context-path: "/"
  
  virtual-threads:
    enabled: true
    thread-name-prefix: "vt-forge"
    max-concurrent-tasks: -1
    enable-metrics: true
    shutdown-timeout-ms: 5000
```

### Spring Boot Integration
```yaml
spring:
  application:
    name: my-forge-app
  profiles:
    active: dev
  main:
    web-application-type: none  # Required for F.O.R.G.E
```

### Advanced Configuration
```yaml
# Logging
logging:
  level:
    com.forge: INFO
    org.springframework: WARN
  file:
    name: logs/forge.log
    max-size: 100MB
    max-history: 30

# Management endpoints
management:
  endpoints:
    web:
      exposure:
        include: "health,info,metrics"
  health:
    show-details: always
```

## Docker Configuration

### Quick Start with Docker
```bash
# Build and run
docker-compose up -d

# With monitoring stack
docker-compose --profile monitoring up -d

# Custom environment
FORGE_PORT=9090 SPRING_PROFILES_ACTIVE=prod docker-compose up -d
```

### Docker Environment Variables
```yaml
# docker-compose.yml
environment:
  - FORGE_PORT=8080
  - FORGE_VT_ENABLED=true
  - SPRING_PROFILES_ACTIVE=staging
  - LOG_LEVEL_FORGE=INFO
```

### Custom Docker Build
```bash
# Build custom image
docker build -t my-forge-app .

# Run with custom settings
docker run -d \
  -p 8080:8080 \
  -e FORGE_PORT=8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -v $(pwd)/logs:/app/logs \
  my-forge-app
```

## Testing Configuration

### Unit Tests
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.forge.core.ForgeTest"

# Run with custom configuration
FORGE_TEST_PORT=8888 ./gradlew test
```

### Integration Tests
```bash
# Run integration tests
./gradlew integrationTest

# With specific profile
SPRING_PROFILES_ACTIVE=test ./gradlew test
```

### Test Environment Variables
```bash
# .env.test
FORGE_TEST_PORT=0
LOG_LEVEL_ROOT=WARN
LOG_LEVEL_FORGE=DEBUG
FORGE_VT_ENABLED=true
FORGE_VT_MAX_TASKS=50
```

## Production Deployment

### Systemd Service
```ini
# /etc/systemd/system/forge.service
[Unit]
Description=F.O.R.G.E Framework Application
After=network.target

[Service]
Type=simple
User=forge
WorkingDirectory=/opt/forge
Environment=SPRING_PROFILES_ACTIVE=prod
Environment=FORGE_PORT=8080
Environment=LOG_FILE=/var/log/forge/forge.log
ExecStart=/usr/bin/java -jar /opt/forge/forge-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### Production Checklist

#### ✅ Pre-deployment
- [ ] Set `SPRING_PROFILES_ACTIVE=prod`
- [ ] Configure proper logging directory
- [ ] Set up log rotation
- [ ] Configure monitoring endpoints
- [ ] Set resource limits
- [ ] Enable security settings

#### ✅ Environment Variables
```bash
export SPRING_PROFILES_ACTIVE=prod
export FORGE_PORT=8080
export LOG_FILE=/var/log/forge/forge-prod.log
export FORGE_VT_MAX_TASKS=10000
export FORGE_VT_METRICS=false
export MANAGEMENT_SECURITY_ENABLED=true
```

#### ✅ JVM Options
```bash
export JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication"
```

#### ✅ Security
- Disable unnecessary management endpoints
- Enable endpoint security
- Configure proper file permissions
- Use non-root user
- Enable firewall rules

### Kubernetes Deployment
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: forge-config
data:
  SPRING_PROFILES_ACTIVE: "prod"
  FORGE_PORT: "8080"
  FORGE_VT_ENABLED: "true"
  LOG_LEVEL_FORGE: "INFO"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: forge-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: forge
  template:
    metadata:
      labels:
        app: forge
    spec:
      containers:
      - name: forge
        image: forge:1.0.0
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: forge-config
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 30
```

## Troubleshooting

### Common Issues

#### Virtual Threads Not Working
```bash
# Check JDK version
java -version  # Must be 21+

# Enable virtual thread debugging
export LOG_LEVEL_FORGE=DEBUG
export FORGE_VT_METRICS=true
```

#### Port Already in Use
```bash
# Use random port
export FORGE_PORT=0

# Or find available port
lsof -i :8080
```

#### Configuration Not Loading
```bash
# Check active profile
java -jar forge.jar --spring.profiles.active=dev --debug

# Verify configuration location
java -jar forge.jar --spring.config.location=classpath:/application.yml
```

### Performance Tuning

#### JVM Options for Virtual Threads
```bash
# Recommended JVM flags
export JAVA_OPTS="
  -Xms1g -Xmx2g
  -XX:+UseG1GC
  -XX:+UseStringDeduplication
  -XX:+OptimizeStringConcat
  -XX:MaxGCPauseMillis=100
"
```

#### Virtual Thread Configuration
```yaml
forge:
  virtual-threads:
    max-concurrent-tasks: 10000  # Adjust based on workload
    enable-metrics: false        # Disable in production for performance
    shutdown-timeout-ms: 30000   # Graceful shutdown time
```

For more advanced configuration and deployment scenarios, see the [Deployment Guide](DEPLOYMENT.md).