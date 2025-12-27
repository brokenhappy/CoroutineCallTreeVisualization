package com.woutwerkman.calltreevisualizer

import kotlinhax.shadowroutines.*
import kotlin.io.path.Path

data class Measurement(
    private val _city: String,
    private val _totalTemperature: Double,
    private val _count: Int,
) {
    suspend fun getCity() = _city
    suspend fun getTotalTemperature() = _totalTemperature
    suspend fun getCount() = _count
}

private suspend fun Measurement.add(other: Measurement): Measurement = copy(
    _totalTemperature = other.getTotalTemperature() + getTotalTemperature(),
    _count = other.getCount() + getCount(),
)

suspend fun highlyBranchingCalls() {
    for (i in (0..100).susSequence()) {
        foo()
    }
}

suspend fun foo() {
    coroutineScope {
        launch { bar() }
        bar()
    }
}

suspend fun bar() {
    coroutineScope {
        launch { baz() }
        baz()
    }
}

suspend fun baz() {
    coroutineScope {
        launch { foobs() }
        foobs()
    }
}

suspend fun foobs() {}

@NonTracked
public suspend fun runGlobalScopeTracker(tracker: StackTrackingContext): Nothing =
    kotlinhax.shadowroutines.runGlobalScopeTracker(tracker)

suspend fun recurse(a: Int) {
    if (a <= 0) error("AAAH")
    recurse(a - 1)
}

suspend fun linearExplosion() {
    repeat(10) {
        try {
            recurse(10)
        } catch (_: Throwable) {
            // ... Continue
        }
    }
}

suspend fun measureLinearlyWithUnstructuredConcurrency() {
    val tasks = (0..3).map {
        GlobalScope.launch { measureLinearly() }
    }
    try {
        measureLinearly()
        tasks.forEach { it.join() }
    } finally {
        tasks.forEach { it.cancel() }
    }
}

suspend fun measureLinearly() {
    Path("/Users/Wout.Werkman/IdeaProjects/CoroutineCallTreeVisualization/examples/src/main/resources/measurements.txt")
//    (object {})::class
//        .java
//        .getResource("measurements.txt")!!
//        .toURI()
//        .toPath()
        .suseLines { lines ->
            lines
                .map { it.split(": ") }
                .map { (city, temperature) -> Measurement(city, temperature.toDouble(), 1) }
                .groupingBy { it.getCity() }
                .reduce { _, acc, it -> it.add(acc) }
                .values
                .susSequence()
                .map {
                    coroutineScope {
                        val city = async { it.getCity() }
                        val totalTemperature = async { it.getTotalTemperature() }
                        city.await() to totalTemperature.await() / it.getCount()
                    }
                }
                .toList()
                .sortedByDescending { (_, temperature) -> temperature }
                .joinToString("\n") { (city, temperature) -> "$city: $temperature" }
        }
        .also(::println)
}
