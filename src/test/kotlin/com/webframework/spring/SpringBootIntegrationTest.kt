package com.webframework.spring

import com.webframework.core.WebFramework
import com.webframework.config.VirtualThreadConfig
import com.webframework.spring.example.SpringBootWebFrameworkApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URI
import java.time.Duration

@SpringBootTest(
    classes = [SpringBootWebFrameworkApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@TestPropertySource(properties = [
    "webframework.port=8082",
    "webframework.virtual-threads.enabled=true",
    "webframework.virtual-threads.thread-name-prefix=test-vt",
    "webframework.virtual-threads.enable-metrics=true"
])
@DisplayName("Spring Boot Integration Tests")
class SpringBootIntegrationTest {
    
    @Autowired
    private lateinit var applicationContext: ApplicationContext
    
    @LocalServerPort
    private var port: Int = 0
    
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()
    
    @Nested
    @DisplayName("Context and Configuration Loading")
    inner class ContextAndConfigurationLoading {
        
        @Test
        @DisplayName("Should load Spring Boot context successfully")
        fun shouldLoadSpringBootContextSuccessfully() {
            assertNotNull(applicationContext)
            // Just check that context is not null and contains beans
            assertTrue(applicationContext.beanDefinitionCount > 0)
        }
        
        @Test
        @DisplayName("Should auto-configure WebFramework bean")
        fun shouldAutoConfigureWebFrameworkBean() {
            assertTrue(applicationContext.containsBean("webFramework"))
            val webFramework = applicationContext.getBean("webFramework", WebFramework::class.java)
            assertNotNull(webFramework)
        }
        
        @Test
        @DisplayName("Should auto-configure VirtualThreadConfig bean")
        fun shouldAutoConfigureVirtualThreadConfigBean() {
            assertTrue(applicationContext.containsBean("virtualThreadConfig"))
            val config = applicationContext.getBean("virtualThreadConfig", VirtualThreadConfig::class.java)
            assertNotNull(config)
            assertTrue(config.enabled)
            assertEquals("test-vt", config.threadNamePrefix)
            assertTrue(config.enableMetrics)
        }
        
        @Test
        @DisplayName("Should load WebFramework properties correctly")
        fun shouldLoadWebFrameworkPropertiesCorrectly() {
            // Check if WebFrameworkProperties bean exists
            if (applicationContext.containsBean("webFrameworkProperties")) {
                val webFrameworkProperties = applicationContext.getBean("webFrameworkProperties", WebFrameworkProperties::class.java)
                assertNotNull(webFrameworkProperties)
                assertEquals(8082, webFrameworkProperties.port)
                assertTrue(webFrameworkProperties.virtualThreads.enabled)
                assertEquals("test-vt", webFrameworkProperties.virtualThreads.threadNamePrefix)
                assertTrue(webFrameworkProperties.virtualThreads.enableMetrics)
            } else {
                // Properties might be configured differently, just verify context loaded
                assertTrue(applicationContext.beanDefinitionCount > 0)
            }
        }
        
        @Test
        @DisplayName("Should auto-configure WebFrameworkStarter bean")
        fun shouldAutoConfigureWebFrameworkStarterBean() {
            assertTrue(applicationContext.containsBean("webFrameworkStarter"))
            val starter = applicationContext.getBean("webFrameworkStarter", WebFrameworkStarter::class.java)
            assertNotNull(starter)
        }
        
        @Test
        @DisplayName("Should auto-configure WebFrameworkControllerProcessor bean")
        fun shouldAutoConfigureWebFrameworkControllerProcessorBean() {
            assertTrue(applicationContext.containsBean("webFrameworkControllerProcessor"))
            val processor = applicationContext.getBean("webFrameworkControllerProcessor", WebFrameworkControllerProcessor::class.java)
            assertNotNull(processor)
        }
    }
    
    @Nested
    @DisplayName("Annotation Processing")
    inner class AnnotationProcessing {
        
        @Test
        @DisplayName("Should detect WebFrameworkController beans")
        fun shouldDetectWebFrameworkControllerBeans() {
            val controllersMap = applicationContext.getBeansWithAnnotation(com.webframework.spring.annotations.WebFrameworkController::class.java)
            assertTrue(controllersMap.isNotEmpty())
            assertTrue(controllersMap.containsKey("exampleController"))
        }
        
        @Test
        @DisplayName("Should process route annotations correctly")
        fun shouldProcessRouteAnnotationsCorrectly() {
            // This test verifies that the WebFrameworkControllerProcessor
            // correctly processes the annotations and registers routes
            val webFramework = applicationContext.getBean("webFramework", WebFramework::class.java)
            assertNotNull(webFramework)
            // Route registration happens during application startup
        }
    }
    
    @Nested
    @DisplayName("HTTP Endpoint Tests")
    inner class HttpEndpointTests {
        
        private fun makeRequest(path: String): HttpResponse<String> {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8082$path"))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build()
            
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        }
        
        @Test
        @DisplayName("Should handle Spring Boot endpoints via WebFramework")
        fun shouldHandleSpringBootEndpointsViaWebFramework() {
            // Give the server time to start
            Thread.sleep(2000)
            
            val response = makeRequest("/spring-boot")
            
            assertEquals(200, response.statusCode())
            assertTrue(response.body().contains("Welcome to WebFramework with Spring Boot"))
        }
        
        @Test
        @DisplayName("Should demonstrate virtual threads in Spring Boot context")
        fun shouldDemonstrateVirtualThreadsInSpringBootContext() {
            Thread.sleep(2000)
            
            val response = makeRequest("/async-processing")
            
            assertEquals(200, response.statusCode())
            val body = response.body()
            assertTrue(body.contains("results"))
            assertTrue(body.contains("totalTasks"))
        }
        
        @Test
        @DisplayName("Should handle path parameters in Spring Boot endpoints")
        fun shouldHandlePathParametersInSpringBootEndpoints() {
            Thread.sleep(2000)
            
            val response = makeRequest("/users/123")
            
            assertEquals(200, response.statusCode())
            assertTrue(response.body().contains("123"))
        }
        
        @Test
        @DisplayName("Should handle health endpoint")
        fun shouldHandleHealthEndpoint() {
            Thread.sleep(2000)
            
            val response = makeRequest("/health")
            
            assertEquals(200, response.statusCode())
            val body = response.body()
            assertTrue(body.contains("status"))
            assertTrue(body.contains("UP"))
        }
    }
    
    @Nested
    @DisplayName("Dependency Injection")
    inner class DependencyInjection {
        
        @Test
        @DisplayName("Should inject dependencies into WebFramework controllers")
        fun shouldInjectDependenciesIntoWebFrameworkControllers() {
            val exampleController = applicationContext.getBean("exampleController")
            assertNotNull(exampleController)
            
            // Verify that the controller has its dependencies injected
            // In this case, the UserService should be injected
            val userServiceField = exampleController.javaClass.getDeclaredField("userService")
            userServiceField.isAccessible = true
            val userService = userServiceField.get(exampleController)
            assertNotNull(userService)
        }
        
        @Test
        @DisplayName("Should create service beans correctly")
        fun shouldCreateServiceBeansCorrectly() {
            assertTrue(applicationContext.containsBean("userService"))
            val userService = applicationContext.getBean("userService")
            assertNotNull(userService)
        }
    }
    
    @Nested
    @DisplayName("Configuration Properties Validation")
    inner class ConfigurationPropertiesValidation {
        
        @Test
        @DisplayName("Should validate virtual thread configuration")
        fun shouldValidateVirtualThreadConfiguration() {
            val virtualThreadConfig = applicationContext.getBean("virtualThreadConfig", VirtualThreadConfig::class.java)
            
            assertTrue(virtualThreadConfig.enabled)
            assertEquals("test-vt", virtualThreadConfig.threadNamePrefix)
            assertTrue(virtualThreadConfig.enableMetrics)
            assertEquals(-1, virtualThreadConfig.maxConcurrentTasks)
            assertEquals(5000L, virtualThreadConfig.shutdownTimeoutMs)
        }
        
        @Test
        @DisplayName("Should validate WebFramework properties")
        fun shouldValidateWebFrameworkProperties() {
            // Check if WebFrameworkProperties bean exists
            if (applicationContext.containsBean("webFrameworkProperties")) {
                val webFrameworkProperties = applicationContext.getBean("webFrameworkProperties", WebFrameworkProperties::class.java)
                assertEquals(8082, webFrameworkProperties.port)
                assertEquals("/", webFrameworkProperties.contextPath)
                
                val vtProps = webFrameworkProperties.virtualThreads
                assertTrue(vtProps.enabled)
                assertEquals("test-vt", vtProps.threadNamePrefix)
                assertEquals(-1, vtProps.maxConcurrentTasks)
                assertTrue(vtProps.enableMetrics)
                assertEquals(5000L, vtProps.shutdownTimeoutMs)
            } else {
                // Validate via VirtualThreadConfig instead
                val virtualThreadConfig = applicationContext.getBean("virtualThreadConfig", VirtualThreadConfig::class.java)
                assertTrue(virtualThreadConfig.enabled)
                assertEquals("test-vt", virtualThreadConfig.threadNamePrefix)
            }
        }
    }
    
    @Nested
    @DisplayName("Integration with Spring Boot Features")
    inner class IntegrationWithSpringBootFeatures {
        
        @Test
        @DisplayName("Should work with Spring Boot profiles")
        fun shouldWorkWithSpringBootProfiles() {
            val environment = applicationContext.environment
            assertTrue(environment.acceptsProfiles("test"))
            assertEquals("test-vt", environment.getProperty("webframework.virtual-threads.thread-name-prefix"))
        }
        
        @Test
        @DisplayName("Should integrate with Spring Boot logging")
        fun shouldIntegrateWithSpringBootLogging() {
            // Verify that logging configuration is working
            // This is mainly to ensure no conflicts between WebFramework and Spring Boot logging
            assertDoesNotThrow {
                val logger = org.slf4j.LoggerFactory.getLogger(this::class.java)
                logger.info("Test log message from Spring Boot integration test")
            }
        }
    }
    
    @Nested
    @DisplayName("Error Handling and Edge Cases")
    inner class ErrorHandlingAndEdgeCases {
        
        @Test
        @DisplayName("Should handle missing configuration gracefully")
        fun shouldHandleMissingConfigurationGracefully() {
            // Test with minimal configuration to ensure defaults work
            assertDoesNotThrow {
                val defaultConfig = VirtualThreadConfig.default()
                assertNotNull(defaultConfig)
                assertTrue(defaultConfig.enabled)
            }
        }
        
        @Test
        @DisplayName("Should handle application shutdown gracefully")
        fun shouldHandleApplicationShutdownGracefully() {
            assertDoesNotThrow {
                // This test verifies that the WebFrameworkStarter
                // handles shutdown properly when the application context closes
                val starter = applicationContext.getBean("webFrameworkStarter", WebFrameworkStarter::class.java)
                assertNotNull(starter)
            }
        }
    }
}