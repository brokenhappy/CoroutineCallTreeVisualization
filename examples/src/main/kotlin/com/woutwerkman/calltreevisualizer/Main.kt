package com.woutwerkman.calltreevisualizer

import groupingBy
import kotlinhax.shadowroutines.async
import kotlinhax.shadowroutines.launch
import kotlinhax.shadowroutines.coroutineScope
import map
import reduce
import susSequence
import suseLines
import toList
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.toPath

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
