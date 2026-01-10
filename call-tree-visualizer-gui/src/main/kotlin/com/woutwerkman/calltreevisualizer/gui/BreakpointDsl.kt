package com.woutwerkman.calltreevisualizer.gui

import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEvent
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType
import kotlin.reflect.*
import kotlin.reflect.jvm.javaMethod

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

sealed class ExecutionControl {
    data object Running : ExecutionControl()
    data object Paused : ExecutionControl()
    data object WaitingForSingleStep : ExecutionControl()
}

fun ExecutionControl.isRunning(): Boolean = this is ExecutionControl.Running

data class AutomatonResult(
    val nextAutomaton: BreakpointAutomaton,
    val shouldPauseBefore: Boolean,
    val shouldPauseAfter: Boolean,
    val newSpeed: Int?
)

data class BreakpointAutomaton(val steps: BreakpointSteps)

private data class MatchResult(
    val remainingSteps: BreakpointSteps,
    val newSpeed: Int?,
    val matched: Boolean
)

private tailrec fun consumeSpeedChanges(steps: BreakpointSteps, currentSpeed: Int?): Pair<BreakpointSteps, Int?> =
    when (steps) {
        is BreakpointSteps.SetSpeed -> consumeSpeedChanges(steps.next, steps.eventsPerSecond)
        else -> steps to currentSpeed
    }

private fun tryMatchBreakpointBefore(
    steps: BreakpointSteps,
    event: CallStackTrackEvent,
    currentSpeed: Int?
): MatchResult = when (steps) {
    BreakpointSteps.Empty -> MatchResult(steps, currentSpeed, matched = false)
    is BreakpointSteps.SetSpeed -> {
        val (remaining, speed) = consumeSpeedChanges(steps, currentSpeed)
        MatchResult(remaining, speed, matched = false)
    }
    is BreakpointSteps.BreakBefore -> {
        if (steps.matcher.matches(event)) {
            val (remaining, speed) = consumeSpeedChanges(steps.next, currentSpeed)
            MatchResult(remaining, speed, matched = true)
        } else {
            MatchResult(steps, currentSpeed, matched = false)
        }
    }
    is BreakpointSteps.BreakAfter -> MatchResult(steps, currentSpeed, matched = false)
}

private fun tryMatchBreakpointAfter(
    steps: BreakpointSteps,
    event: CallStackTrackEvent,
    currentSpeed: Int?
): MatchResult = when (steps) {
    BreakpointSteps.Empty -> MatchResult(steps, currentSpeed, matched = false)
    is BreakpointSteps.SetSpeed -> {
        val (remaining, speed) = consumeSpeedChanges(steps, currentSpeed)
        MatchResult(remaining, speed, matched = false)
    }
    is BreakpointSteps.BreakBefore -> MatchResult(steps, currentSpeed, matched = false)
    is BreakpointSteps.BreakAfter -> {
        if (steps.matcher.matches(event)) {
            val (remaining, speed) = consumeSpeedChanges(steps.next, currentSpeed)
            MatchResult(remaining, speed, matched = true)
        } else {
            MatchResult(steps, currentSpeed, matched = false)
        }
    }
}

fun progressAutomaton(
    automaton: BreakpointAutomaton,
    event: CallStackTrackEvent,
    currentSpeed: Int?
): AutomatonResult {
    val beforeResult = tryMatchBreakpointBefore(automaton.steps, event, currentSpeed)

    val afterResult = tryMatchBreakpointAfter(beforeResult.remainingSteps, event, beforeResult.newSpeed)

    return AutomatonResult(
        nextAutomaton = BreakpointAutomaton(afterResult.remainingSteps),
        shouldPauseBefore = beforeResult.matched,
        shouldPauseAfter = afterResult.matched,
        newSpeed = if (afterResult.newSpeed != currentSpeed) afterResult.newSpeed else null
    )
}

fun createAutomaton(program: BreakpointProgram): Pair<BreakpointAutomaton, Int?> {
    val (steps, speed) = consumeSpeedChanges(program.steps, null)
    return BreakpointAutomaton(steps) to speed
}
