package com.woutwerkman.calltreevisualizer.gui

import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEvent
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallTreeNode
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Test
import kotlin.test.assertEquals

class CallTreeTest {

    private fun stubPushEvent(id: Int, fqn: String, parent: CallTreeNode? = null): CallStackTrackEvent {
        return CallStackTrackEvent(CallTreeNode(id, fqn, parent), CallStackTrackEventType.CallStackPushType(fqn))
    }

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
}
