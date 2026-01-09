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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woutwerkman.calltreevisualizer.call_tree_visualizer_gui.generated.resources.Explosion_dark_theme
import com.woutwerkman.calltreevisualizer.call_tree_visualizer_gui.generated.resources.Explosion_light_theme
import com.woutwerkman.calltreevisualizer.call_tree_visualizer_gui.generated.resources.Res
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEvent
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallTreeNode
import com.woutwerkman.calltreevisualizer.coroutineintegration.trackingCallStacks
import com.woutwerkman.calltreevisualizer.runGlobalScopeTracker
import kotlinx.collections.immutable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.painterResource
import kotlin.time.ExperimentalTime

data class CallTree(val nodes: PersistentMap<Int, Node>, val roots: PersistentList<Int>) {
    data class Node(val id: Int, val type: Type, val childIds: PersistentList<Int>) {
        sealed class Type {
            data class Normal(val name: String) : Type()
            data class ThrewException(val parentId: Int?, val wasCancellation: Boolean) : Type()
        }
    }
}

fun CallTree.treeAfter(event: CallStackTrackEvent): CallTree {
    val (node, eventType) = event
    return when (eventType) {
        is CallStackTrackEventType.CallStackThrowType -> addThrownException(node, wasCancellation = false)
        is CallStackTrackEventType.CallStackCancelled -> addThrownException(node, wasCancellation = true)
        is CallStackTrackEventType.CallStackPopType -> removeNode(node.id, node.parent?.id)
        is CallStackTrackEventType.CallStackPushType -> when (val parent = node.parent) {
            null -> copy(
                nodes = nodes.put(node.id, CallTree.Node(
                    id = node.id,
                    type = CallTree.Node.Type.Normal(node.functionFqn),
                    childIds = persistentListOf(),
                )),
                roots = roots.add(node.id),
            )
            else -> {
                val childIdsToCut = nodes[parent.id]?.childIds?.filter { nodes[it]?.type !is CallTree.Node.Type.Normal }
                val treeAfterCuts = if (childIdsToCut.isNullOrEmpty()) {
                    this
                } else {
                    copy(
                        nodes = nodes
                            .removeAll(allChildIdsRecursivelyStartingFrom(rootIds = childIdsToCut))
                            .update(parent.id) { parentNode -> parentNode!!.copy(childIds = parentNode.childIds.removeAll(childIdsToCut)) },
                    )
                }
                treeAfterCuts.copy(
                    nodes = treeAfterCuts.nodes
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
}

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
@Composable
fun CallTreeUI(
    config: Flow<Config>,
    stepSignals: Flow<StepSignal>,
    breakpointProgram: BreakpointProgram,
    onConfigChange: (Config) -> Unit,
    program: suspend () -> Unit
) {
    val viewModel = remember(program, breakpointProgram) {
        CallTreeViewModel(
            config = config,
            stepSignals = stepSignals,
            breakpointProgram = breakpointProgram,
            onConfigChange = onConfigChange,
            events = trackingCallStacks {
                val job = launch { runGlobalScopeTracker(it) }
                program()
                job.cancelAndJoin()
            }
        )
    }

    val tree by viewModel.tree.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.run()
    }

    CallTreeUI(tree)
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
                val lastNodeWithChildren = generateSequence(node) { it.childIds.singleOrNull()?.let { programState.nodes[it] } }
                    .last()
                val children = lastNodeWithChildren.childIds.map { programState.nodes[it]!! }

                if (children.isNotEmpty()) {
                    var childCoordinates by remember(children) { mutableStateOf(persistentMapOf<Int, androidx.compose.ui.layout.LayoutCoordinates>()) }
                    Column(
                        modifier = Modifier.width(IntrinsicSize.Max),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        var columnCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            children.forEach { child ->
                                Box(
                                    modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
                                        childCoordinates = childCoordinates.put(child.id, layoutCoordinates)
                                    }
                                ) {
                                    DrawNode(child)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().height(24.dp).onGloballyPositioned { columnCoordinates = it }
                        ) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val colCoords = columnCoordinates
                                if (colCoords == null || !colCoords.isAttached) return@Canvas

                                val color = Color.Gray.copy(alpha = 0.5f)
                                val strokeWidth = 2.dp.toPx()
                                val midY = size.height / 2

                                // Vertical line to parent
                                drawLine(
                                    color = color,
                                    start = androidx.compose.ui.geometry.Offset(size.width / 2, size.height),
                                    end = androidx.compose.ui.geometry.Offset(size.width / 2, midY),
                                    strokeWidth = strokeWidth
                                )

                                val currentChildXPositions = children.mapNotNull { child ->
                                    val coords = childCoordinates[child.id]
                                    if (coords != null && coords.isAttached) {
                                        child.id to colCoords.localPositionOf(coords, androidx.compose.ui.geometry.Offset.Zero).x + coords.size.width / 2f
                                    } else null
                                }.toMap()

                                if (currentChildXPositions.isNotEmpty()) {
                                    val minX = currentChildXPositions.values.minOrNull() ?: 0f
                                    val maxX = currentChildXPositions.values.maxOrNull() ?: 0f

                                    // Horizontal line connecting all children
                                    if (children.size > 1 && currentChildXPositions.size == children.size) {
                                        drawLine(
                                            color = color,
                                            start = androidx.compose.ui.geometry.Offset(minX, midY),
                                            end = androidx.compose.ui.geometry.Offset(maxX, midY),
                                            strokeWidth = strokeWidth
                                        )
                                    }

                                    // Vertical lines to each child
                                    children.forEach { child ->
                                        currentChildXPositions[child.id]?.let { x ->
                                            drawLine(
                                                color = color,
                                                start = androidx.compose.ui.geometry.Offset(x, midY),
                                                end = androidx.compose.ui.geometry.Offset(x, 0f),
                                                strokeWidth = strokeWidth
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // No children, nothing to draw above this node
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


private fun <K, V> PersistentMap<K, V>.update(key: K, updater: (V?) -> V): PersistentMap<K, V> =
    put(key, updater(this[key]))
