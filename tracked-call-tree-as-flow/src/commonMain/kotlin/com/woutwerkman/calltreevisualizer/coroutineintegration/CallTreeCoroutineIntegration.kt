package com.woutwerkman.calltreevisualizer.coroutineintegration

import com.woutwerkman.calltreevisualizer.StackTrackingContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.coroutines.cancellation.CancellationException


public class CallTreeNode(public val id: Int, public val functionFqn: String, public val parent: CallTreeNode? = null)

public class CallStackTrackEvent(public val node: CallTreeNode, public val eventType: CallStackTrackEventType) {
    public operator fun component1(): CallTreeNode = node
    public operator fun component2(): CallStackTrackEventType = eventType
}

public sealed class CallStackTrackEventType {
    public data class CallStackPushType(val functionFqn: String) : CallStackTrackEventType()
    public data object CallStackPopType : CallStackTrackEventType()
    public class CallStackThrowType(public val throwable: Throwable) : CallStackTrackEventType()
    public object CallStackCancelled : CallStackTrackEventType()
}

@OptIn(ExperimentalAtomicApi::class)
public fun trackingCallStacks(
    block: suspend CoroutineScope.(rootNode: StackTrackingContext) -> Unit,
): Flow<CallStackTrackEvent> = channelFlow {
    val nodeCounter = AtomicInt(0)
    suspend fun sendOnFlowScope(callStackTrackEvent: CallStackTrackEvent) {
        try {
            send(callStackTrackEvent)
        } catch (c: CancellationException) {
            withContext(NonCancellable) {
                this@channelFlow.async { send(callStackTrackEvent) }.await()
            }
            throw c
        }
    }
    fun CallTreeNode?.toStackTrackedCoroutineContext(): StackTrackingContext = object : StackTrackingContext {
        override suspend fun <T> track(functionFqn: String, child: suspend () -> T): T {
            val childNode = CallTreeNode(nodeCounter.incrementAndFetch(), functionFqn, this@toStackTrackedCoroutineContext)
            sendOnFlowScope(CallStackTrackEvent(childNode, CallStackTrackEventType.CallStackPushType(functionFqn)))
            return try {
                withContext(childNode.toStackTrackedCoroutineContext()) {
                    child()
                }.also { sendOnFlowScope(CallStackTrackEvent(childNode, CallStackTrackEventType.CallStackPopType)) }
            } catch (t: Throwable) {
                sendOnFlowScope(CallStackTrackEvent(
                    childNode,
                    if (t is CancellationException && currentCoroutineContext().isActive)
                        CallStackTrackEventType.CallStackThrowType(t)
                    else CallStackTrackEventType.CallStackCancelled,
                ))
                throw t
            }
        }
    }

    val rootTracker = null.toStackTrackedCoroutineContext()
    withContext(rootTracker) {
        block(rootTracker)
    }
}.buffer(RENDEZVOUS)