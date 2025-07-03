package com.webframework.concurrent

import org.slf4j.LoggerFactory
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicLong

class VirtualThreadExecutor private constructor(
    private val threadNamePrefix: String = "vt-webframework"
) : Executor {
    
    private val logger = LoggerFactory.getLogger(VirtualThreadExecutor::class.java)
    private val threadCounter = AtomicLong(0)
    private val executor: ExecutorService
    
    init {
        // TODO: Java 21 Virtual Thread features commented out for Java 17 compatibility
        // val threadFactory = ThreadFactory { runnable ->
        //     Thread.ofVirtual()
        //         .name("$threadNamePrefix-${threadCounter.incrementAndGet()}")
        //         .factory()
        //         .newThread(runnable)
        // }
        // 
        // executor = Executors.newThreadPerTaskExecutor(threadFactory)
        
        // Java 17 compatible implementation using fixed thread pool
        val threadFactory = ThreadFactory { runnable ->
            Thread(runnable, "$threadNamePrefix-${threadCounter.incrementAndGet()}")
        }
        
        executor = Executors.newCachedThreadPool(threadFactory)
        logger.info("VirtualThreadExecutor initialized with prefix: $threadNamePrefix (Java 17 compatible mode)")
    }
    
    companion object {
        @JvmStatic
        fun create(threadNamePrefix: String = "vt-webframework"): VirtualThreadExecutor {
            return VirtualThreadExecutor(threadNamePrefix)
        }
        
        @JvmStatic
        fun createDefault(): VirtualThreadExecutor {
            return VirtualThreadExecutor()
        }
    }
    
    override fun execute(command: Runnable) {
        try {
            executor.execute(command)
        } catch (e: Exception) {
            logger.error("Failed to execute task on virtual thread", e)
            throw e
        }
    }
    
    fun executeAsync(task: () -> Unit) {
        execute {
            try {
                task()
            } catch (e: Exception) {
                logger.error("Error in async task execution", e)
            }
        }
    }
    
    fun <T> executeWithResult(task: () -> T): java.util.concurrent.CompletableFuture<T> {
        val future = java.util.concurrent.CompletableFuture<T>()
        
        execute {
            try {
                val result = task()
                future.complete(result)
            } catch (e: Exception) {
                logger.error("Error in task execution with result", e)
                future.completeExceptionally(e)
            }
        }
        
        return future
    }
    
    fun shutdown() {
        try {
            executor.shutdown()
            logger.info("VirtualThreadExecutor shutdown completed")
        } catch (e: Exception) {
            logger.error("Error during VirtualThreadExecutor shutdown", e)
        }
    }
    
    fun shutdownNow(): List<Runnable> {
        return try {
            val pendingTasks = executor.shutdownNow()
            logger.info("VirtualThreadExecutor force shutdown completed, ${pendingTasks.size} tasks were pending")
            pendingTasks
        } catch (e: Exception) {
            logger.error("Error during VirtualThreadExecutor force shutdown", e)
            emptyList()
        }
    }
    
    fun isShutdown(): Boolean = executor.isShutdown
    
    fun isTerminated(): Boolean = executor.isTerminated
    
    fun getActiveThreadCount(): Long {
        return threadCounter.get()
    }
}