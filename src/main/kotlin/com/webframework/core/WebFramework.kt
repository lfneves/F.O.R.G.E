package com.webframework.core

import com.webframework.concurrent.VirtualThreadExecutor
import com.webframework.config.VirtualThreadConfig
import com.webframework.routing.Handler
import com.webframework.routing.Route
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.LoggerFactory

class WebFramework private constructor(
    private val virtualThreadConfig: VirtualThreadConfig = VirtualThreadConfig.default()
) {
    
    private val logger = LoggerFactory.getLogger(WebFramework::class.java)
    private val routes = mutableListOf<Route>()
    private val beforeHandlers = mutableListOf<Handler>()
    private val afterHandlers = mutableListOf<Handler>()
    private val exceptionHandlers = mutableMapOf<Class<out Exception>, (Exception, Context) -> Unit>()
    private val objectMapper = ObjectMapper().registerKotlinModule()
    private var server: Server? = null
    private val virtualThreadExecutor = if (virtualThreadConfig.enabled) {
        VirtualThreadExecutor.create(virtualThreadConfig.threadNamePrefix)
    } else null
    
    companion object {
        fun create(): WebFramework = WebFramework()
        
        fun create(config: VirtualThreadConfig): WebFramework = WebFramework(config)
    }
    
    fun get(path: String, handler: Handler): WebFramework {
        routes.add(Route("GET", path, handler))
        return this
    }
    
    fun post(path: String, handler: Handler): WebFramework {
        routes.add(Route("POST", path, handler))
        return this
    }
    
    fun put(path: String, handler: Handler): WebFramework {
        routes.add(Route("PUT", path, handler))
        return this
    }
    
    fun delete(path: String, handler: Handler): WebFramework {
        routes.add(Route("DELETE", path, handler))
        return this
    }
    
    fun patch(path: String, handler: Handler): WebFramework {
        routes.add(Route("PATCH", path, handler))
        return this
    }
    
    fun before(handler: Handler): WebFramework {
        beforeHandlers.add(handler)
        return this
    }
    
    fun after(handler: Handler): WebFramework {
        afterHandlers.add(handler)
        return this
    }
    
    fun exception(exceptionClass: Class<out Exception>, handler: (Exception, Context) -> Unit): WebFramework {
        exceptionHandlers[exceptionClass] = handler
        return this
    }
    
    fun start(port: Int = 8080): WebFramework {
        server = Server(port)
        
        val context = ServletContextHandler(ServletContextHandler.SESSIONS)
        context.contextPath = "/"
        server!!.handler = context
        
        val servlet = object : HttpServlet() {
            override fun service(request: HttpServletRequest, response: HttpServletResponse) {
                handleRequest(request, response)
            }
        }
        
        context.addServlet(ServletHolder(servlet), "/*")
        
        try {
            server!!.start()
            logger.info("WebFramework started on port $port")
            server!!.join()
        } catch (e: Exception) {
            logger.error("Failed to start server", e)
        }
        
        return this
    }
    
    fun stop() {
        server?.stop()
        virtualThreadExecutor?.shutdown()
        logger.info("WebFramework stopped")
    }
    
    private fun handleRequest(request: HttpServletRequest, response: HttpServletResponse) {
        if (virtualThreadConfig.enabled && virtualThreadExecutor != null) {
            virtualThreadExecutor.executeAsync {
                processRequest(request, response)
            }
        } else {
            processRequest(request, response)
        }
    }
    
    private fun processRequest(request: HttpServletRequest, response: HttpServletResponse) {
        val ctx = Context(request, response, objectMapper)
        
        try {
            beforeHandlers.forEach { it.handle(ctx) }
            
            val matchingRoute = routes.find { route ->
                route.matches(request.method, request.pathInfo ?: request.servletPath)
            }
            
            if (matchingRoute != null) {
                val pathParams = matchingRoute.extractPathParams(request.pathInfo ?: request.servletPath)
                request.setAttribute("pathParams", pathParams)
                matchingRoute.handler.handle(ctx)
            } else {
                response.status = 404
                response.writer.write("Not Found")
            }
            
            afterHandlers.forEach { it.handle(ctx) }
            
        } catch (e: Exception) {
            handleException(e, ctx)
        }
    }
    
    private fun handleException(exception: Exception, ctx: Context) {
        val handler = exceptionHandlers[exception::class.java]
        if (handler != null) {
            handler(exception, ctx)
        } else {
            logger.error("Unhandled exception", exception)
            ctx.status(500).result("Internal Server Error")
        }
    }
}