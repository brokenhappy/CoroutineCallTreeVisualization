@file:OptIn(ExperimentalTime::class)

package com.woutwerkman.calltreevisualizer.gui

import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEvent
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class CallTreeViewModel(
    private val config: Flow<Config>,
    private val stepSignals: Flow<StepSignal>,
    private val breakpointProgram: BreakpointProgram,
    private val onConfigChange: (Config) -> Unit,
    private val events: Flow<CallStackTrackEvent>,
    private val clock: Clock = Clock.System
) {
    private val _tree = MutableStateFlow(CallTree.Empty)
    val tree: StateFlow<CallTree> = _tree.asStateFlow()

    private val _executionControl = MutableStateFlow<ExecutionControl>(ExecutionControl.Paused)
    val executionControl: StateFlow<ExecutionControl> = _executionControl.asStateFlow()

    suspend fun run() {
        val (initialAutomaton, initialSpeed) = createAutomaton(breakpointProgram)
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
                    _executionControl.value = when (signal) {
                        StepSignal.Step -> ExecutionControl.WaitingForSingleStep
                        StepSignal.Resume -> ExecutionControl.Running
                    }
                }
            }

            events.collect { event ->
                val currentConfig = config.first()
                val result = progressAutomaton(automaton, event, currentConfig.speed)
                automaton = result.nextAutomaton

                // Pause before processing if breakpoint matched
                if (result.shouldPauseBefore) {
                    _executionControl.value = ExecutionControl.Paused
                    _executionControl.waitForResume(lastEmission, config, clock)
                }

                _tree.value = _tree.value.after(event)

                // Update speed if changed
                result.newSpeed?.let { newSpeed ->
                    val currentConf = config.first()
                    if (currentConf.speed != newSpeed) {
                        onConfigChange(currentConf.copy(speed = newSpeed))
                    }
                }

                // Pause after processing if breakpoint matched
                if (result.shouldPauseAfter) {
                    _executionControl.value = ExecutionControl.Paused
                }

                // Convert WaitingForSingleStep to Paused after processing one event
                if (_executionControl.value == ExecutionControl.WaitingForSingleStep) {
                    _executionControl.value = ExecutionControl.Paused
                }

                // Wait for rate limiting or resume signal
                _executionControl.waitForResume(lastEmission, config, clock)
                lastEmission = clock.now()
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun Flow<ExecutionControl>.waitForResume(
    lastProcessedAt: Instant,
    configFlow: Flow<Config>,
    clock: Clock
) {
    combine(configFlow) { control, config -> control to config.speed }
        .distinctUntilChanged()
        .mapLatest { (control, speed) ->
            when (control) {
                ExecutionControl.WaitingForSingleStep,
                ExecutionControl.Running -> {
                    // Apply rate limiting if speed is set
                    speed?.let { eventsPerSecond ->
                        if (eventsPerSecond > 0) {
                            val timeSinceLastElement = clock.now() - lastProcessedAt
                            val delayTime = 1.seconds / eventsPerSecond - timeSinceLastElement
                            if (delayTime.isPositive()) delay(delayTime)
                        } else {
                            awaitCancellation()
                        }
                    }
                }
                ExecutionControl.Paused -> awaitCancellation()
            }
        }.first()
}
