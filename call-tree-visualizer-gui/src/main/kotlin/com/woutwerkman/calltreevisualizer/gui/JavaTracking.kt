package com.woutwerkman.calltreevisualizer.gui

import com.woutwerkman.StackTrackerJava
import com.woutwerkman.tracker
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEvent
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallTreeEventNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.Callable
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

@OptIn(ExperimentalAtomicApi::class)
fun trackingJavaCallStacks(program: Runnable): Flow<CallStackTrackEvent> = channelFlow {
    val nodeCounter = AtomicInt(0)
    val currentNode = ScopedValue.newInstance<CallTreeEventNode>()

    val trackerImpl = object : StackTrackerJava {
        override fun <T> track(functionFqn: String, child: () -> T): T {
            val parentNode = if (currentNode.isBound()) currentNode.get() else null
            val childNode = CallTreeEventNode(nodeCounter.incrementAndFetch(), functionFqn, parentNode)
            trySendBlocking(CallStackTrackEvent(childNode, CallStackTrackEventType.CallStackPushType))
            return try {
                ScopedValue.where(currentNode, childNode).call<T, Throwable> { child() }.also {
                    trySendBlocking(CallStackTrackEvent(childNode, CallStackTrackEventType.CallStackPopType))
                }
            } catch (t: Throwable) {
                trySendBlocking(CallStackTrackEvent(childNode, CallStackTrackEventType.CallStackThrowType(t)))
                throw t
            }
        }
    }

    withContext(Dispatchers.IO) {
        ScopedValue.where(tracker, trackerImpl).run { program.run() }
    }
}.buffer(RENDEZVOUS)
