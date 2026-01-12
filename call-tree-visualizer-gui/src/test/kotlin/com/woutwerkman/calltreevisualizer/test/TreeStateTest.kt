@file:OptIn(ExperimentalTime::class)

package com.woutwerkman.calltreevisualizer.test

import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEvent
import com.woutwerkman.calltreevisualizer.gui.*
import com.woutwerkman.calltreevisualizer.coroutineintegration.trackingCallStacks
import com.woutwerkman.calltreevisualizer.owningGlobalScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.TimeSource
import kotlin.time.measureTime

class TreeStateTest {
    @Test
    fun `breakBefore plus step equals breakAfter`() = runTest(timeout = 2.seconds) {
        val fqn = "com.woutwerkman.calltreevisualizer.test.foo"

        // Break Before
        val beforeTree = treeAfterDebuggerProgramOrNullIfProgramFinished(
            program = { simpleCall() },
            debuggerProgram = breakBefore(functionCall(fqn)),
            interactions = listOf(Interaction.Resume)
        )!!

        // Break After
        val afterTree = treeAfterDebuggerProgramOrNullIfProgramFinished(
            program = { simpleCall() },
            debuggerProgram = breakAfter(functionCall(fqn)),
            interactions = listOf(Interaction.Resume)
        )!!

        // Before + F8 should equal After
        val beforePlusStepTree = treeAfterDebuggerProgramOrNullIfProgramFinished(
            program = { simpleCall() },
            debuggerProgram = breakBefore(functionCall(fqn)),
            interactions = listOf(Interaction.Resume, Interaction.Step)
        )!!

        assertEquals(afterTree, beforePlusStepTree)
        assertTrue(afterTree.nodes.size > beforeTree.nodes.size, "After tree should have more nodes than before tree")
    }

    @Test
    fun `breakBefore and breakAfter combined pauses before adding node`() = runTest(timeout = 2.seconds) {
        val fqn = "com.woutwerkman.calltreevisualizer.test.foo"

        val tree = treeAfterDebuggerProgramOrNullIfProgramFinished(
            program = { simpleCall() },
            debuggerProgram = breakBefore(functionCall(fqn)).then(breakAfter(functionCall(fqn))),
            interactions = listOf(Interaction.Resume)
        )!!

        val fooNode = tree.nodes.values.find { (it.type as? CallTree.Node.Type.Normal)?.name == fqn }
        assertTrue(fooNode == null, "Should have paused before adding 'foo'")
    }

    @Test
    fun `breakAtNextStep pauses before first step with empty tree`() = runTest(timeout = 2.seconds) {
        val tree = treeAfterDebuggerProgramOrNullIfProgramFinished(
            program = { simpleCall() },
            debuggerProgram = breakAtNextStep(),
            interactions = listOf(Interaction.Resume)
        )!!

        assertTrue(tree.nodes.isEmpty(), "Tree should be empty when breaking before the next step (which is the first step)")
    }

    @Test
    fun `branching call creates multiple child nodes`() = runTest(timeout = 2.seconds) {
        val rootFqn = "com.woutwerkman.calltreevisualizer.test.persistentBranchingCall"
        val foobsFqn = "com.woutwerkman.calltreevisualizer.test.foobsForever"

        // Wait until first foobs is called (break AFTER)
        val finalTree = treeAfterDebuggerProgramOrNullIfProgramFinished(
            program = { persistentBranchingCall() },
            debuggerProgram = breakAfter(functionCall(foobsFqn)),
            interactions = listOf(Interaction.Resume)
        )!!

        val rootId = finalTree.roots.single()
        val rootNode = finalTree.nodes[rootId]!!
        assertEquals(rootFqn, (rootNode.type as CallTree.Node.Type.Normal).name)

        // At least one foobs should be there
        val children = rootNode.childIds.map { finalTree.nodes[it]!! }
        assertTrue(children.any { (it.type as? CallTree.Node.Type.Normal)?.name == foobsFqn }, "At least one foobs should be there")
    }

    @Test
    fun `function node changes type to ThrewException after throwing`() = runTest(timeout = 2.seconds) {
        val fqn = "com.woutwerkman.calltreevisualizer.test.throwingCall"

        // Pause when it's about to throw
        val treeBefore = treeAfterDebuggerProgramOrNullIfProgramFinished(
            program = { try { throwingCall() } catch (e: Exception) {} },
            debuggerProgram = breakBefore(functionThrows(fqn)),
            interactions = listOf(Interaction.Resume)
        )!!

        val throwingNodeBefore = treeBefore.nodes.values.find { (it.type as? CallTree.Node.Type.Normal)?.name == fqn }
        assertTrue(throwingNodeBefore != null, "Node should exist")
        assertTrue(throwingNodeBefore.type is CallTree.Node.Type.Normal, "Should still be Normal")

        // Pause after it has thrown
        val treeAfter = treeAfterDebuggerProgramOrNullIfProgramFinished(
            program = { try { throwingCall() } catch (e: Exception) {} },
            debuggerProgram = breakAfter(functionThrows(fqn)),
            interactions = listOf(Interaction.Resume)
        )!!

        val throwingNodeAfter = treeAfter.nodes[throwingNodeBefore.id]!!
        assertTrue(throwingNodeAfter.type is CallTree.Node.Type.ThrewException, "Should be ThrewException now")
    }

    @Test
    @Ignore
    fun `cancelled function is marked with wasCancellation flag`() = runTest(timeout = 2.seconds) {
        val fqn = "com.woutwerkman.calltreevisualizer.test.foobsForever"

        val treeAfter = treeAfterDebuggerProgramOrNullIfProgramFinished(
            program = { cancellingCall() },
            debuggerProgram = breakAfter(functionCancels(fqn)),
            interactions = listOf(Interaction.Resume)
        )!!

        val cancelledNode = treeAfter.nodes.values.find { (it.type as? CallTree.Node.Type.ThrewException)?.wasCancellation == true }
        assertTrue(cancelledNode != null, "Should find a cancelled node")
    }

    @Test
    fun `changeSpeed triggers config change exactly once`() = runTest(timeout = 2.seconds) {
        var configChangeCount = 0

        val config = MutableStateFlow(Config())
        val stepSignals = MutableSharedFlow<StepSignal>(replay = 1)

        val viewModel = CallTreeViewModel(
            config = config,
            stepSignals = stepSignals,
            breakpointProgram = changeSpeed(10)
                .then(breakAfter(functionCall("com.woutwerkman.calltreevisualizer.test.foo"))),
            onConfigChange = {
                configChangeCount++
                config.value = it
            },
            events = trackingCallStacks {
                simpleCall() // calls foo, bar, baz
            },
            clock = TestClock(testScheduler.timeSource)
        )

        coroutineScope {
            val job = launch { viewModel.run() }
            stepSignals.emit(StepSignal.Resume)

            // Wait until it resumes
            viewModel.executionControl.first { it !is ExecutionControl.Paused }
            // Wait until it hits the breakpoint after 'foo'
            viewModel.executionControl.first { it is ExecutionControl.Paused }

            assertEquals(1, configChangeCount, "onConfigChange should be called exactly once for changeSpeed(10), but was called $configChangeCount times")

            job.cancelAndJoin()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `the time spend in the program does not affect time between events`() = runTest(timeout = 2.seconds) {
        val timeTaken = testTimeSource.measureTime {
            val result = treeAfterDebuggerProgramOrNullIfProgramFinished(
                debuggerProgram = changeSpeed(1)
                    .then(breakAfter(functionCall("function that never gets called"))),
                interactions = listOf(Interaction.Resume),
                program = {
                    trackedDelay(.5.seconds)
                },
            )
            assertNull(result)
        }
        assertEquals(timeTaken, 2.seconds)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `the time spend processing events does not affect time waited between events`() = runTest(timeout = 2.seconds) {
        val timeTaken = testTimeSource.measureTime {
            val result = treeAfterDebuggerProgramOrNullIfProgramFinished(
                debuggerProgram = changeSpeed(1)
                    .then(breakAfter(functionCall("function that never gets called"))),
                interactions = listOf(Interaction.Resume),
                events = trackingCallStacks {
                    owningGlobalScope {
                        bar()
                    }
                }.onEach {
                    delay(.5.seconds) // Simulate processing time of events
                },
            )
            assertNull(result)
        }
        assertEquals(timeTaken, 2.seconds)
    }
}

private class TestClock(private val timeSource: TimeSource.WithComparableMarks) : Clock {
    private val startMark = timeSource.markNow()
    override fun now() =
        Instant.fromEpochMilliseconds((timeSource.markNow() - startMark).inWholeMilliseconds)
}

private sealed interface Interaction {
    data object Step : Interaction
    data object Resume : Interaction
}

private suspend fun TestScope.treeAfterDebuggerProgramOrNullIfProgramFinished(
    program: suspend () -> Unit,
    debuggerProgram: BreakpointProgram,
    interactions: List<Interaction>
): CallTree? = treeAfterDebuggerProgramOrNullIfProgramFinished(
    events = trackingCallStacks {
        owningGlobalScope {
            program()
        }
    },
    debuggerProgram,
    interactions,
)

private suspend fun TestScope.treeAfterDebuggerProgramOrNullIfProgramFinished(
    events: Flow<CallStackTrackEvent>,
    debuggerProgram: BreakpointProgram,
    interactions: List<Interaction>
): CallTree? = coroutineScope {
    val config = MutableStateFlow(Config())
    val stepSignals = MutableSharedFlow<StepSignal>(replay = 1)
    val viewModel = CallTreeViewModel(
        config = config,
        stepSignals = stepSignals,
        breakpointProgram = debuggerProgram,
        onConfigChange = { config.value = it },
        events = events,
        clock = TestClock(testScheduler.timeSource),
    )

    race(
        {
            viewModel.run()
            null /* Program finished */
        },
        {
            for (interaction in interactions) {
                when (interaction) {
                    Interaction.Step -> stepSignals.emit(StepSignal.Step)
                    Interaction.Resume -> stepSignals.emit(StepSignal.Resume)
                }

                // Wait for the interaction to be processed and for the state to settle
                if (interaction == Interaction.Step) {
                    viewModel.executionControl.first { it is ExecutionControl.WaitingForSingleStep }
                } else {
                    viewModel.executionControl.first { it !is ExecutionControl.Paused }
                }
                viewModel.executionControl.first { it is ExecutionControl.Paused }
            }

            viewModel.tree.value
        },
    )
}

suspend fun <T> race(vararg tasks: suspend CoroutineScope.() -> T) =
    channelFlow { tasks.forEach { launch { send(it()) } } }.first()