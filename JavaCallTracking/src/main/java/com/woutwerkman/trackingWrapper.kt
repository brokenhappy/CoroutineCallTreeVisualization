package com.woutwerkman

import java.lang.ScopedValue

fun <T> stackTrackedJava(functionFqn: String, child: () -> T): T =
    if (!tracker.isBound()) child()
    else tracker.get().track(functionFqn, child)

val tracker: ScopedValue<StackTrackerJava> = ScopedValue.newInstance()

/**
 * Any implementations of this interface will not have their members wrapped with [stackTrackedJava] calls.
 * This prevents infinite recursion in vast majority of cases.
 */
interface StackTrackerJava {
    fun <T> track(functionFqn: String, child: () -> T): T
}