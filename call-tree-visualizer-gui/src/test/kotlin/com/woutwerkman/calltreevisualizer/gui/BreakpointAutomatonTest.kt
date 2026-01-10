package com.woutwerkman.calltreevisualizer.gui

import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEvent
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallTreeNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BreakpointAutomatonTest {

    private fun stubEvent(fqn: String, type: CallStackTrackEventType = CallStackTrackEventType.CallStackPushType(fqn)): CallStackTrackEvent {
        return CallStackTrackEvent(CallTreeNode(1, fqn), type)
    }

    @Test
    fun testBreakBefore() {
        val program = breakBefore(functionCall("foo"))
        val (automaton, _) = createAutomaton(program)

        val event = stubEvent("foo")
        val result = progressAutomaton(automaton, event, currentSpeed = null)

        assertTrue(result.shouldPauseBefore)
        assertFalse(result.shouldPauseAfter)
    }

    @Test
    fun testBreakAfter() {
        val program = breakAfter(functionCall("foo"))
        val (automaton, _) = createAutomaton(program)

        val event = stubEvent("foo")
        val result = progressAutomaton(automaton, event, currentSpeed = null)

        assertFalse(result.shouldPauseBefore)
        assertTrue(result.shouldPauseAfter)
    }

    @Test
    fun testBreakBoth() {
        val program = breakBefore(functionCall("foo")).then(breakAfter(functionCall("foo")))
        val (automaton, _) = createAutomaton(program)

        val event = stubEvent("foo")
        val result = progressAutomaton(automaton, event, currentSpeed = null)

        assertTrue(result.shouldPauseBefore)
        assertTrue(result.shouldPauseAfter)
    }

    @Test
    fun testChangeSpeed() {
        val program = changeSpeed(10).then(breakBefore(functionCall("foo")))
        val (automaton, initialSpeed) = createAutomaton(program)

        assertEquals(10, initialSpeed)

        val event = stubEvent("foo")
        val result = progressAutomaton(automaton, event, currentSpeed = initialSpeed)
        assertTrue(result.shouldPauseBefore)

        // After resuming - the next event should not cause a pause
        val nextResult = progressAutomaton(result.nextAutomaton, stubEvent("bar"), currentSpeed = initialSpeed)
        assertFalse(nextResult.shouldPauseBefore)
        assertFalse(nextResult.shouldPauseAfter)
    }
}
