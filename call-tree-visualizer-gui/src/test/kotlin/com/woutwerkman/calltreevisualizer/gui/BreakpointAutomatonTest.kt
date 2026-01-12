package com.woutwerkman.calltreevisualizer.gui

import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEvent
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallTreeEventNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BreakpointAutomatonTest {

    private fun stubEvent(fqn: String, type: CallStackTrackEventType = CallStackTrackEventType.CallStackPushType): CallStackTrackEvent {
        return CallStackTrackEvent(CallTreeEventNode(1, fqn), type)
    }

    @Test
    fun `breakBefore pauses before function execution`() {
        val program = breakBefore(functionCall("foo"))
        val (automaton, _) = createAutomaton(program)

        val event = stubEvent("foo")
        val result = progressAutomaton(automaton, event, currentSpeed = null)

        assertTrue(result.shouldPauseBefore)
        assertFalse(result.shouldPauseAfter)
    }

    @Test
    fun `breakAfter pauses after function execution`() {
        val program = breakAfter(functionCall("foo"))
        val (automaton, _) = createAutomaton(program)

        val event = stubEvent("foo")
        val result = progressAutomaton(automaton, event, currentSpeed = null)

        assertFalse(result.shouldPauseBefore)
        assertTrue(result.shouldPauseAfter)
    }

    @Test
    fun `combining breakBefore and breakAfter pauses both before and after`() {
        val program = breakBefore(functionCall("foo")).then(breakAfter(functionCall("foo")))
        val (automaton, _) = createAutomaton(program)

        val event = stubEvent("foo")
        val result = progressAutomaton(automaton, event, currentSpeed = null)

        assertTrue(result.shouldPauseBefore)
        assertTrue(result.shouldPauseAfter)
    }

    @Test
    fun `changeSpeed sets initial speed and only pauses at specified breakpoint`() {
        val program = changeSpeed(10).then(breakBefore(functionCall("foo")))
        val (automaton, initialSpeed) = createAutomaton(program)

        assertEquals(10, initialSpeed)

        val event = stubEvent("foo")
        val result = progressAutomaton(automaton, event, currentSpeed = initialSpeed)
        assertTrue(result.shouldPauseBefore)

        val nextResult = progressAutomaton(result.nextAutomaton, stubEvent("bar"), currentSpeed = initialSpeed)
        assertFalse(nextResult.shouldPauseBefore)
        assertFalse(nextResult.shouldPauseAfter)
    }

    @Test
    fun `multiple consecutive changeSpeed calls consume all speed changes`() {
        val program = changeSpeed(10).then(changeSpeed(20)).then(changeSpeed(30)).then(breakBefore(functionCall("foo")))
        val (automaton, initialSpeed) = createAutomaton(program)

        assertEquals(30, initialSpeed, "Should consume all SetSpeed steps and return the last speed")

        val event = stubEvent("foo")
        val result = progressAutomaton(automaton, event, currentSpeed = initialSpeed)
        assertTrue(result.shouldPauseBefore)
    }
}
