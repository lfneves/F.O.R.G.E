package com.webframework.core

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import com.fasterxml.jackson.databind.ObjectMapper

class Context(
    private val request: HttpServletRequest,
    private val response: HttpServletResponse,
    private val objectMapper: ObjectMapper
) {
    
    fun pathParam(key: String): String? {
        val pathParams = request.getAttribute("pathParams") as? Map<*, *>
        return pathParams?.get(key)?.toString()
    }
    
    fun queryParam(key: String): String? = request.getParameter(key)
    
    fun queryParams(key: String): Array<String>? = request.getParameterValues(key)
    
    fun header(name: String): String? = request.getHeader(name)
    
    fun headers(name: String): List<String> = request.getHeaders(name).toList()
    
    fun cookie(name: String): String? {
        return request.cookies?.find { it.name == name }?.value
    }
    
    fun body(): String = request.inputStream.bufferedReader().use { it.readText() }
    
    fun <T> bodyAsClass(clazz: Class<T>): T = objectMapper.readValue(body(), clazz)
    
    fun status(code: Int): Context {
        response.status = code
        return this
    }
    
    fun header(name: String, value: String): Context {
        response.setHeader(name, value)
        return this
    }
    
    fun contentType(type: String): Context {
        response.contentType = type
        return this
    }
    
    fun result(content: String): Context {
        try {
            val writer = response.writer
            if (writer != null) {
                writer.write(content)
            } else {
                // Fallback for test environments or when writer is not available
                response.outputStream?.write(content.toByteArray())
            }
        } catch (e: Exception) {
            // In test environments, this might fail, so we'll handle it gracefully
            response.outputStream?.write(content.toByteArray())
        }
        return this
    }
    
    fun json(obj: Any): Context {
        contentType("application/json")
        result(objectMapper.writeValueAsString(obj))
        return this
    }
    
    fun html(content: String): Context {
        contentType("text/html")
        result(content)
        return this
    }
    
    fun redirect(url: String): Context {
        response.sendRedirect(url)
        return this
    }
    
    fun req(): HttpServletRequest = request
    
    fun res(): HttpServletResponse = response
    
    fun setAttribute(name: String, value: Any): Context {
        request.setAttribute(name, value)
        return this
    }
    
    fun getAttribute(name: String): Any? = request.getAttribute(name)
}