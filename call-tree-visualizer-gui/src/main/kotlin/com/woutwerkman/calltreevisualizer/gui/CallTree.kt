@file:OptIn(ExperimentalTime::class)

package com.woutwerkman.calltreevisualizer.gui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType
import com.woutwerkman.calltreevisualizer.coroutineintegration.trackingCallStacks
import com.woutwerkman.calltreevisualizer.runGlobalScopeTracker
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class CallTree(val nodes: PersistentMap<Int, Node>, val roots: PersistentList<Int>) {
    data class Node(val id: Int, val name: String, val childIds: PersistentList<Int>)
}



@OptIn(ExperimentalTime::class)
@Composable
fun CallTreeUI(config: Flow<Config>, program: suspend () -> Unit) {
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
                    CallStackTrackEventType.CallStackPopType -> when (val parent = node.parent) {
                        null -> tree.copy(roots = tree.roots.remove(node.id))
                        else -> tree.copy(
                            nodes = tree
                                .nodes
                                .remove(node.id)
                                .update(parent.id) { it!!.copy(childIds = it.childIds.remove(node.id)) },
                        )
                    }
                    is CallStackTrackEventType.CallStackThrowType,
                    is CallStackTrackEventType.CallStackPushType -> when (val parent = node.parent) {
                        null -> tree.copy(
                            nodes = tree.nodes.put(node.id, CallTree.Node(node.id, node.functionFqn, persistentListOf())),
                            roots = tree.roots.add(node.id),
                        )
                        else -> tree.copy(
                            nodes = tree
                                .nodes
                                .put(node.id, CallTree.Node(node.id, node.functionFqn, persistentListOf()))
                                .update(parent.id) { it!!.copy(childIds = it.childIds.add(node.id)) }
                        )
                    }
                }
                withContext(Dispatchers.Main) {
                    tree = newTree
                }
                config.waitUntilItsTimeForNextElementGivenThatLastElementWasProcessedAt(lastEmission)
                lastEmission = Clock.System.now()
            }
        }
    }
    CallTreeUI(tree)
}

@Composable
fun CallTreeUI(programState: CallTree) {
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
                generateSequence(node) { it.childIds.singleOrNull().let { programState.nodes[it] } }
                    .last()
                    .childIds
                    .forEach { childId ->
                        DrawNode(programState.nodes[childId]!!)
                    }
            }
            Column(
                modifier = Modifier
                    .padding(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, Color.Black, RoundedCornerShape(4.dp)),
                verticalArrangement = Arrangement.Bottom,
            ) {
                generateSequence(node) { it.childIds.singleOrNull().let { programState.nodes[it] } }
                    .toList()
                    .asReversed()
                    .forEach { node ->
                        if (node.childIds.size == 1) {
                            Box(modifier = Modifier.background(Color.Black).size(134.dp, 1.dp))
                        }
                        Box(
                            modifier = Modifier
                                .background(Color.LightGray)
                                .padding(horizontal = 5.dp, vertical = 0.dp)
                                .padding(2.dp)
                                .width(120.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = node.name.substringAfterLast("."),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
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

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun Flow<Config>.waitUntilItsTimeForNextElementGivenThatLastElementWasProcessedAt(moment: Instant) {
    mapLatest { config ->
        config.speed?.let { speed ->
            (Clock.System.now() - moment)
                .let { timeSinceLastElement -> 1.seconds / speed - timeSinceLastElement }
                .takeIf { it > 0.seconds }
                ?.let { delay(it) }
        }
    }.first()
}

private fun <K, V> PersistentMap<K, V>.update(key: K, updater: (V?) -> V): PersistentMap<K, V> =
    put(key, updater(this[key]))
