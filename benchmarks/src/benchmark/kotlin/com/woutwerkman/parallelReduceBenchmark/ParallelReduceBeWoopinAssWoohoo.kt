package com.woutwerkman.parallelReduceBenchmark

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

@State(Scope.Benchmark)
open class ParallelReduceBenchmark {

    private lateinit var list: List<Int>
    private val processorCount = Runtime.getRuntime().availableProcessors()

    @Setup
    open fun prepare() {
        list = (0..1_024).map { (-200..300).random() }
    }

    @Benchmark
    open fun synchronous() = runBlockingMultithreaded {
        list.reduce(Int::plus)
    }

    @Benchmark
    open fun parallel() = runBlockingMultithreaded {
        list.parallelReduce(processorCount, operation = Int::plus)
    }
}

// Too lazy to find the proper solution rn
fun <T> runBlockingMultithreaded(block: suspend CoroutineScope.() -> T): T {
    val future = CompletableFuture<T>()
    GlobalScope.launch(Dispatchers.Default) { future.complete(block()) }
    return future.get()
}