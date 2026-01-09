package com.woutwerkman.calltreevisualizer.gui

import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEvent
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallTreeNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class BreakpointAutomatonTest {

    private fun stubEvent(fqn: String, type: CallStackTrackEventType = CallStackTrackEventType.CallStackPushType(fqn)): CallStackTrackEvent {
        return CallStackTrackEvent(CallTreeNode(1, fqn), type)
    }

    @Test
    fun testBreakBefore() {
        val program = breakBeforeEvent(functionCall("foo"))
        val (automaton, _) = BreakpointAutomaton.create(program)
        
        val event = stubEvent("foo")
        val progression = automaton.progress(event, isResumed = true, currentSpeed = null)
        
        assertEquals(BreakType.BEFORE, progression.breakType)
        assertEquals(DebuggerState.Paused, progression.nextDebuggerState)
    }

    @Test
    fun testBreakAfter() {
        val program = breakAfterEvent(functionCall("foo"))
        val (automaton, _) = BreakpointAutomaton.create(program)
        
        val event = stubEvent("foo")
        val progression = automaton.progress(event, isResumed = true, currentSpeed = null)
        
        assertEquals(BreakType.AFTER, progression.breakType)
        // For AFTER, it returns Paused as the next state
        assertEquals(DebuggerState.Paused, progression.nextDebuggerState)
    }

    @Test
    fun testBreakBoth() {
        val program = breakBeforeEvent(functionCall("foo")).then(breakAfterEvent(functionCall("foo")))
        val (automaton, _) = BreakpointAutomaton.create(program)
        
        val event = stubEvent("foo")
        val progression = automaton.progress(event, isResumed = true, currentSpeed = null)
        
        assertEquals(BreakType.BOTH, progression.breakType)
        assertEquals(DebuggerState.Paused, progression.nextDebuggerState)
    }

    @Test
    fun testChangeSpeed() {
        val program = changeSpeed(10).then(breakBeforeEvent(functionCall("foo")))
        val (automaton, initialSpeed) = BreakpointAutomaton.create(program)
        
        assertEquals(10, initialSpeed)
        
        val event = stubEvent("foo")
        val progression = automaton.progress(event, isResumed = true, currentSpeed = initialSpeed)
        assertEquals(BreakType.BEFORE, progression.breakType)
        assertEquals(DebuggerState.Paused, progression.nextDebuggerState)
        
        // After resuming
        val nextState = progression.nextAutomaton.progress(stubEvent("bar"), isResumed = true, currentSpeed = initialSpeed).nextDebuggerState
        assertEquals(DebuggerState.RunningAtLimitedSpeed(10), nextState)
    }
}
