package com.woutwerkman

import java.lang.ScopedValue

fun <T> stackTrackedJava(functionFqn: String, child: () -> T): T =
    tracker.orElse(object : StackTrackerJava {
        override fun <T> track(functionFqn: String, child: () -> T): T = child()
    }).track(functionFqn, child)

val tracker: ScopedValue<StackTrackerJava> = ScopedValue.newInstance()

interface StackTrackerJava {
    fun <T> track(functionFqn: String, child: () -> T): T
}