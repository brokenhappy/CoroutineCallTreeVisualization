package com.woutwerkman.calltreevisualizer

import groupingBy
import map
import reduce
import susSequence
import suseLines
import toList
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

suspend fun measureLinearly() {
    (object {})::class
        .java
        .getResource("measurements.txt")!!
        .toURI()
        .toPath()
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
