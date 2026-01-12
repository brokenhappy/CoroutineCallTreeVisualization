package com.woutwerkman.calltreevisualizer.gui

import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEvent
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallTreeEventNode
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

data class CallTree(val nodes: PersistentMap<Int, Node>, val roots: PersistentList<Int>) {
    companion object {
        val Empty: CallTree = CallTree(persistentMapOf(), persistentListOf())
    }

    data class Node(val id: Int, val type: Type, val childIds: PersistentList<Int>) {
        sealed class Type {
            data class Normal(val name: String) : Type()
            data class ThrewException(val parentId: Int?, val wasCancellation: Boolean) : Type()
        }
    }
}

fun CallTree.after(event: CallStackTrackEvent): CallTree {
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

private fun CallTree.addThrownException(node: CallTreeEventNode, wasCancellation: Boolean): CallTree = copy(
    nodes = nodes.update(node.id) {
        it!!.copy(type = CallTree.Node.Type.ThrewException(node.parent?.id, wasCancellation))
    },
)

private fun CallTree.removeNode(childNodeId: Int, parentNodeId: Int?): CallTree {
    val allChildIdsToRemove = if (childNodeId in nodes) {
        allChildIdsRecursivelyStartingFrom(rootIds = listOf(childNodeId))
    } else {
        listOf(childNodeId)
    }

    return when (parentNodeId) {
        null -> copy(
            roots = roots.remove(childNodeId),
            nodes = nodes.removeAll(allChildIdsToRemove)
        )
        else -> copy(
            nodes = nodes
                .removeAll(allChildIdsToRemove)
                .update(parentNodeId) { it!!.copy(childIds = it.childIds.remove(childNodeId)) },
        )
    }
}


private fun <K, V> PersistentMap<K, V>.update(key: K, updater: (V?) -> V): PersistentMap<K, V> =
    put(key, updater(this[key]))