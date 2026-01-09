@file:OptIn(ExperimentalTime::class)

package com.woutwerkman.calltreevisualizer.gui

import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEvent
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CallTreeViewModel(
    private val config: Flow<Config>,
    private val stepSignals: Flow<StepSignal>,
    private val breakpointProgram: BreakpointProgram,
    private val onConfigChange: (Config) -> Unit,
    private val events: Flow<CallStackTrackEvent>,
    private val clock: Clock = Clock.System
) {
    private val _tree = MutableStateFlow(CallTree(nodes = persistentMapOf(), roots = persistentListOf()))
    val tree: StateFlow<CallTree> = _tree.asStateFlow()

    private val _debuggerState = MutableStateFlow<DebuggerState>(DebuggerState.Paused)
    val debuggerState: StateFlow<DebuggerState> = _debuggerState.asStateFlow()

    suspend fun run() {
        val (initialAutomaton, initialSpeed) = BreakpointAutomaton.create(breakpointProgram)
        var automaton = initialAutomaton
        initialSpeed?.let { speed ->
            val currentConf = config.first()
            if (currentConf.speed != speed) {
                onConfigChange(currentConf.copy(speed = speed))
            }
        }
        var lastEmission = clock.now()
        
        coroutineScope {
            launch {
                stepSignals.collect { signal ->
                    val currentConfig = config.first()
                    _debuggerState.value = when (signal) {
                        StepSignal.Step -> DebuggerState.WaitingForSingleStep
                        StepSignal.Resume -> DebuggerState.fromSpeed(currentConfig.speed, isResumed = true)
                    }
                }
            }

            events.collect { event ->
                val currentConfig = config.first()
                val currentIsResumed = _debuggerState.value !is DebuggerState.Paused && _debuggerState.value !is DebuggerState.WaitingForSingleStep
                val progression = automaton.progress(event, currentIsResumed, currentConfig.speed)
                automaton = progression.nextAutomaton

                if (progression.breakType == BreakType.BEFORE || progression.breakType == BreakType.BOTH) {
                    _debuggerState.value = DebuggerState.Paused
                    _debuggerState.waitUntilItsTimeForNextElementGivenThatLastElementWasProcessedAt(lastEmission, clock)
                    if (_debuggerState.value == DebuggerState.WaitingForSingleStep) {
                        _debuggerState.value = DebuggerState.Paused
                    }
                }

                _tree.value = _tree.value.updateWithEvent(event)
                
                progression.newSpeed?.let { newSpeed ->
                    val currentConf = config.first()
                    if (currentConf.speed != newSpeed) {
                        onConfigChange(currentConf.copy(speed = newSpeed))
                    }
                }

                if (progression.breakType == BreakType.AFTER || progression.breakType == BreakType.BOTH) {
                    _debuggerState.value = DebuggerState.Paused
                } else {
                    val latestConfig = config.first()
                    val isResumedNow = _debuggerState.value !is DebuggerState.Paused && _debuggerState.value !is DebuggerState.WaitingForSingleStep
                    _debuggerState.value = DebuggerState.fromSpeed(latestConfig.speed, isResumedNow)
                }

                _debuggerState.waitUntilItsTimeForNextElementGivenThatLastElementWasProcessedAt(lastEmission, clock)
                if (_debuggerState.value == DebuggerState.WaitingForSingleStep) {
                    _debuggerState.value = DebuggerState.Paused
                }
                lastEmission = clock.now()
            }
        }
    }
}

private suspend fun Flow<DebuggerState>.waitUntilItsTimeForNextElementGivenThatLastElementWasProcessedAt(
    moment: Instant,
    clock: Clock
) {
    mapLatest { currentState ->
        when (currentState) {
            is DebuggerState.WaitingForSingleStep,
            is DebuggerState.Unrestrained -> { /* Do nothing */ }
            is DebuggerState.RunningAtLimitedSpeed -> {
                val speed = currentState.eventsPerSecond
                if (speed <= 0) kotlinx.coroutines.awaitCancellation()

                val timeSinceLastElement = clock.now() - moment
                val delayTime = 1.seconds / speed - timeSinceLastElement

                if (delayTime.isPositive()) kotlinx.coroutines.delay(delayTime)
            }
            is DebuggerState.Paused -> kotlinx.coroutines.awaitCancellation()
        }
    }.first()
}
