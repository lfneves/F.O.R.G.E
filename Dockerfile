# F.O.R.G.E Framework Docker Image
# Multi-stage build for optimized production image

# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy gradle wrapper and build files
COPY gradle/ gradle/
COPY gradlew .
COPY gradle.properties .
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Copy source code
COPY src/ src/

# Make gradlew executable and build the application
RUN chmod +x gradlew
RUN ./gradlew clean build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="F.O.R.G.E Framework"
LABEL description="High-performance web framework with JDK 21 Virtual Threads"
LABEL version="1.0.0"

# Create non-root user
RUN addgroup -g 1000 forge && \
    adduser -D -s /bin/sh -u 1000 -G forge forge

# Create app directory and logs directory
WORKDIR /app
RUN mkdir -p /app/logs && \
    chown -R forge:forge /app

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/forge-*.jar /app/forge.jar
COPY --from=builder --chown=forge:forge /app/build/libs/forge-*.jar /app/forge.jar

# Install curl for health checks
RUN apk add --no-cache curl

# Switch to non-root user
USER forge

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options for production
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+OptimizeStringConcat"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar forge.jar"]