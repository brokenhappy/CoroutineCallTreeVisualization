package com.woutwerkman

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AgentTest {

    class TestClass {
        fun trackedMethod() {
            anotherTrackedMethod()
        }

        fun anotherTrackedMethod() {
        }
    }

    @Test
    fun testAgentTracking() {
        val calls = mutableListOf<String>()
        val trackerImpl = object : StackTrackerJava {
            override fun <T> track(functionFqn: String, child: () -> T): T {
                calls.add(functionFqn)
                return child()
            }
        }

        ScopedValue.where(tracker, trackerImpl).run {
            TestClass().trackedMethod()
        }

        println("Calls: $calls")
        assertTrue(calls.any { it.contains("trackedMethod") })
        assertTrue(calls.any { it.contains("anotherTrackedMethod") })
    }
}
