package com.woutwerkman.calltreevisualizer.gui

import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEvent
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType
import kotlin.reflect.*
import kotlin.reflect.jvm.javaMethod

// Event Matchers - Pure data + functions
sealed interface BreakpointEventMatcher

data class FunctionCallMatcher(val fqn: String) : BreakpointEventMatcher
data class FunctionThrowsMatcher(val fqn: String) : BreakpointEventMatcher
data class FunctionCancelsMatcher(val fqn: String) : BreakpointEventMatcher
data object NextStepMatcher : BreakpointEventMatcher

fun BreakpointEventMatcher.matches(event: CallStackTrackEvent): Boolean = when (this) {
    is FunctionCallMatcher -> {
        val type = event.eventType
        type is CallStackTrackEventType.CallStackPushType && type.functionFqn == fqn
    }
    is FunctionThrowsMatcher -> {
        val type = event.eventType
        type is CallStackTrackEventType.CallStackThrowType && event.node.functionFqn == fqn
    }
    is FunctionCancelsMatcher -> {
        val type = event.eventType
        type is CallStackTrackEventType.CallStackCancelled && event.node.functionFqn == fqn
    }
    NextStepMatcher -> true
}

fun functionCall(fqn: String) = FunctionCallMatcher(fqn)
fun functionThrows(fqn: String) = FunctionThrowsMatcher(fqn)
fun functionCancels(fqn: String) = FunctionCancelsMatcher(fqn)
fun functionCall(function: KFunction<Unit>) = FunctionCallMatcher(function.javaMethod!!.declaringClass.packageName + "." + function.name)
fun functionThrows(function: KFunction<Unit>) = FunctionThrowsMatcher(function.javaMethod!!.declaringClass.packageName + "." + function.name)
fun functionCancels(function: KFunction<Unit>) = FunctionCancelsMatcher(function.javaMethod!!.declaringClass.packageName + "." + function.name)

sealed class BreakpointSteps {
    data object Empty : BreakpointSteps()
    data class SetSpeed(val eventsPerSecond: Int, val next: BreakpointSteps) : BreakpointSteps()
    data class BreakBefore(val matcher: BreakpointEventMatcher, val next: BreakpointSteps) : BreakpointSteps()
    data class BreakAfter(val matcher: BreakpointEventMatcher, val next: BreakpointSteps) : BreakpointSteps()
}

fun BreakpointSteps.append(other: BreakpointSteps): BreakpointSteps = when (this) {
    BreakpointSteps.Empty -> other
    is BreakpointSteps.SetSpeed -> copy(next = next.append(other))
    is BreakpointSteps.BreakBefore -> copy(next = next.append(other))
    is BreakpointSteps.BreakAfter -> copy(next = next.append(other))
}

data class BreakpointProgram(val steps: BreakpointSteps)

fun BreakpointProgram.then(program: BreakpointProgram): BreakpointProgram =
    BreakpointProgram(steps.append(program.steps))

fun changeSpeed(eventsPerSecond: Int) = BreakpointProgram(BreakpointSteps.SetSpeed(eventsPerSecond, BreakpointSteps.Empty))
val Int.eventsPerSecond get() = this

fun breakBefore(matcher: BreakpointEventMatcher) = BreakpointProgram(BreakpointSteps.BreakBefore(matcher, BreakpointSteps.Empty))
fun breakAfter(matcher: BreakpointEventMatcher) = BreakpointProgram(BreakpointSteps.BreakAfter(matcher, BreakpointSteps.Empty))
fun breakAtNextStep() = BreakpointProgram(BreakpointSteps.BreakBefore(NextStepMatcher, BreakpointSteps.Empty))

enum class StepSignal {
    Step, Resume
}

sealed class DebuggerState {
    data object Unrestrained : DebuggerState()
    data class RunningAtLimitedSpeed(val eventsPerSecond: Int) : DebuggerState()
    data object Paused : DebuggerState()
    data object WaitingForSingleStep : DebuggerState()
}

fun debuggerStateFromSpeed(speed: Int?, isResumed: Boolean): DebuggerState = when {
    !isResumed -> DebuggerState.Paused
    speed == null -> DebuggerState.Unrestrained
    else -> DebuggerState.RunningAtLimitedSpeed(speed)
}

enum class BreakType {
    NONE, BEFORE, AFTER, BOTH
}

fun BreakType.combine(other: BreakType): BreakType = when {
    this == BreakType.NONE -> other
    other == BreakType.NONE -> this
    this == BreakType.BEFORE && other == BreakType.AFTER -> BreakType.BOTH
    this == BreakType.AFTER && other == BreakType.BEFORE -> BreakType.BOTH
    else -> this
}

data class BreakpointProgression(
    val nextAutomaton: BreakpointAutomaton,
    val nextDebuggerState: DebuggerState,
    val breakType: BreakType,
    val newSpeed: Int? = null
)

data class BreakpointAutomaton(val steps: BreakpointSteps)

private data class ProgressState(
    val steps: BreakpointSteps,
    val breakType: BreakType,
    val speed: Int?,
    val speedChanged: Boolean
)

private tailrec fun advanceOverSpeedChanges(steps: BreakpointSteps, currentSpeed: Int?): ProgressState =
    when (steps) {
        is BreakpointSteps.SetSpeed -> advanceOverSpeedChanges(steps.next, steps.eventsPerSecond)
        else -> ProgressState(steps, BreakType.NONE, currentSpeed, currentSpeed != null)
    }

private fun checkBreakpoint(
    steps: BreakpointSteps,
    event: CallStackTrackEvent,
    currentBreakType: BreakType,
    speed: Int?
): ProgressState = when (steps) {
    BreakpointSteps.Empty -> ProgressState(steps, currentBreakType, speed, false)
    is BreakpointSteps.SetSpeed -> advanceOverSpeedChanges(steps, speed)
    is BreakpointSteps.BreakBefore -> {
        if (steps.matcher.matches(event)) {
            val advanced = advanceOverSpeedChanges(steps.next, speed)
            advanced.copy(breakType = currentBreakType.combine(BreakType.BEFORE))
        } else {
            ProgressState(steps, currentBreakType, speed, false)
        }
    }
    is BreakpointSteps.BreakAfter -> {
        if (steps.matcher.matches(event)) {
            val advanced = advanceOverSpeedChanges(steps.next, speed)
            advanced.copy(breakType = currentBreakType.combine(BreakType.AFTER))
        } else {
            ProgressState(steps, currentBreakType, speed, false)
        }
    }
}

fun progressAutomaton(
    automaton: BreakpointAutomaton,
    event: CallStackTrackEvent,
    isResumed: Boolean,
    currentSpeed: Int?,
): BreakpointProgression {
    // Check BreakBefore
    val afterBefore = checkBreakpoint(automaton.steps, event, BreakType.NONE, currentSpeed)

    // Check BreakAfter (only if BreakBefore didn't match or both can match)
    val afterBoth = checkBreakpoint(afterBefore.steps, event, afterBefore.breakType, afterBefore.speed)

    val hasBreakpoint = afterBoth.breakType != BreakType.NONE
    val nextDebuggerState = if (hasBreakpoint) {
        DebuggerState.Paused
    } else {
        debuggerStateFromSpeed(afterBoth.speed, isResumed)
    }

    return BreakpointProgression(
        nextAutomaton = BreakpointAutomaton(afterBoth.steps),
        nextDebuggerState = nextDebuggerState,
        breakType = afterBoth.breakType,
        newSpeed = if (afterBoth.speedChanged) afterBoth.speed else null
    )
}

fun createAutomaton(program: BreakpointProgram): Pair<BreakpointAutomaton, Int?> {
    val advanced = advanceOverSpeedChanges(program.steps, null)
    return BreakpointAutomaton(advanced.steps) to advanced.speed
}
