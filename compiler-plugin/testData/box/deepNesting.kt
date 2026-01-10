// Test deep nesting of suspend function calls
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

suspend fun level1(): String {
    return level2()
}

suspend fun level2(): String {
    return level3()
}

suspend fun level3(): String {
    return "deep"
}

fun box(): String {
    val tracker = TestTracker()
    var result: Result<String>? = null

    val coro: suspend () -> String = { level1() }
    coro.startCoroutine(object : Continuation<String> {
        override val context: CoroutineContext = tracker
        override fun resumeWith(res: Result<String>) {
            result = res
        }
    })

    val value = result!!.getOrThrow()

    val expected = listOf(
        "enter:level1",
        "enter:level2",
        "enter:level3",
        "exit:level3",
        "exit:level2",
        "exit:level1"
    )

    return if (tracker.calls == expected && value == "deep") {
        "OK"
    } else {
        "FAIL: calls=${tracker.calls}, value=$value"
    }
}
