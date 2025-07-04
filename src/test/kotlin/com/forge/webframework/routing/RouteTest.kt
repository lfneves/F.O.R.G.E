package com.forge.routing

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*

@DisplayName("Route Matching Tests")
class RouteTest {
    
    @Nested
    @DisplayName("Static Routes")
    inner class StaticRoutes {
        
        @Test
        @DisplayName("Should match exact static routes")
        fun shouldMatchExactStaticRoutes() {
            val handler = Handler { }
            val route = Route("GET", "/users", handler)
            
            assertTrue(route.matches("GET", "/users"))
            assertFalse(route.matches("GET", "/user"))
            assertFalse(route.matches("GET", "/users/"))
            assertFalse(route.matches("POST", "/users"))
        }
        
        @Test
        @DisplayName("Should be case insensitive for HTTP methods")
        fun shouldBeCaseInsensitiveForHttpMethods() {
            val handler = Handler { }
            val route = Route("GET", "/test", handler)
            
            assertTrue(route.matches("GET", "/test"))
            assertTrue(route.matches("get", "/test"))
            assertTrue(route.matches("Get", "/test"))
            assertTrue(route.matches("gEt", "/test"))
        }
        
        @Test
        @DisplayName("Should match nested static routes")
        fun shouldMatchNestedStaticRoutes() {
            val handler = Handler { }
            val route = Route("GET", "/api/v1/users", handler)
            
            assertTrue(route.matches("GET", "/api/v1/users"))
            assertFalse(route.matches("GET", "/api/v1/user"))
            assertFalse(route.matches("GET", "/api/v2/users"))
        }
    }
    
    @Nested
    @DisplayName("Dynamic Routes with Parameters")
    inner class DynamicRoutes {
        
        @Test
        @DisplayName("Should match single parameter routes")
        fun shouldMatchSingleParameterRoutes() {
            val handler = Handler { }
            val route = Route("GET", "/users/:id", handler)
            
            assertTrue(route.matches("GET", "/users/123"))
            assertTrue(route.matches("GET", "/users/abc"))
            assertTrue(route.matches("GET", "/users/user-123"))
            assertFalse(route.matches("GET", "/users"))
            assertFalse(route.matches("GET", "/users/"))
            assertFalse(route.matches("GET", "/users/123/profile"))
        }
        
        @Test
        @DisplayName("Should match multiple parameter routes")
        fun shouldMatchMultipleParameterRoutes() {
            val handler = Handler { }
            val route = Route("GET", "/users/:userId/posts/:postId", handler)
            
            assertTrue(route.matches("GET", "/users/123/posts/456"))
            assertTrue(route.matches("GET", "/users/john/posts/hello-world"))
            assertFalse(route.matches("GET", "/users/123/posts"))
            assertFalse(route.matches("GET", "/users/123/comments/456"))
        }
        
        @Test
        @DisplayName("Should match mixed static and dynamic routes")
        fun shouldMatchMixedStaticAndDynamicRoutes() {
            val handler = Handler { }
            val route = Route("GET", "/api/v1/users/:id/profile", handler)
            
            assertTrue(route.matches("GET", "/api/v1/users/123/profile"))
            assertTrue(route.matches("GET", "/api/v1/users/john/profile"))
            assertFalse(route.matches("GET", "/api/v1/users/123/settings"))
            assertFalse(route.matches("GET", "/api/v2/users/123/profile"))
        }
    }
    
    @Nested
    @DisplayName("Parameter Extraction")
    inner class ParameterExtraction {
        
        @Test
        @DisplayName("Should extract single parameter")
        fun shouldExtractSingleParameter() {
            val handler = Handler { }
            val route = Route("GET", "/users/:id", handler)
            
            val params = route.extractPathParams("/users/123")
            assertEquals(mapOf("id" to "123"), params)
        }
        
        @Test
        @DisplayName("Should extract multiple parameters")
        fun shouldExtractMultipleParameters() {
            val handler = Handler { }
            val route = Route("GET", "/users/:userId/posts/:postId", handler)
            
            val params = route.extractPathParams("/users/john/posts/hello-world")
            assertEquals(mapOf(
                "userId" to "john",
                "postId" to "hello-world"
            ), params)
        }
        
        @Test
        @DisplayName("Should extract parameters with special characters")
        fun shouldExtractParametersWithSpecialCharacters() {
            val handler = Handler { }
            val route = Route("GET", "/files/:filename", handler)
            
            val params = route.extractPathParams("/files/document-v1.2.pdf")
            assertEquals(mapOf("filename" to "document-v1.2.pdf"), params)
        }
        
        @Test
        @DisplayName("Should return empty map for non-matching paths")
        fun shouldReturnEmptyMapForNonMatchingPaths() {
            val handler = Handler { }
            val route = Route("GET", "/users/:id", handler)
            
            val params = route.extractPathParams("/products/123")
            assertEquals(emptyMap<String, String>(), params)
        }
        
        @Test
        @DisplayName("Should return empty map for static routes")
        fun shouldReturnEmptyMapForStaticRoutes() {
            val handler = Handler { }
            val route = Route("GET", "/users", handler)
            
            val params = route.extractPathParams("/users")
            assertEquals(emptyMap<String, String>(), params)
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {
        
        @Test
        @DisplayName("Should handle empty paths")
        fun shouldHandleEmptyPaths() {
            val handler = Handler { }
            val route = Route("GET", "/", handler)
            
            assertTrue(route.matches("GET", "/"))
            assertFalse(route.matches("GET", ""))
        }
        
        @Test
        @DisplayName("Should handle routes with trailing slashes")
        fun shouldHandleRoutesWithTrailingSlashes() {
            val handler = Handler { }
            val route = Route("GET", "/users/", handler)
            
            assertTrue(route.matches("GET", "/users/"))
            assertFalse(route.matches("GET", "/users"))
        }
        
        @Test
        @DisplayName("Should handle complex parameter patterns")
        fun shouldHandleComplexParameterPatterns() {
            val handler = Handler { }
            val route = Route("GET", "/api/:version/users/:userId/orders/:orderId/items/:itemId", handler)
            
            assertTrue(route.matches("GET", "/api/v1/users/123/orders/456/items/789"))
            
            val params = route.extractPathParams("/api/v2/users/john/orders/order-123/items/item-456")
            assertEquals(mapOf(
                "version" to "v2",
                "userId" to "john",
                "orderId" to "order-123",
                "itemId" to "item-456"
            ), params)
        }
        
        @Test
        @DisplayName("Should handle numeric and alphanumeric parameters")
        fun shouldHandleNumericAndAlphanumericParameters() {
            val handler = Handler { }
            val route = Route("GET", "/items/:id", handler)
            
            // Numeric IDs
            assertTrue(route.matches("GET", "/items/123"))
            assertEquals(mapOf("id" to "123"), route.extractPathParams("/items/123"))
            
            // Alphanumeric IDs
            assertTrue(route.matches("GET", "/items/abc123"))
            assertEquals(mapOf("id" to "abc123"), route.extractPathParams("/items/abc123"))
            
            // UUIDs
            val uuid = "550e8400-e29b-41d4-a716-446655440000"
            assertTrue(route.matches("GET", "/items/$uuid"))
            assertEquals(mapOf("id" to uuid), route.extractPathParams("/items/$uuid"))
        }
        
        @Test
        @DisplayName("Should not match parameters containing slashes")
        fun shouldNotMatchParametersContainingSlashes() {
            val handler = Handler { }
            val route = Route("GET", "/users/:id", handler)
            
            // Parameters should not span multiple path segments
            assertFalse(route.matches("GET", "/users/123/profile"))
            assertFalse(route.matches("GET", "/users/john/settings"))
        }
    }
    
    @Nested
    @DisplayName("HTTP Methods")
    inner class HttpMethods {
        
        @Test
        @DisplayName("Should distinguish between different HTTP methods")
        fun shouldDistinguishBetweenDifferentHttpMethods() {
            val handler = Handler { }
            val getRoute = Route("GET", "/users", handler)
            val postRoute = Route("POST", "/users", handler)
            val putRoute = Route("PUT", "/users/:id", handler)
            val deleteRoute = Route("DELETE", "/users/:id", handler)
            val patchRoute = Route("PATCH", "/users/:id", handler)
            
            // Same path, different methods
            assertTrue(getRoute.matches("GET", "/users"))
            assertTrue(postRoute.matches("POST", "/users"))
            assertFalse(getRoute.matches("POST", "/users"))
            assertFalse(postRoute.matches("GET", "/users"))
            
            // With parameters
            assertTrue(putRoute.matches("PUT", "/users/123"))
            assertTrue(deleteRoute.matches("DELETE", "/users/123"))
            assertTrue(patchRoute.matches("PATCH", "/users/123"))
            assertFalse(putRoute.matches("DELETE", "/users/123"))
            assertFalse(deleteRoute.matches("PATCH", "/users/123"))
        }
    }
}