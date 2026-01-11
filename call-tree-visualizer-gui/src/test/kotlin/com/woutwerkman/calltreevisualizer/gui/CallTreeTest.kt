package com.woutwerkman.calltreevisualizer.gui

import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEvent
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallTreeNode
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Test
import kotlin.test.assertEquals

class CallTreeTest {

    private fun CallStackTrackEventType.on(node: CallTreeNode) = CallStackTrackEvent(node, this)

    private fun pushing(fqn: String) = CallStackTrackEventType.CallStackPushType(fqn)
    private fun popping() = CallStackTrackEventType.CallStackPopType
    private fun throwing(throwable: Throwable = RuntimeException("test")) = CallStackTrackEventType.CallStackThrowType(throwable)

    @Test
    fun testInitialRoot() {
        val rootNode = CallTreeNode(1, "root")

        val treeWithRoot = CallTree(persistentMapOf(), persistentListOf())
            .treeAfter(pushing("root").on(rootNode))

        assertEquals(1, treeWithRoot.roots.size)
        assertEquals(1, treeWithRoot.roots[0])
        assertEquals("root", (treeWithRoot.nodes[1]?.type as? CallTree.Node.Type.Normal)?.name)
    }

    @Test
    fun testAddChild() {
        val rootNode = CallTreeNode(1, "root")
        val childNode = CallTreeNode(2, "child", rootNode)

        val treeWithChild = CallTree(persistentMapOf(), persistentListOf())
            .treeAfter(pushing("root").on(rootNode))
            .treeAfter(pushing("child").on(childNode))

        assertEquals(1, treeWithChild.nodes[1]?.childIds?.size)
        assertEquals(2, treeWithChild.nodes[1]?.childIds?.get(0))
        assertEquals("child", (treeWithChild.nodes[2]?.type as? CallTree.Node.Type.Normal)?.name)
    }

    @Test
    fun testExceptionFramesRemovedWhenCatchingFrameCompletes() {
        val rootNode = CallTreeNode(1, "root")
        val catchingNode = CallTreeNode(2, "catching", rootNode)
        val throwingNode = CallTreeNode(3, "throwing", catchingNode)

        val treeWithException = CallTree(persistentMapOf(), persistentListOf())
            .treeAfter(pushing("root").on(rootNode))
            .treeAfter(pushing("catching").on(catchingNode))
            .treeAfter(pushing("throwing").on(throwingNode))
            .treeAfter(throwing().on(throwingNode))

        assertEquals(3, treeWithException.nodes.size)
        assertEquals(true, treeWithException.nodes[3]?.type is CallTree.Node.Type.ThrewException)

        val treeAfterCatch = treeWithException.treeAfter(popping().on(catchingNode))

        assertEquals(null, treeAfterCatch.nodes[2])
        assertEquals(null, treeAfterCatch.nodes[3])
        assertEquals(1, treeAfterCatch.nodes.size)
        assertEquals(0, treeAfterCatch.nodes[1]?.childIds?.size)
    }
}
