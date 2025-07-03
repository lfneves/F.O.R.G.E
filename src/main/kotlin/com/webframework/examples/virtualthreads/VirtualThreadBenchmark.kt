package com.webframework.examples.virtualthreads

import com.webframework.concurrent.VirtualThreadExecutor
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

object VirtualThreadBenchmark {
    
    fun runBenchmark() {
        println("JDK 21 Virtual Threads Benchmark")
        println("=================================")
        
        benchmarkConcurrentTasks()
        benchmarkMemoryUsage()
        benchmarkThroughput()
    }
    
    private fun benchmarkConcurrentTasks() {
        println("\n1. Concurrent Tasks Benchmark")
        println("-----------------------------")
        
        val taskCount = 10000
        val taskDuration = 100L
        
        val platformTime = measureTimeMillis {
            val executor = Executors.newFixedThreadPool(200)
            val tasks = (1..taskCount).map {
                CompletableFuture.supplyAsync({
                    Thread.sleep(taskDuration)
                    "Task $it"
                }, executor)
            }
            CompletableFuture.allOf(*tasks.toTypedArray()).get()
            executor.shutdown()
        }
        
        val virtualTime = measureTimeMillis {
            val executor = VirtualThreadExecutor.create("benchmark")
            val tasks = (1..taskCount).map {
                executor.executeWithResult {
                    Thread.sleep(taskDuration)
                    "Task $it"
                }
            }
            CompletableFuture.allOf(*tasks.toTypedArray()).get()
            executor.shutdown()
        }
        
        println("Platform threads: ${platformTime}ms")
        println("Virtual threads:  ${virtualTime}ms")
        println("Performance gain: ${((platformTime - virtualTime).toDouble() / platformTime * 100).toInt()}%")
    }
    
    private fun benchmarkMemoryUsage() {
        println("\n2. Memory Usage Benchmark")
        println("-------------------------")
        
        val runtime = Runtime.getRuntime()
        
        runtime.gc()
        val memBefore = runtime.totalMemory() - runtime.freeMemory()
        
        val virtualExecutor = VirtualThreadExecutor.create("memory-test")
        val tasks = (1..1000).map { i ->
            virtualExecutor.executeWithResult {
                Thread.sleep(50)
                ByteArray(1024) { (i % 256).toByte() }
            }
        }
        
        CompletableFuture.allOf(*tasks.toTypedArray()).get()
        
        runtime.gc()
        val memAfter = runtime.totalMemory() - runtime.freeMemory()
        
        virtualExecutor.shutdown()
        
        val memoryUsed = (memAfter - memBefore) / 1024 / 1024
        println("Memory used for 1000 virtual threads: ${memoryUsed}MB")
        println("Average memory per virtual thread: ${memoryUsed * 1024 / 1000}KB")
    }
    
    private fun benchmarkThroughput() {
        println("\n3. Throughput Benchmark")
        println("-----------------------")
        
        val duration = 5000L
        val virtualExecutor = VirtualThreadExecutor.create("throughput")
        
        var completedTasks = 0
        val startTime = System.currentTimeMillis()
        
        val tasks = mutableListOf<CompletableFuture<Unit>>()
        
        while (System.currentTimeMillis() - startTime < duration) {
            val task = virtualExecutor.executeWithResult {
                Thread.sleep(10)
                completedTasks++
                Unit
            }
            tasks.add(task)
        }
        
        CompletableFuture.allOf(*tasks.toTypedArray()).get()
        virtualExecutor.shutdown()
        
        val actualDuration = System.currentTimeMillis() - startTime
        val throughput = (completedTasks * 1000) / actualDuration
        
        println("Tasks completed in ${actualDuration}ms: $completedTasks")
        println("Throughput: $throughput tasks/second")
    }
}

fun main() {
    VirtualThreadBenchmark.runBenchmark()
}