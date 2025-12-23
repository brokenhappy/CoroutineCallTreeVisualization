package com.woutwerkman.calltreevisualizer

import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

public annotation class NonTracked

public class CallTreeNode(public val functionFqn: String, public val parent: CallTreeNode? = null)

public class CallStackTrackEvent(public val node: CallTreeNode, public val eventType: CallStackTrackEventType)

public sealed class CallStackTrackEventType {
    public data class CallStackPushType(val functionFqn: String) : CallStackTrackEventType()
    public data object CallStackPopType : CallStackTrackEventType()
    public class CallStackThrowType(public val throwable: Throwable) : CallStackTrackEventType()
}

public interface StackTrackingContext: CoroutineContext.Element {
    public suspend fun <T> track(functionFqn: String, child: suspend () -> T): T
    public companion object : CoroutineContext.Key<StackTrackingContext>
    override val key: CoroutineContext.Key<*> get() = StackTrackingContext
}

public suspend inline fun <T> stackTracked(functionFqn: String, noinline child: suspend () -> T): T =
    currentCoroutineContext()[StackTrackingContext]!!.track(functionFqn, child)

public fun trackingCallStacks(block: suspend () -> Unit): Flow<CallStackTrackEvent> = channelFlow {
    fun CallTreeNode?.toStackTrackedCoroutineContext(): StackTrackingContext = object : StackTrackingContext {
        override suspend fun <T> track(functionFqn: String, child: suspend () -> T): T {
            val childNode = CallTreeNode(functionFqn, this@toStackTrackedCoroutineContext)
            send(CallStackTrackEvent(childNode, CallStackTrackEventType.CallStackPushType(functionFqn)))
            return try {
                withContext(childNode.toStackTrackedCoroutineContext()) {
                    child()
                }.also { send(CallStackTrackEvent(childNode, CallStackTrackEventType.CallStackPopType)) }
            } catch (t: Throwable) {
                send(CallStackTrackEvent(childNode, CallStackTrackEventType.CallStackThrowType(t)))
                throw t
            }
        }
    }

    withContext(null.toStackTrackedCoroutineContext()) {
        block()
    }
}.buffer(RENDEZVOUS)