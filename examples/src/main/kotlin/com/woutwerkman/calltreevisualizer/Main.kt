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

suspend fun programWithAllTypes() {
    measureLinearly()
    linearExplosion()
    measureLinearlyWithUnstructuredConcurrency()
    firstStructuredConcurrency()
    highlyBranchingCode()
}

suspend fun firstStructuredConcurrency() {
    coroutineScope {
        launch { yield() }
        yield()
    }
}

suspend fun highlyBranchingCode() {
    coroutineScope {
        launch { bar(shouldThrowInLaunchedCoroutine = true) }
        bar(shouldThrowInLaunchedCoroutine = false)
    }
}

suspend fun bar(shouldThrowInLaunchedCoroutine: Boolean) {
    coroutineScope {
        launch { baz(shouldThrow = shouldThrowInLaunchedCoroutine) }
        baz(shouldThrow = false)
    }
}

suspend fun baz(shouldThrow: Boolean) {
    if (shouldThrow) error("Aaaah!")
    awaitCancellation()
}

@NonTracked
public suspend fun <T> owningGlobalScope(block: suspend () -> T): T =
    kotlinhax.shadowroutines.owningGlobalScope { block() }

suspend fun recurse(a: Int) {
    if (a <= 0) error("AAAH")
    recurse(a - 1)
}

suspend fun linearExplosion() {
    try {
        recurse(10)
    } catch (_: Throwable) {
        // ... Continue
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
                .map { it.getCity() to it.getTotalTemperature() / it.getCount() }
                .toList()
                .sortedByDescending { (_, temperature) -> temperature }
                .joinToString("\n") { (city, temperature) -> "$city: $temperature" }
        }
        .also(::println)
}
