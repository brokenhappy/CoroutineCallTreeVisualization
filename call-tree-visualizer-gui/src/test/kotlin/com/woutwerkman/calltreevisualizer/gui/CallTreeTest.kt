package com.woutwerkman.calltreevisualizer.gui

import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEvent
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType.CallStackThrowType
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallTreeNode
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Test
import kotlin.test.assertEquals

class CallTreeTest {
    @Test
    fun `pushing a root node adds it to the tree`() {
        val tree = CallStack.Empty
            .afterPushing("root")
            .tree

        assertEquals(1, tree.roots.size)
        assertEquals(1, tree.roots[0])
        assertEquals("root", (tree.nodes[1]?.type as? CallTree.Node.Type.Normal)?.name)
    }

    @Test
    fun `pushing a child node adds it to its parent`() {
        val tree = CallStack.Empty
            .afterPushing("root")
            .afterPushing("child")
            .tree

        assertEquals(1, tree.nodes[1]?.childIds?.size)
        assertEquals(2, tree.nodes[1]?.childIds?.get(0))
        assertEquals("child", (tree.nodes[2]?.type as? CallTree.Node.Type.Normal)?.name)
    }

    @Test
    fun `popping a catching frame removes it and all exception child nodes`() {
        val stackWithException = CallStack.Empty
            .afterPushing("root")
            .afterPushing("catching")
            .afterPushing("throwing")
            .afterThrowing()

        assertEquals(3, stackWithException.tree.nodes.size)
        assertEquals(true, stackWithException.tree.nodes[3]?.type is CallTree.Node.Type.ThrewException)

        val treeAfterCatch = stackWithException
            .afterPopping()
            .tree

        assertEquals(null, treeAfterCatch.nodes[2])
        assertEquals(null, treeAfterCatch.nodes[3])
        assertEquals(1, treeAfterCatch.nodes.size)
        assertEquals(0, treeAfterCatch.nodes[1]?.childIds?.size)
    }
}

/**
 * In the tests, most call trees are just stacks; with this assumption this builds some simpler APIs on top
 * Another assumption is that the Node ids are simply positive integers increasing from the root.
 */
private data class CallStack(val tree: CallTree, val eventNodes: PersistentMap<Int, CallTreeNode>) {
    companion object {
        val Empty = CallStack(CallTree.Empty, persistentMapOf())
    }
}

private fun CallStack.leafNode(): CallTreeNode? {
    if (tree.nodes.isEmpty()) return null
    val root = tree
        .roots
        .singleOrNull()
        ?.let { tree.nodes[it] }
        .let { it ?: error("These test utility functions only make sense on trees that are single call stack") }

    return generateSequence(root) { it.childIds.singleOrNull()?.let { tree.nodes[it] } }
        .last { it.type is CallTree.Node.Type.Normal }
        .let { eventNodes[it.id] }
}

private fun CallStack.afterPushing(fqn: String): CallStack {
    val event = pushing(fqn).on(leafNode())
    return CallStack(
        tree.after(event),
        eventNodes.put(event.node.id, event.node),
    )
}

private fun CallStack.afterPopping(): CallStack {
    val leaf = leafNode()!!
    return CallStack(
        tree.after(popping(leaf)),
        eventNodes.remove(leaf.id, leaf),
    )
}

private fun CallStack.afterThrowing(throwable: Throwable = IgnoredException()): CallStack {
    val leaf = leafNode()!!
    return CallStack(
        tree.after(throwing(throwable).from(leaf)),
        eventNodes.remove(leaf.id, leaf),
    )
}

private data class PushingDsl(val fqn: String)
private fun pushing(fqn: String): PushingDsl = PushingDsl(fqn)
private fun PushingDsl.on(node: CallTreeNode?): CallStackTrackEvent =
    CallStackTrackEvent(CallTreeNode((node?.id ?: 0) + 1, fqn, node), CallStackTrackEventType.CallStackPushType)

private fun popping(leaf: CallTreeNode): CallStackTrackEvent =
    CallStackTrackEvent(leaf, CallStackTrackEventType.CallStackPopType)

private fun throwing(throwable: Throwable = IgnoredException()): CallStackThrowType = CallStackThrowType(throwable)
private fun CallStackThrowType.from(node: CallTreeNode): CallStackTrackEvent = CallStackTrackEvent(node, this)

class IgnoredException: Throwable(null, null, false, false)
