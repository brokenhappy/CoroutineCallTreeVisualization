// Test that early returns are handled correctly
import kotlin.coroutines.*

class TestTracker : com.woutwerkman.calltreevisualizer.StackTrackingContext {
    val calls = mutableListOf<String>()

    @com.woutwerkman.calltreevisualizer.NonTracked
    override suspend fun <T> track(functionFqn: String, child: suspend () -> T): T {
        calls.add("enter:$functionFqn")
        return try {
            child()
        } finally {
            calls.add("exit:$functionFqn")
        }
    }

    override val key: CoroutineContext.Key<*> get() = com.woutwerkman.calltreevisualizer.StackTrackingContext
}

suspend fun earlyReturn(condition: Boolean): String {
    if (condition) {
        return "early"
    }
    return "late"
}

fun box(): String {
    val tracker = TestTracker()
    var result1: Result<String>? = null
    var result2: Result<String>? = null

    // Test early return
    val coro1: suspend () -> String = { earlyReturn(true) }
    coro1.startCoroutine(object : Continuation<String> {
        override val context: CoroutineContext = tracker
        override fun resumeWith(res: Result<String>) {
            result1 = res
        }
    })

    val value1 = result1!!.getOrThrow()

    // Test late return
    val coro2: suspend () -> String = { earlyReturn(false) }
    coro2.startCoroutine(object : Continuation<String> {
        override val context: CoroutineContext = tracker
        override fun resumeWith(res: Result<String>) {
            result2 = res
        }
    })

    val value2 = result2!!.getOrThrow()

    val expected = listOf(
        "enter:earlyReturn",
        "exit:earlyReturn",
        "enter:earlyReturn",
        "exit:earlyReturn"
    )

    return if (tracker.calls == expected && value1 == "early" && value2 == "late") {
        "OK"
    } else {
        "FAIL: calls=${tracker.calls}, value1=$value1, value2=$value2"
    }
}
