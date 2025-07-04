import java.time.LocalDateTime

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("kapt") version "1.9.20"
    kotlin("plugin.spring") version "1.9.20"
    java
    `maven-publish`
    id("org.springframework.boot") version "3.2.1"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.owasp.dependencycheck") version "9.0.7"
    jacoco
}

group = "com.forge"
version = "1.0.0"
description = "F.O.R.G.E - Framework Optimized for Resilient, Global Execution. High-performance web framework built on JDK 21 Virtual Threads"

val isSnapshot = version.toString().endsWith("-SNAPSHOT")

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
    implementation("org.eclipse.jetty:jetty-server:11.0.18")
    implementation("org.eclipse.jetty:jetty-servlet:11.0.18")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    
    // Spring Boot dependencies
    implementation("org.springframework.boot:spring-boot-starter:3.2.1")
    implementation("org.springframework.boot:spring-boot-starter-web:3.2.1")
    implementation("org.springframework.boot:spring-boot-starter-actuator:3.2.1")
    implementation("org.springframework.boot:spring-boot-autoconfigure:3.2.1")
    implementation("org.springframework:spring-context:6.1.2")
    implementation("org.springframework:spring-beans:6.1.2")
    implementation("org.springframework:spring-core:6.1.2")
    
    // Configuration processor for IDE support
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:3.2.1")
    // TODO: kapt has issues with JDK 21, using annotationProcessor instead
    // kapt("org.springframework.boot:spring-boot-configuration-processor:3.2.1")
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.20")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.2.1")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

// Configure Spring Boot plugin
springBoot {
    mainClass.set("com.forge.spring.example.SpringBootForgeApplicationKt")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

// JaCoCo configuration
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(true)
    }
}

// OWASP Dependency Check configuration
dependencyCheck {
    format = "HTML"
    suppressionFile = "owasp-suppressions.xml"
    failBuildOnCVSS = 7.0f
    analyzers.assemblyEnabled = false
    analyzers.nodeEnabled = false
}

// Publishing configuration
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("FORGE")
                description.set("F.O.R.G.E - Framework Optimized for Resilient, Global Execution. High-performance web framework built on JDK 21 Virtual Threads")
                url.set("https://github.com/lfneves/forge")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("lfneves")
                        name.set("lfneves")
                        email.set("lfneves@github.com")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/lfneves/forge.git")
                    developerConnection.set("scm:git:ssh://github.com:lfneves/forge.git")
                    url.set("https://github.com/lfneves/forge")
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/lfneves/forge")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
}

// JAR configuration
tasks.jar {
    archiveBaseName.set("forge")
    archiveVersion.set(version.toString())
    
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to "FORGE",
                "Implementation-Version" to version,
                "Implementation-Vendor" to "FORGE Team",
                "Built-By" to System.getProperty("user.name"),
                "Built-Date" to LocalDateTime.now().toString(),
                "Built-JDK" to System.getProperty("java.version"),
                "Built-Gradle" to gradle.gradleVersion
            )
        )
    }
}

// Sources JAR
tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

// Javadoc JAR
tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

// Add sources and javadoc JARs to artifacts
artifacts {
    archives(tasks["sourcesJar"])
    archives(tasks["javadocJar"])
}

// Release tasks
tasks.register("release") {
    group = "release"
    description = "Creates a release build"
    dependsOn("clean", "build", "publishToMavenLocal")
    
    doLast {
        println("🔥 FORGE Release ${version} created successfully!")
        println("📦 JAR: build/libs/forge-${version}.jar")
        println("📚 Sources: build/libs/forge-${version}-sources.jar")
        println("📖 Javadoc: build/libs/forge-${version}-javadoc.jar")
    }
}

tasks.register("publishRelease") {
    group = "release"
    description = "Publishes release to configured repositories"
    dependsOn("release", "publish")
}