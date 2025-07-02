/**
 * Virtual threads support for high-performance concurrent request processing.
 * 
 * This package leverages JDK 21 Virtual Threads to provide:
 * - [VirtualThreadExecutor]: Custom executor for managing virtual threads
 * - High-concurrency request handling with minimal memory overhead
 * - Async task execution with CompletableFuture integration
 * - Thread naming and lifecycle management
 * 
 * Virtual threads enable handling millions of concurrent requests with
 * significantly lower memory usage compared to platform threads.
 */
package com.webframework.concurrent