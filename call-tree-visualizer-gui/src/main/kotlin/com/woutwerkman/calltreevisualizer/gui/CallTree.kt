@file:OptIn(ExperimentalTime::class)

package com.woutwerkman.calltreevisualizer.gui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woutwerkman.calltreevisualizer.call_tree_visualizer_gui.generated.resources.Explosion_dark_theme
import com.woutwerkman.calltreevisualizer.call_tree_visualizer_gui.generated.resources.Explosion_light_theme
import com.woutwerkman.calltreevisualizer.call_tree_visualizer_gui.generated.resources.Res
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallTreeNode
import com.woutwerkman.calltreevisualizer.coroutineintegration.trackingCallStacks
import com.woutwerkman.calltreevisualizer.runGlobalScopeTracker
import kotlinx.collections.immutable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class CallTree(val nodes: PersistentMap<Int, Node>, val roots: PersistentList<Int>) {
    data class Node(val id: Int, val type: Type, val childIds: PersistentList<Int>) {
        sealed class Type {
            data class Normal(val name: String) : Type()
            data class ThrewException(val parentId: Int?, val wasCancellation: Boolean) : Type()
        }
    }
}


@OptIn(ExperimentalTime::class)
@Composable
fun CallTreeUI(config: Flow<Config>, manualStepSignals: Flow<Unit>, program: suspend () -> Unit) {
    var tree by remember { mutableStateOf(CallTree(nodes = persistentMapOf(), roots = persistentListOf())) }

    LaunchedEffect(program) {
        withContext(Dispatchers.Default) {
            var lastEmission = Clock.System.now()
            trackingCallStacks {
                val job = launch { runGlobalScopeTracker(it) }
                program()
                job.cancelAndJoin()
            }.collect { (node, event) ->
                val newTree = when (event) {
                    is CallStackTrackEventType.CallStackThrowType -> tree.addThrownException(node, wasCancellation = false)
                    is CallStackTrackEventType.CallStackCancelled -> tree.addThrownException(node, wasCancellation = true)
                    is CallStackTrackEventType.CallStackPopType -> tree.removeNode(node.id, node.parent?.id)
                    is CallStackTrackEventType.CallStackPushType -> when (val parent = node.parent) {
                        null -> tree.copy(
                            nodes = tree.nodes.put(node.id, CallTree.Node(
                                id = node.id,
                                type = CallTree.Node.Type.Normal(node.functionFqn),
                                childIds = persistentListOf(),
                            )),
                            roots = tree.roots.add(node.id),
                        )
                        else -> {
                            val childIdsToCut = tree.nodes[parent.id]?.childIds?.filter { tree.nodes[it]?.type !is CallTree.Node.Type.Normal }
                            val treeAfterExceptionThrowsRemovedByNewChildAddition = if (childIdsToCut.isNullOrEmpty()) {
                                tree
                            } else {
                                tree.copy(
                                    nodes = tree
                                        .nodes
                                        .removeAll(tree.allChildIdsRecursivelyStartingFrom(rootIds = childIdsToCut))
                                        .update(parent.id) { parentNode -> parentNode!!.copy(childIds = parentNode.childIds.removeAll(childIdsToCut)) },
                                )
                            }
                            treeAfterExceptionThrowsRemovedByNewChildAddition.copy(
                                nodes = treeAfterExceptionThrowsRemovedByNewChildAddition
                                    .nodes
                                    .put(node.id, CallTree.Node(
                                        id = node.id,
                                        type = CallTree.Node.Type.Normal(node.functionFqn),
                                        childIds = persistentListOf(),
                                    ))
                                    .update(parent.id) { parentNode ->
                                        parentNode!!.copy(
                                            childIds = parentNode.childIds.add(node.id)
                                        )
                                    }
                                )
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    tree = newTree
                }
                config.waitUntilItsTimeForNextElementGivenThatLastElementWasProcessedAt(lastEmission, manualStepSignals)
                lastEmission = Clock.System.now()
            }
        }
    }
    CallTreeUI(tree, onExplosionDone = { childNodeId, parentNodeId ->
        tree = tree.removeNode(childNodeId, parentNodeId)
    })
}

private fun <K, V> PersistentMap<K, V>.removeAll(keysToRemove: List<K>): PersistentMap<K, V> = mutate { builder ->
    keysToRemove.forEach { key ->
        builder.remove(key)
    }
}

private fun CallTree.allChildIdsRecursivelyStartingFrom(rootIds: List<Int>): List<Int> = buildList {
    fun Int.visit() {
        add(this)
        nodes[this]?.childIds?.forEach { it.visit() }
    }
    rootIds.forEach { it.visit() }
}

@Composable
fun CallTreeUI(programState: CallTree, onExplosionDone: (childNodeId: Int, parentNodeId: Int?) -> Unit) {
    @Composable
    fun DrawNode(node: CallTree.Node) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
            ) {
                generateSequence(node) { it.childIds.singleOrNull()?.let { programState.nodes[it] } }
                    .last()
                    .childIds
                    .forEach { childId ->
                        DrawNode(programState.nodes[childId]!!)
                    }
            }
            Column(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.5.dp, MaterialTheme.colors.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colors.surface),
                verticalArrangement = Arrangement.Bottom,
            ) {
                generateSequence(node) { it.childIds.singleOrNull()?.let { programState.nodes[it] } }
                    .toList()
                    .asReversed()
                    .forEach { node ->
                        if (node.childIds.size == 1) {
                            Box(modifier = Modifier.background(MaterialTheme.colors.onSurface.copy(alpha = 0.1f)).size(140.dp, 1.dp))
                        }

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .width(132.dp)
                                .height(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            when (node.type) {
                                is CallTree.Node.Type.Normal -> Text(
                                    text = node.type.name.substringAfterLast("."),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.body1,
                                    color = MaterialTheme.colors.onSurface
                                )
                                is CallTree.Node.Type.ThrewException if node.type.wasCancellation -> FullSizeDiagonalRedCross()
                                is CallTree.Node.Type.ThrewException -> Explosion()
                            }
                        }
                    }
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
    ) {
        for (root in programState.roots) {
            DrawNode(programState.nodes[root]!!)
            Spacer(modifier = Modifier.width(10.dp))
        }
    }
}

@Composable
private fun Explosion() {
    val drawable = if (MaterialTheme.colors.isLight) {
        Res.drawable.Explosion_light_theme
    } else {
        Res.drawable.Explosion_dark_theme
    }
    Image(
        painter = painterResource(drawable),
        contentDescription = "Explosion",
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun FullSizeDiagonalRedCross() {
    val color = MaterialTheme.colors.error
    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height),
            strokeWidth = 3f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width, 0f),
            end = androidx.compose.ui.geometry.Offset(0f, size.height),
            strokeWidth = 3f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

private fun CallTree.addThrownException(node: CallTreeNode, wasCancellation: Boolean): CallTree = copy(
    nodes = nodes.update(node.id) {
        it!!.copy(type = CallTree.Node.Type.ThrewException(node.parent?.id, wasCancellation))
    },
)

private fun CallTree.removeNode(childNodeId: Int, parentNodeId: Int?): CallTree = when (parentNodeId) {
    null -> copy(roots = roots.remove(childNodeId))
    else -> copy(
        nodes = nodes
            .remove(childNodeId)
            .update(parentNodeId) { it!!.copy(childIds = it.childIds.remove(childNodeId)) },
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun Flow<Config>.waitUntilItsTimeForNextElementGivenThatLastElementWasProcessedAt(
    moment: Instant,
    manualStepSignals: Flow<Unit>
) {
    mapLatest { config ->
        config.speed?.let { speed ->
            if (speed == 0) {
                manualStepSignals.first()
            } else {
                val timeSinceLastElement = Clock.System.now() - moment
                val delayTime = 1.seconds / speed - timeSinceLastElement
                if (delayTime.isPositive()) {
                    coroutineScope {
                        val timeoutJob = launch { delay(delayTime) }
                        val manualStepJob = launch { manualStepSignals.first() }
                        select<Unit> {
                            timeoutJob.onJoin { manualStepJob.cancel() }
                            manualStepJob.onJoin { timeoutJob.cancel() }
                        }
                    }
                }
            }
        }
    }.first()
}

private fun <K, V> PersistentMap<K, V>.update(key: K, updater: (V?) -> V): PersistentMap<K, V> =
    put(key, updater(this[key]))
