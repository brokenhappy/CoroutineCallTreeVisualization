// Test the basic transformation using intrinsics only
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

// Simple test tracker that collects function calls
class TestTracker : com.woutwerkman.calltreevisualizer.StackTrackingContext {
    val calls = mutableListOf<String>()

    @com.woutwerkman.calltreevisualizer.NonTracked
    override suspend fun <T> track(functionFqn: String, child: suspend () -> T): T {
        calls.add("enter:$functionFqn")
        return try {
            val result = child()
            calls.add("exit:$functionFqn")
            result
        } catch (e: Throwable) {
            calls.add("throw:$functionFqn:${e.message}")
            throw e
        }
    }

    override val key: CoroutineContext.Key<*> get() = com.woutwerkman.calltreevisualizer.StackTrackingContext
}

// Helper to run suspend functions
fun <T> runBlocking(block: suspend () -> T): T {
    val tracker = TestTracker()
    var result: Result<T>? = null

    block.startCoroutine(object : Continuation<T> {
        override val context: CoroutineContext = tracker
        override fun resumeWith(res: Result<T>) {
            result = res
        }
    })

    return result!!.getOrThrow()
}

// Test functions
suspend fun testFoo(): String {
    return "foo"
}

suspend fun testBar(): String {
    testFoo()
    return "bar"
}

fun box(): String {
    val tracker = TestTracker()

    // Run a simple test
    var result: Result<String>? = null
    val continuation = object : Continuation<String> {
        override val context: CoroutineContext = tracker
        override fun resumeWith(res: Result<String>) {
            result = res
        }
    }

    val coro: suspend () -> String = { testBar() }
    coro.startCoroutine(continuation)

    val value = result!!.getOrThrow()

    // Check that tracking happened
    val expected = listOf(
        "enter:testBar",
        "enter:testFoo",
        "exit:testFoo",
        "exit:testBar"
    )

    return if (tracker.calls == expected && value == "bar") {
        "OK"
    } else {
        "FAIL: calls=${tracker.calls}, value=$value"
    }
}
