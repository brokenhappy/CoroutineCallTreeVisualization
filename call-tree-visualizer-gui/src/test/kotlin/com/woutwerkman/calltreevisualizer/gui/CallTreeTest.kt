package com.woutwerkman.calltreevisualizer.gui

import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEvent
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType.CallStackThrowType
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallTreeEventNode
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

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
    fun `cancelled node is marked as ThrewException with wasCancellation true`() {
        val tree = CallStack.Empty
            .afterPushing("root")
            .afterPushing("cancelling")
            .afterCancelling()
            .tree

        val cancelledNode = tree.nodes[2]!!
        val type = cancelledNode.type as CallTree.Node.Type.ThrewException
        assertEquals(true, type.wasCancellation)
    }

    @Test
    fun `pushing a child node removes existing exception child nodes`() {
        val tree = CallStack.Empty
            .afterPushing("root")
            .afterPushing("throwing")
            .afterThrowing()
            // Now "throwing" is a ThrewException node under "root" with id 2
            .afterPushing("newChild")
            .tree

        assertEquals(
            listOf(
                "root",
                "newChild"
            ),
            generateSequence(tree.roots.map { tree.nodes[it]!! }) { nodes ->
                when (nodes.size) {
                    0 -> null
                    1 -> nodes.single().childIds.map { tree.nodes[it]!! }
                    else -> fail("Tree is expected to be a stack, instead got a fork: $nodes")
                }
            }
                .mapNotNull { it.firstOrNull()?.type }
                .map { (it as? CallTree.Node.Type.Normal) ?: fail("Expected only normal nodes, got $it") }
                .map { it.name }
                .toList(),
        )
    }

    @Test
    fun `pushing a child node does NOT remove existing normal child nodes`() {
        val root = CallTreeEventNode(1, "root", null)

        val tree = CallTree.Empty
            .after(pushing(root))
            .after(pushing("child1").on(root))
            .after(pushing("child2").on(root))

        val rootNodeAfter = tree.nodes[1]!!
        assertEquals(2, rootNodeAfter.childIds.size)
    }

    @Test
    fun `popping a root node removes it and all its children`() {
        val tree = CallStack.Empty
            .afterPushing("root")
            .afterPushing("child")
            .afterPopping() // pop child
            .afterPopping() // pop root
            .tree

        assertEquals(tree, CallTree.Empty)
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
private data class CallStack(val tree: CallTree, val eventNodes: PersistentMap<Int, CallTreeEventNode>) {
    companion object {
        val Empty = CallStack(CallTree.Empty, persistentMapOf())
    }
}

private fun CallStack.leafNode(): CallTreeEventNode? {
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

private fun CallStack.afterCancelling(): CallStack {
    val leaf = leafNode()!!
    return CallStack(
        tree.after(CallStackTrackEvent(leaf, CallStackTrackEventType.CallStackCancelled)),
        eventNodes.remove(leaf.id, leaf),
    )
}

private data class PushingDsl(val fqn: String)
private fun pushing(fqn: String): PushingDsl = PushingDsl(fqn)
private fun PushingDsl.on(node: CallTreeEventNode?): CallStackTrackEvent =
    CallStackTrackEvent(CallTreeEventNode((node?.id ?: 0) + 1, fqn, node), CallStackTrackEventType.CallStackPushType)
private fun pushing(node: CallTreeEventNode): CallStackTrackEvent =
    CallStackTrackEvent(node, CallStackTrackEventType.CallStackPushType)

private fun popping(leaf: CallTreeEventNode): CallStackTrackEvent =
    CallStackTrackEvent(leaf, CallStackTrackEventType.CallStackPopType)

private fun throwing(throwable: Throwable = IgnoredException()): CallStackThrowType = CallStackThrowType(throwable)
private fun CallStackThrowType.from(node: CallTreeEventNode): CallStackTrackEvent = CallStackTrackEvent(node, this)

class IgnoredException: Throwable(null, null, false, false)
