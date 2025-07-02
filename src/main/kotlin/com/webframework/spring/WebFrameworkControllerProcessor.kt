package com.webframework.spring

import com.webframework.core.Context
import com.webframework.routing.Handler
import com.webframework.core.WebFramework
import com.webframework.spring.annotations.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.javaMethod

@Component
class WebFrameworkControllerProcessor(
    @Autowired private val webFramework: WebFramework,
    @Autowired private val applicationContext: ApplicationContext
) : ApplicationListener<ContextRefreshedEvent> {
    
    private val logger = LoggerFactory.getLogger(WebFrameworkControllerProcessor::class.java)
    
    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        processControllers()
    }
    
    private fun processControllers() {
        val controllers = applicationContext.getBeansWithAnnotation(WebFrameworkController::class.java)
        
        controllers.forEach { (beanName, controller) ->
            logger.info("Processing WebFramework controller: $beanName")
            processController(controller)
        }
    }
    
    private fun processController(controller: Any) {
        val controllerClass = controller::class
        val functions = controllerClass.functions
        
        functions.forEach { function ->
            processFunction(controller, function)
        }
    }
    
    private fun processFunction(controller: Any, function: KFunction<*>) {
        val getMapping = function.findAnnotation<GetMapping>()
        val postMapping = function.findAnnotation<PostMapping>()
        val putMapping = function.findAnnotation<PutMapping>()
        val deleteMapping = function.findAnnotation<DeleteMapping>()
        val patchMapping = function.findAnnotation<PatchMapping>()
        
        when {
            getMapping != null -> {
                registerRoute("GET", getMapping.path, controller, function)
            }
            postMapping != null -> {
                registerRoute("POST", postMapping.path, controller, function)
            }
            putMapping != null -> {
                registerRoute("PUT", putMapping.path, controller, function)
            }
            deleteMapping != null -> {
                registerRoute("DELETE", deleteMapping.path, controller, function)
            }
            patchMapping != null -> {
                registerRoute("PATCH", patchMapping.path, controller, function)
            }
        }
    }
    
    private fun registerRoute(method: String, path: String, controller: Any, function: KFunction<*>) {
        logger.info("Registering route: $method $path -> ${controller::class.simpleName}.${function.name}")
        
        val handler = Handler { ctx: Context ->
            try {
                val result = function.call(controller, ctx)
                if (result != null && result !is Unit) {
                    ctx.json(result)
                }
            } catch (e: Exception) {
                logger.error("Error executing controller method", e)
                ctx.status(500).result("Internal Server Error")
            }
        }
        
        when (method) {
            "GET" -> webFramework.get(path, handler)
            "POST" -> webFramework.post(path, handler)
            "PUT" -> webFramework.put(path, handler)
            "DELETE" -> webFramework.delete(path, handler)
            "PATCH" -> webFramework.patch(path, handler)
        }
    }
}