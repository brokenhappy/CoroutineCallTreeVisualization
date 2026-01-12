// Test that @NonTracked annotation prevents instrumentation
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

@com.woutwerkman.calltreevisualizer.NonTracked
suspend fun notTracked(): String {
    return "hidden"
}

suspend fun tracked(): String {
    return notTracked()
}

fun box(): String {
    val tracker = TestTracker()
    var result: Result<String>? = null

    val coro: suspend () -> String = { tracked() }
    coro.startCoroutine(object : Continuation<String> {
        override val context: CoroutineContext = tracker
        override fun resumeWith(res: Result<String>) {
            result = res
        }
    })

    val value = result!!.getOrThrow()

    // notTracked should not appear in the calls list
    val expected = listOf(
        "enter:tracked",
        "exit:tracked"
    )

    return if (tracker.calls == expected && value == "hidden") {
        "OK"
    } else {
        "FAIL: calls=${tracker.calls}, value=$value, expected=$expected"
    }
}
