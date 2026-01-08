// WITH_STDLIB
// WITH_COROUTINES

import kotlinx.coroutines.flow.toList
import com.woutwerkman.calltreevisualizer.coroutineintegration.CallStackTrackEventType
import com.woutwerkman.calltreevisualizer.coroutineintegration.trackingCallStacks

@com.woutwerkman.calltreevisualizer.NonTracked
suspend fun box(): String {
    val result = trackingCallStacks {
        foo()
    }
        .toList()
        .map { it.eventType }
        .joinToString("\n") {
            when (it) {
                CallStackTrackEventType.CallStackPopType -> "Pop"
                is CallStackTrackEventType.CallStackPushType -> "Push ${it.functionFqn}"
                is CallStackTrackEventType.CallStackThrowType -> "Boom ${it.throwable.message}"
            }
        }

    val expected = """
        Push test.foo
        Push test.bar
        Boom Aaaah!
        Pop
    """.trimIndent()
    return if (result != expected) {
        result
    } else {
        "OK"
    }
}

suspend fun foo() {
    try {
        bar()
    } catch (_: Throwable) {

    }
}

suspend fun bar() {
    error("Aaaah!")
}
