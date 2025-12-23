package com.woutwerkman.calltreevisualizer.coroutineintegration

import com.woutwerkman.calltreevisualizer.StackTrackingContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException


public class CallTreeNode(public val functionFqn: String, public val parent: CallTreeNode? = null)

public class CallStackTrackEvent(public val node: CallTreeNode, public val eventType: CallStackTrackEventType)

public sealed class CallStackTrackEventType {
    public data class CallStackPushType(val functionFqn: String) : CallStackTrackEventType()
    public data object CallStackPopType : CallStackTrackEventType()
    public class CallStackThrowType(public val throwable: Throwable) : CallStackTrackEventType()
}

public fun trackingCallStacks(block: suspend () -> Unit): Flow<CallStackTrackEvent> = channelFlow {
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
            val childNode = CallTreeNode(functionFqn, this@toStackTrackedCoroutineContext)
            sendOnFlowScope(CallStackTrackEvent(childNode, CallStackTrackEventType.CallStackPushType(functionFqn)))
            return try {
                withContext(childNode.toStackTrackedCoroutineContext()) {
                    child()
                }.also { sendOnFlowScope(CallStackTrackEvent(childNode, CallStackTrackEventType.CallStackPopType)) }
            } catch (t: Throwable) {
                sendOnFlowScope(CallStackTrackEvent(childNode, CallStackTrackEventType.CallStackThrowType(t)))
                throw t
            }
        }
    }

    withContext(null.toStackTrackedCoroutineContext()) {
        block()
    }
}.buffer(RENDEZVOUS)