package com.woutwerkman.calltreevisualizer

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

public annotation class NonTracked

public interface StackTrackingContext: CoroutineContext.Element {
    public suspend fun <T> track(functionFqn: String, child: suspend () -> T): T
    public companion object : CoroutineContext.Key<StackTrackingContext>
    override val key: CoroutineContext.Key<*> get() = StackTrackingContext
}

public suspend inline fun <T> stackTracked(functionFqn: String, noinline child: suspend () -> T): T =
    coroutineContext[StackTrackingContext]!!.track(functionFqn, child)
