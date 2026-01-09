package com.woutwerkman.calltreevisualizer.gui

import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEvent
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType
import kotlin.reflect.*
import kotlin.reflect.jvm.javaMethod

sealed interface BreakpointEventMatcher {
    fun matches(event: CallStackTrackEvent): Boolean
}

data class FunctionCallMatcher(val fqn: String) : BreakpointEventMatcher {
    override fun matches(event: CallStackTrackEvent): Boolean {
        val type = event.eventType
        return type is CallStackTrackEventType.CallStackPushType && type.functionFqn == fqn
    }
}

data class FunctionThrowsMatcher(val fqn: String) : BreakpointEventMatcher {
    override fun matches(event: CallStackTrackEvent): Boolean {
        val type = event.eventType
        return type is CallStackTrackEventType.CallStackThrowType && event.node.functionFqn == fqn
    }
}

data class FunctionCancelsMatcher(val fqn: String) : BreakpointEventMatcher {
    override fun matches(event: CallStackTrackEvent): Boolean {
        val type = event.eventType
        return type is CallStackTrackEventType.CallStackCancelled && event.node.functionFqn == fqn
    }
}

object NextStepMatcher : BreakpointEventMatcher {
    override fun matches(event: CallStackTrackEvent): Boolean = true
}

fun functionCall(fqn: String) = FunctionCallMatcher(fqn)
fun functionThrows(fqn: String) = FunctionThrowsMatcher(fqn)
fun functionCancels(fqn: String) = FunctionCancelsMatcher(fqn)
fun functionCall(function: KFunction<Unit>) = FunctionCallMatcher(function.javaMethod!!.declaringClass.packageName + "." + function.name)
fun functionThrows(function: KFunction<Unit>) = FunctionThrowsMatcher(function.javaMethod!!.declaringClass.packageName + "." + function.name)
fun functionCancels(function: KFunction<Unit>) = FunctionCancelsMatcher(function.javaMethod!!.declaringClass.packageName + "." + function.name)

sealed interface BreakpointStep {
    data class SetSpeed(val eventsPerSecond: Int) : BreakpointStep
    data class BreakBefore(val matcher: BreakpointEventMatcher) : BreakpointStep
    data class BreakAfter(val matcher: BreakpointEventMatcher) : BreakpointStep
}

class BreakpointProgram(val steps: List<BreakpointStep>) {
    fun then(program: BreakpointProgram): BreakpointProgram {
        return BreakpointProgram(steps + program.steps)
    }
}

fun changeSpeed(eventsPerSecond: Int) = BreakpointProgram(listOf(BreakpointStep.SetSpeed(eventsPerSecond)))
val Int.eventsPerSecond get() = this

fun breakBeforeEvent(matcher: BreakpointEventMatcher) = BreakpointProgram(listOf(BreakpointStep.BreakBefore(matcher)))
fun breakAfterEvent(matcher: BreakpointEventMatcher) = BreakpointProgram(listOf(BreakpointStep.BreakAfter(matcher)))
fun breakAtNextStep() = BreakpointProgram(listOf(BreakpointStep.BreakBefore(NextStepMatcher)))

fun BreakpointStep.toProgram() = BreakpointProgram(listOf(this))

fun BreakpointProgram.then(step: BreakpointStep): BreakpointProgram = then(step.toProgram())

enum class StepSignal {
    Step, Resume
}

sealed class DebuggerState {
    data object Unrestrained : DebuggerState()
    data class RunningAtLimitedSpeed(val eventsPerSecond: Int) : DebuggerState()
    data object Paused : DebuggerState()
    data object WaitingForSingleStep : DebuggerState()

    companion object {
        fun fromSpeed(speed: Int?, isResumed: Boolean): DebuggerState {
            if (!isResumed) return Paused
            return if (speed == null || speed <= 0) Unrestrained else RunningAtLimitedSpeed(speed)
        }
    }
}

enum class BreakType {
    NONE, BEFORE, AFTER, BOTH
}

data class BreakpointProgression(
    val nextAutomaton: BreakpointAutomaton,
    val nextDebuggerState: DebuggerState,
    val breakType: BreakType,
    val newSpeed: Int? = null
)

data class BreakpointAutomaton(
    val steps: List<BreakpointStep>,
    val currentStepIndex: Int = 0
) {
    fun progress(event: CallStackTrackEvent, isResumed: Boolean, currentSpeed: Int?): BreakpointProgression {
        var automaton = this
        var breakBefore = false
        var breakAfter = false
        var newSpeed: Int? = currentSpeed
        var speedChanged = false

        fun advance() {
            while (automaton.currentStepIndex < automaton.steps.size) {
                val step = automaton.steps[automaton.currentStepIndex]
                if (step is BreakpointStep.SetSpeed) {
                    newSpeed = step.eventsPerSecond
                    speedChanged = true
                    automaton = automaton.copy(currentStepIndex = automaton.currentStepIndex + 1)
                } else {
                    break
                }
            }
        }

        // 1. Check for BreakBefore
        if (automaton.currentStepIndex < automaton.steps.size) {
            val step = automaton.steps[automaton.currentStepIndex]
            if (step is BreakpointStep.BreakBefore && step.matcher.matches(event)) {
                breakBefore = true
                automaton = automaton.copy(currentStepIndex = automaton.currentStepIndex + 1)
                advance()
            }
        }

        // 2. Check for BreakAfter
        if (automaton.currentStepIndex < automaton.steps.size) {
            val step = automaton.steps[automaton.currentStepIndex]
            if (step is BreakpointStep.BreakAfter && step.matcher.matches(event)) {
                breakAfter = true
                automaton = automaton.copy(currentStepIndex = automaton.currentStepIndex + 1)
                advance()
            }
        }

        val breakType = when {
            breakBefore && breakAfter -> BreakType.BOTH
            breakBefore -> BreakType.BEFORE
            breakAfter -> BreakType.AFTER
            else -> BreakType.NONE
        }

        val nextDebuggerState = if (breakBefore || breakAfter) {
            DebuggerState.Paused
        } else {
            DebuggerState.fromSpeed(newSpeed, isResumed)
        }

        return BreakpointProgression(automaton, nextDebuggerState, breakType, if (speedChanged) newSpeed else null)
    }

    companion object {
        fun create(program: BreakpointProgram): Pair<BreakpointAutomaton, Int?> {
            var automaton = BreakpointAutomaton(program.steps)
            var initialSpeed: Int? = null
            var index = 0
            while (index < automaton.steps.size) {
                val step = automaton.steps[index]
                if (step is BreakpointStep.SetSpeed) {
                    initialSpeed = step.eventsPerSecond
                    index++
                } else {
                    break
                }
            }
            return automaton.copy(currentStepIndex = index) to initialSpeed
        }
    }
}
