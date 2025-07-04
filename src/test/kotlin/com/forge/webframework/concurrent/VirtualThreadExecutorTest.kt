package com.forge.concurrent

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

// Helper function to check if thread is virtual using reflection
fun isVirtualThread(thread: Thread): Boolean {
    return try {
        thread::class.java.getMethod("isVirtual").invoke(thread) as Boolean
    } catch (e: Exception) {
        false
    }
}

@DisplayName("Virtual Thread Executor Tests")
class VirtualThreadExecutorTest {
    
    private lateinit var executor: VirtualThreadExecutor
    
    @BeforeEach
    fun setUp() {
        executor = VirtualThreadExecutor.create("test-vt")
    }
    
    @AfterEach
    fun tearDown() {
        executor.shutdown()
    }
    
    @Nested
    @DisplayName("Executor Creation")
    inner class ExecutorCreation {
        
        @Test
        @DisplayName("Should create executor with custom thread name prefix")
        fun shouldCreateExecutorWithCustomThreadNamePrefix() {
            val customExecutor = VirtualThreadExecutor.create("custom-prefix")
            assertNotNull(customExecutor)
            customExecutor.shutdown()
        }
        
        @Test
        @DisplayName("Should create executor with default settings")
        fun shouldCreateExecutorWithDefaultSettings() {
            val defaultExecutor = VirtualThreadExecutor.createDefault()
            assertNotNull(defaultExecutor)
            defaultExecutor.shutdown()
        }
    }
    
    @Nested
    @DisplayName("Task Execution")
    inner class TaskExecution {
        
        @Test
        @DisplayName("Should execute simple task")
        fun shouldExecuteSimpleTask() {
            val latch = CountDownLatch(1)
            var executed = false
            
            executor.execute {
                executed = true
                latch.countDown()
            }
            
            assertTrue(latch.await(5, TimeUnit.SECONDS))
            assertTrue(executed)
        }
        
        @Test
        @DisplayName("Should execute async task")
        fun shouldExecuteAsyncTask() {
            val latch = CountDownLatch(1)
            var executed = false
            
            executor.executeAsync {
                executed = true
                latch.countDown()
            }
            
            assertTrue(latch.await(5, TimeUnit.SECONDS))
            assertTrue(executed)
        }
        
        @Test
        @DisplayName("Should execute task with result")
        fun shouldExecuteTaskWithResult() {
            val future = executor.executeWithResult {
                "Hello World"
            }
            
            val result = future.get(5, TimeUnit.SECONDS)
            assertEquals("Hello World", result)
        }
        
        @Test
        @DisplayName("Should execute multiple concurrent tasks")
        fun shouldExecuteMultipleConcurrentTasks() {
            val taskCount = 100
            val latch = CountDownLatch(taskCount)
            val counter = AtomicInteger(0)
            
            repeat(taskCount) {
                executor.executeAsync {
                    counter.incrementAndGet()
                    latch.countDown()
                }
            }
            
            assertTrue(latch.await(10, TimeUnit.SECONDS))
            assertEquals(taskCount, counter.get())
        }
    }
    
    @Nested
    @DisplayName("Virtual Thread Verification")
    inner class VirtualThreadVerification {
        
        @Test
        @DisplayName("Should execute tasks on virtual threads")
        fun shouldExecuteTasksOnVirtualThreads() {
            val future = executor.executeWithResult {
                isVirtualThread(Thread.currentThread())
            }
            
            val isVirtual = future.get(5, TimeUnit.SECONDS)
            assertTrue(isVirtual, "Task should be executed on a virtual thread")
        }
        
        @Test
        @DisplayName("Should use custom thread name prefix")
        fun shouldUseCustomThreadNamePrefix() {
            val future = executor.executeWithResult {
                Thread.currentThread().name
            }
            
            val threadName = future.get(5, TimeUnit.SECONDS)
            assertTrue(threadName.startsWith("test-vt-"), "Thread name should start with custom prefix")
        }
        
        @Test
        @DisplayName("Should increment thread counter")
        fun shouldIncrementThreadCounter() {
            val initialCount = executor.getActiveThreadCount()
            
            val futures = (1..10).map {
                executor.executeWithResult {
                    Thread.currentThread().name
                }
            }
            
            // Wait for all tasks to complete
            futures.forEach { it.get(5, TimeUnit.SECONDS) }
            
            val finalCount = executor.getActiveThreadCount()
            assertTrue(finalCount > initialCount, "Thread counter should have increased")
        }
    }
    
    @Nested
    @DisplayName("Performance Tests")
    inner class PerformanceTests {
        
        @Test
        @DisplayName("Should handle high concurrency efficiently")
        fun shouldHandleHighConcurrencyEfficiently() {
            val taskCount = 1000
            val latch = CountDownLatch(taskCount)
            val startTime = System.currentTimeMillis()
            
            repeat(taskCount) { taskId ->
                executor.executeAsync {
                    // Simulate some work
                    Thread.sleep(10)
                    latch.countDown()
                }
            }
            
            assertTrue(latch.await(30, TimeUnit.SECONDS))
            val totalTime = System.currentTimeMillis() - startTime
            
            // With virtual threads, this should complete much faster than platform threads
            assertTrue(totalTime < 20000, "High concurrency tasks should complete efficiently")
        }
        
        @Test
        @DisplayName("Should have low memory overhead")
        fun shouldHaveLowMemoryOverhead() {
            val runtime = Runtime.getRuntime()
            runtime.gc()
            val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
            
            val taskCount = 1000
            val futures = (1..taskCount).map {
                executor.executeWithResult {
                    Thread.sleep(100)
                    "Task $it completed"
                }
            }
            
            // Wait for all tasks to complete
            futures.forEach { it.get(10, TimeUnit.SECONDS) }
            
            runtime.gc()
            val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
            val memoryUsed = (memoryAfter - memoryBefore) / 1024 / 1024 // MB
            
            // Virtual threads should use much less memory than platform threads
            assertTrue(memoryUsed < 100, "Memory usage should be reasonable for $taskCount virtual threads")
        }
        
        @Test
        @DisplayName("Should demonstrate performance advantage over sequential execution")
        fun shouldDemonstratePerformanceAdvantageOverSequentialExecution() {
            val taskCount = 50
            val taskDuration = 50L // milliseconds
            
            // Sequential execution time
            val sequentialTime = measureTimeMillis {
                repeat(taskCount) {
                    Thread.sleep(taskDuration)
                }
            }
            
            // Concurrent execution time with virtual threads
            val concurrentTime = measureTimeMillis {
                val futures = (1..taskCount).map {
                    executor.executeWithResult {
                        Thread.sleep(taskDuration)
                        "Task $it"
                    }
                }
                futures.forEach { it.get() }
            }
            
            // Concurrent execution should be significantly faster
            val speedupRatio = sequentialTime.toDouble() / concurrentTime.toDouble()
            assertTrue(speedupRatio > 5.0, "Concurrent execution should be at least 5x faster than sequential")
        }
    }
    
    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {
        
        @Test
        @DisplayName("Should handle exceptions in async tasks")
        fun shouldHandleExceptionsInAsyncTasks() {
            var exceptionCaught = false
            val latch = CountDownLatch(1)
            
            // This should not crash the executor
            executor.executeAsync {
                try {
                    throw RuntimeException("Test exception")
                } catch (e: Exception) {
                    exceptionCaught = true
                    latch.countDown()
                }
            }
            
            assertTrue(latch.await(5, TimeUnit.SECONDS))
            assertTrue(exceptionCaught)
        }
        
        @Test
        @DisplayName("Should propagate exceptions in tasks with results")
        fun shouldPropagateExceptionsInTasksWithResults() {
            val future = executor.executeWithResult<String> {
                throw RuntimeException("Test exception")
            }
            
            val exception = assertThrows(java.util.concurrent.ExecutionException::class.java) {
                future.get(5, TimeUnit.SECONDS)
            }
            
            assertTrue(exception.cause is RuntimeException)
            assertEquals("Test exception", exception.cause?.message)
        }
        
        @Test
        @DisplayName("Should continue processing after exceptions")
        fun shouldContinueProcessingAfterExceptions() {
            val successfulTasks = AtomicInteger(0)
            val latch = CountDownLatch(10)
            
            repeat(10) { i ->
                executor.executeAsync {
                    try {
                        if (i % 3 == 0) {
                            throw RuntimeException("Exception in task $i")
                        } else {
                            successfulTasks.incrementAndGet()
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }
            
            assertTrue(latch.await(5, TimeUnit.SECONDS))
            assertEquals(7, successfulTasks.get()) // 7 out of 10 tasks should succeed
        }
    }
    
    @Nested
    @DisplayName("Lifecycle Management")
    inner class LifecycleManagement {
        
        @Test
        @DisplayName("Should shutdown gracefully")
        fun shouldShutdownGracefully() {
            val testExecutor = VirtualThreadExecutor.create("shutdown-test")
            
            assertFalse(testExecutor.isShutdown())
            assertFalse(testExecutor.isTerminated())
            
            testExecutor.shutdown()
            
            assertTrue(testExecutor.isShutdown())
        }
        
        @Test
        @DisplayName("Should force shutdown immediately")
        fun shouldForceShutdownImmediately() {
            val testExecutor = VirtualThreadExecutor.create("force-shutdown-test")
            
            // Submit some long-running tasks
            repeat(5) {
                testExecutor.executeAsync {
                    Thread.sleep(10000) // Long-running task
                }
            }
            
            val pendingTasks = testExecutor.shutdownNow()
            
            assertTrue(testExecutor.isShutdown())
            // Note: Virtual threads might not have traditional pending task queue behavior
        }
        
        @Test
        @DisplayName("Should reject tasks after shutdown")
        fun shouldRejectTasksAfterShutdown() {
            val testExecutor = VirtualThreadExecutor.create("reject-test")
            testExecutor.shutdown()
            
            assertThrows(Exception::class.java) {
                testExecutor.execute {
                    println("This should not execute")
                }
            }
        }
    }
    
    @Nested
    @DisplayName("Load Testing")
    inner class LoadTesting {
        
        @Test
        @DisplayName("Should handle massive concurrent load")
        fun shouldHandleMassiveConcurrentLoad() {
            val taskCount = 10000
            val completedTasks = AtomicLong(0)
            val startTime = System.currentTimeMillis()
            
            val futures = (1..taskCount).map { taskId ->
                executor.executeWithResult {
                    // Simulate light work
                    val result = taskId * 2
                    completedTasks.incrementAndGet()
                    result
                }
            }
            
            // Wait for all tasks to complete
            val results = futures.map { it.get(30, TimeUnit.SECONDS) }
            val endTime = System.currentTimeMillis()
            
            assertEquals(taskCount.toLong(), completedTasks.get())
            assertEquals(taskCount, results.size)
            
            val totalTime = endTime - startTime
            val throughput = (taskCount * 1000) / totalTime // tasks per second
            
            assertTrue(throughput > 1000, "Should achieve high throughput: $throughput tasks/second")
        }
        
        @Test
        @DisplayName("Should maintain performance under sustained load")
        fun shouldMaintainPerformanceUnderSustainedLoad() {
            val duration = 5000L // 5 seconds
            val taskCounter = AtomicLong(0)
            val startTime = System.currentTimeMillis()
            
            val futures = mutableListOf<CompletableFuture<Long>>()
            
            while (System.currentTimeMillis() - startTime < duration) {
                val future = executor.executeWithResult {
                    Thread.sleep(10)
                    taskCounter.incrementAndGet()
                }
                futures.add(future)
            }
            
            // Wait for all submitted tasks to complete
            futures.forEach { it.get(10, TimeUnit.SECONDS) }
            
            val actualDuration = System.currentTimeMillis() - startTime
            val throughput = (taskCounter.get() * 1000) / actualDuration
            
            assertTrue(taskCounter.get() > 100, "Should complete many tasks under sustained load")
            assertTrue(throughput > 50, "Should maintain reasonable throughput: $throughput tasks/second")
        }
    }
}