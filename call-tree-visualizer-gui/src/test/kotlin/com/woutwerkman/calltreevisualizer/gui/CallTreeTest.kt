package com.woutwerkman.calltreevisualizer.gui

import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEvent
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallTreeNode
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Test
import kotlin.test.assertEquals

class CallTreeTest {

    private fun stubPushEvent(id: Int, fqn: String, parent: CallTreeNode? = null): CallStackTrackEvent =
        CallStackTrackEvent(CallTreeNode(id, fqn, parent), CallStackTrackEventType.CallStackPushType(fqn))

    @Test
    fun testInitialRoot() {
        val tree = CallTree(persistentMapOf(), persistentListOf())
        val event = stubPushEvent(1, "root")
        val updatedTree = tree.treeAfter(event)
        
        assertEquals(1, updatedTree.roots.size)
        assertEquals(1, updatedTree.roots[0])
        assertEquals("root", (updatedTree.nodes[1]?.type as? CallTree.Node.Type.Normal)?.name)
    }

    @Test
    fun testAddChild() {
        val rootNode = CallTreeNode(1, "root")
        var tree = CallTree(persistentMapOf(), persistentListOf())
        tree = tree.treeAfter(CallStackTrackEvent(rootNode, CallStackTrackEventType.CallStackPushType("root")))

        val childEvent = stubPushEvent(2, "child", rootNode)
        val updatedTree = tree.treeAfter(childEvent)

        assertEquals(1, updatedTree.nodes[1]?.childIds?.size)
        assertEquals(2, updatedTree.nodes[1]?.childIds?.get(0))
        assertEquals("child", (updatedTree.nodes[2]?.type as? CallTree.Node.Type.Normal)?.name)
    }

    @Test
    fun testExceptionFramesRemovedWhenCatchingFrameCompletes() {
        // Setup: root calls catching frame, catching frame calls throwing frame
        val rootNode = CallTreeNode(1, "root")
        val catchingNode = CallTreeNode(2, "catching", rootNode)
        val throwingNode = CallTreeNode(3, "throwing", catchingNode)

        var tree = CallTree(persistentMapOf(), persistentListOf())

        // Push root
        tree = tree.treeAfter(CallStackTrackEvent(rootNode, CallStackTrackEventType.CallStackPushType("root")))

        // Push catching frame
        tree = tree.treeAfter(CallStackTrackEvent(catchingNode, CallStackTrackEventType.CallStackPushType("catching")))

        // Push throwing frame
        tree = tree.treeAfter(CallStackTrackEvent(throwingNode, CallStackTrackEventType.CallStackPushType("throwing")))

        // Exception is thrown in throwing frame (changes its type to ThrewException)
        tree = tree.treeAfter(CallStackTrackEvent(throwingNode, CallStackTrackEventType.CallStackThrowType(RuntimeException("test"))))

        // Verify exception node exists
        assertEquals(3, tree.nodes.size)
        assertEquals(true, tree.nodes[3]?.type is CallTree.Node.Type.ThrewException)

        // The catching frame completes (catches the exception)
        tree = tree.treeAfter(CallStackTrackEvent(catchingNode, CallStackTrackEventType.CallStackPopType))

        // The catching frame and all its children (including exception nodes) should be removed
        assertEquals(null, tree.nodes[2], "Catching frame should be removed")
        assertEquals(null, tree.nodes[3], "Throwing frame (with exception) should be removed")

        // Only root should remain
        assertEquals(1, tree.nodes.size)
        assertEquals(0, tree.nodes[1]?.childIds?.size)
    }
}
