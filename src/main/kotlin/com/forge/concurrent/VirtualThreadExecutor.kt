package com.forge.concurrent

import org.slf4j.LoggerFactory
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicLong

class VirtualThreadExecutor private constructor(
    private val threadNamePrefix: String = "vt-forge"
) : Executor {
    
    private val logger = LoggerFactory.getLogger(VirtualThreadExecutor::class.java)
    private val threadCounter = AtomicLong(0)
    private val executor: ExecutorService
    
    init {
        val threadFactory = ThreadFactory { runnable ->
            try {
                // Try to use virtual threads if available (JDK 21)
                val virtualBuilder = Thread::class.java.getMethod("ofVirtual").invoke(null)
                val namedBuilder = virtualBuilder::class.java.getMethod("name", String::class.java).invoke(virtualBuilder, "$threadNamePrefix-${threadCounter.incrementAndGet()}")
                val factoryMethod = namedBuilder::class.java.getMethod("factory")
                val factory = factoryMethod.invoke(namedBuilder) as ThreadFactory
                factory.newThread(runnable)
            } catch (e: Exception) {
                // Fallback to regular threads if virtual threads not available
                Thread(runnable, "$threadNamePrefix-${threadCounter.incrementAndGet()}")
            }
        }
        
        executor = try {
            // Try to use newThreadPerTaskExecutor if available (JDK 21)
            Executors::class.java.getMethod("newThreadPerTaskExecutor", ThreadFactory::class.java).invoke(null, threadFactory) as ExecutorService
        } catch (e: Exception) {
            // Fallback to cached thread pool
            Executors.newCachedThreadPool(threadFactory)
        }
        logger.info("VirtualThreadExecutor initialized with prefix: $threadNamePrefix")
    }
    
    companion object {
        @JvmStatic
        fun create(threadNamePrefix: String = "vt-forge"): VirtualThreadExecutor {
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