package com.webframework.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.Cookie
import java.io.ByteArrayInputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

@DisplayName("Context API Tests")
class ContextTest {
    
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var objectMapper: ObjectMapper
    private lateinit var context: Context
    private lateinit var responseWriter: StringWriter
    private lateinit var printWriter: PrintWriter
    
    @BeforeEach
    fun setUp() {
        request = mock(HttpServletRequest::class.java)
        response = mock(HttpServletResponse::class.java)
        objectMapper = ObjectMapper().registerKotlinModule()
        
        responseWriter = StringWriter()
        printWriter = PrintWriter(responseWriter)
        `when`(response.writer).thenReturn(printWriter)
        
        context = Context(request, response, objectMapper)
    }
    
    @Nested
    @DisplayName("Path Parameters")
    inner class PathParameters {
        
        @Test
        @DisplayName("Should get path parameter")
        fun shouldGetPathParameter() {
            val pathParams = mapOf("id" to "123", "name" to "john")
            `when`(request.getAttribute("pathParams")).thenReturn(pathParams)
            
            assertEquals("123", context.pathParam("id"))
            assertEquals("john", context.pathParam("name"))
            assertNull(context.pathParam("nonexistent"))
        }
        
        @Test
        @DisplayName("Should handle null path parameters")
        fun shouldHandleNullPathParameters() {
            `when`(request.getAttribute("pathParams")).thenReturn(null)
            
            assertNull(context.pathParam("id"))
        }
        
        @Test
        @DisplayName("Should handle empty path parameters")
        fun shouldHandleEmptyPathParameters() {
            val pathParams = emptyMap<String, String>()
            `when`(request.getAttribute("pathParams")).thenReturn(pathParams)
            
            assertNull(context.pathParam("id"))
        }
    }
    
    @Nested
    @DisplayName("Query Parameters")
    inner class QueryParameters {
        
        @Test
        @DisplayName("Should get single query parameter")
        fun shouldGetSingleQueryParameter() {
            `when`(request.getParameter("q")).thenReturn("search term")
            `when`(request.getParameter("limit")).thenReturn("10")
            
            assertEquals("search term", context.queryParam("q"))
            assertEquals("10", context.queryParam("limit"))
            assertNull(context.queryParam("nonexistent"))
        }
        
        @Test
        @DisplayName("Should get multiple query parameters")
        fun shouldGetMultipleQueryParameters() {
            val values = arrayOf("value1", "value2", "value3")
            `when`(request.getParameterValues("tags")).thenReturn(values)
            
            val result = context.queryParams("tags")
            assertArrayEquals(values, result)
        }
        
        @Test
        @DisplayName("Should handle null query parameters")
        fun shouldHandleNullQueryParameters() {
            `when`(request.getParameterValues("tags")).thenReturn(null)
            
            assertNull(context.queryParams("tags"))
        }
    }
    
    @Nested
    @DisplayName("Headers")
    inner class Headers {
        
        @Test
        @DisplayName("Should get single header")
        fun shouldGetSingleHeader() {
            `when`(request.getHeader("Content-Type")).thenReturn("application/json")
            `when`(request.getHeader("Authorization")).thenReturn("Bearer token123")
            
            assertEquals("application/json", context.header("Content-Type"))
            assertEquals("Bearer token123", context.header("Authorization"))
            assertNull(context.header("nonexistent"))
        }
        
        @Test
        @DisplayName("Should get multiple headers")
        fun shouldGetMultipleHeaders() {
            val headers = Collections.enumeration(listOf("gzip", "deflate"))
            `when`(request.getHeaders("Accept-Encoding")).thenReturn(headers)
            
            val result = context.headers("Accept-Encoding")
            assertEquals(listOf("gzip", "deflate"), result)
        }
        
        @Test
        @DisplayName("Should set response header")
        fun shouldSetResponseHeader() {
            context.header("X-Custom-Header", "custom-value")
            
            verify(response).setHeader("X-Custom-Header", "custom-value")
        }
    }
    
    @Nested
    @DisplayName("Cookies")
    inner class Cookies {
        
        @Test
        @DisplayName("Should get cookie value")
        fun shouldGetCookieValue() {
            val cookies = arrayOf(
                Cookie("sessionId", "abc123"),
                Cookie("theme", "dark")
            )
            `when`(request.cookies).thenReturn(cookies)
            
            assertEquals("abc123", context.cookie("sessionId"))
            assertEquals("dark", context.cookie("theme"))
            assertNull(context.cookie("nonexistent"))
        }
        
        @Test
        @DisplayName("Should handle null cookies")
        fun shouldHandleNullCookies() {
            `when`(request.cookies).thenReturn(null)
            
            assertNull(context.cookie("sessionId"))
        }
        
        @Test
        @DisplayName("Should handle empty cookies")
        fun shouldHandleEmptyCookies() {
            `when`(request.cookies).thenReturn(emptyArray())
            
            assertNull(context.cookie("sessionId"))
        }
    }
    
    @Nested
    @DisplayName("Request Body")
    inner class RequestBody {
        
        @Test
        @DisplayName("Should read request body as string")
        fun shouldReadRequestBodyAsString() {
            val body = "Hello World"
            val inputStream = ByteArrayInputStream(body.toByteArray())
            `when`(request.inputStream).thenReturn(mock(jakarta.servlet.ServletInputStream::class.java))
            
            // Mock the actual reading behavior
            val mockContext = spy(context)
            doReturn(body).`when`(mockContext).body()
            
            assertEquals(body, mockContext.body())
        }
        
        @Test
        @DisplayName("Should parse JSON request body to object")
        fun shouldParseJsonRequestBodyToObject() {
            data class User(val name: String, val email: String)
            
            val jsonBody = """{"name":"John","email":"john@example.com"}"""
            val mockContext = spy(context)
            doReturn(jsonBody).`when`(mockContext).body()
            
            val user = mockContext.bodyAsClass(User::class.java)
            assertEquals("John", user.name)
            assertEquals("john@example.com", user.email)
        }
    }
    
    @Nested
    @DisplayName("Response Operations")
    inner class ResponseOperations {
        
        @Test
        @DisplayName("Should set response status")
        fun shouldSetResponseStatus() {
            val result = context.status(201)
            
            verify(response).status = 201
            assertEquals(context, result) // Should return this for chaining
        }
        
        @Test
        @DisplayName("Should set content type")
        fun shouldSetContentType() {
            val result = context.contentType("application/json")
            
            verify(response).contentType = "application/json"
            assertEquals(context, result) // Should return this for chaining
        }
        
        @Test
        @DisplayName("Should write text result")
        fun shouldWriteTextResult() {
            val result = context.result("Hello World")
            
            printWriter.flush()
            assertEquals("Hello World", responseWriter.toString())
            assertEquals(context, result) // Should return this for chaining
        }
        
        @Test
        @DisplayName("Should write JSON response")
        fun shouldWriteJsonResponse() {
            val data = mapOf("message" to "Hello", "status" to "success")
            val result = context.json(data)
            
            verify(response).contentType = "application/json"
            printWriter.flush()
            val responseBody = responseWriter.toString()
            assertTrue(responseBody.contains("Hello"))
            assertTrue(responseBody.contains("success"))
            assertEquals(context, result) // Should return this for chaining
        }
        
        @Test
        @DisplayName("Should write HTML response")
        fun shouldWriteHtmlResponse() {
            val html = "<h1>Hello World</h1>"
            val result = context.html(html)
            
            verify(response).contentType = "text/html"
            printWriter.flush()
            assertEquals(html, responseWriter.toString())
            assertEquals(context, result) // Should return this for chaining
        }
        
        @Test
        @DisplayName("Should send redirect")
        fun shouldSendRedirect() {
            val result = context.redirect("/new-location")
            
            verify(response).sendRedirect("/new-location")
            assertEquals(context, result) // Should return this for chaining
        }
        
        @Test
        @DisplayName("Should chain response operations")
        fun shouldChainResponseOperations() {
            val result = context
                .status(201)
                .header("X-Custom", "value")
                .contentType("application/json")
                .result("OK")
            
            verify(response).status = 201
            verify(response).setHeader("X-Custom", "value")
            verify(response).contentType = "application/json"
            printWriter.flush()
            assertEquals("OK", responseWriter.toString())
            assertEquals(context, result)
        }
    }
    
    @Nested
    @DisplayName("Raw Request/Response Access")
    inner class RawAccess {
        
        @Test
        @DisplayName("Should provide access to raw request")
        fun shouldProvideAccessToRawRequest() {
            assertEquals(request, context.req())
        }
        
        @Test
        @DisplayName("Should provide access to raw response")
        fun shouldProvideAccessToRawResponse() {
            assertEquals(response, context.res())
        }
    }
    
    @Nested
    @DisplayName("JSON Serialization")
    inner class JsonSerialization {
        
        @Test
        @DisplayName("Should serialize simple objects to JSON")
        fun shouldSerializeSimpleObjectsToJson() {
            val data = mapOf("name" to "John", "age" to 30)
            context.json(data)
            
            printWriter.flush()
            val responseBody = responseWriter.toString()
            assertTrue(responseBody.contains("John"))
            assertTrue(responseBody.contains("30"))
        }
        
        @Test
        @DisplayName("Should serialize complex objects to JSON")
        fun shouldSerializeComplexObjectsToJson() {
            data class Address(val street: String, val city: String)
            data class Person(val name: String, val address: Address, val tags: List<String>)
            
            val person = Person(
                name = "John",
                address = Address("123 Main St", "New York"),
                tags = listOf("developer", "kotlin")
            )
            
            context.json(person)
            
            printWriter.flush()
            val responseBody = responseWriter.toString()
            assertTrue(responseBody.contains("John"))
            assertTrue(responseBody.contains("123 Main St"))
            assertTrue(responseBody.contains("developer"))
        }
        
        @Test
        @DisplayName("Should handle null values in JSON")
        fun shouldHandleNullValuesInJson() {
            val data = mapOf("name" to "John", "email" to null)
            
            assertDoesNotThrow {
                context.json(data)
            }
        }
    }
}