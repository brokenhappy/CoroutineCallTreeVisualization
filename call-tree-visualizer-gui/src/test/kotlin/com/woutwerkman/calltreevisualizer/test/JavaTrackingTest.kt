package com.woutwerkman.calltreevisualizer.test

import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType
import com.woutwerkman.calltreevisualizer.gui.trackingJavaCallStacks
import com.woutwerkman.calltreevisualizer.programsForTest.JavaTestPrograms
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JavaTrackingTest {

    @Test
    fun `linear java call tracks push and pop for each method`() = runBlocking {
        val events = trackingJavaCallStacks { JavaTestPrograms.linearCall() }.toList()

        val pushNodes = events
            .filter { it.eventType == CallStackTrackEventType.CallStackPushType }
            .map { it.node }
        val popNodes = events
            .filter { it.eventType == CallStackTrackEventType.CallStackPopType }
            .map { it.node }

        assertEquals(3, pushNodes.size, "Expected push events for linearCall, a, b")
        assertEquals(3, popNodes.size, "Expected pop events for linearCall, a, b")
    }

    @Test
    fun `linear java call has correct parent-child hierarchy`() = runBlocking {
        val events = trackingJavaCallStacks { JavaTestPrograms.linearCall() }.toList()

        val pushNodes = events
            .filter { it.eventType == CallStackTrackEventType.CallStackPushType }
            .map { it.node }

        val root = pushNodes.single { it.parent == null }
        assertTrue("linearCall" in root.functionFqn)

        val child = pushNodes.single { it.parent == root }
        val grandChild = pushNodes.single { it.parent == child }
        assertNull(grandChild.parent?.parent?.parent)
    }

    @Test
    fun `throwing java method emits throw event, catching method gets pop`() = runBlocking {
        val events = trackingJavaCallStacks { JavaTestPrograms.throwingCall() }.toList()

        val throwNode = events
            .filter { it.eventType is CallStackTrackEventType.CallStackThrowType }
            .map { it.node }
            .single()

        // throwing() is the inner method — it must have a parent (throwingCall)
        assertTrue(throwNode.parent != null, "The throwing method should have a parent")

        // throwingCall() catches the exception and returns normally, so it gets a Pop
        val popNodes = events
            .filter { it.eventType == CallStackTrackEventType.CallStackPopType }
            .map { it.node }
        val root = popNodes.single { it.parent == null }
        assertTrue("throwingCall" in root.functionFqn)
    }
}
