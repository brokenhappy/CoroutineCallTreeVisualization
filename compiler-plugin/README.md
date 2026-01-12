# Compiler Plugin

The `compiler-plugin` is responsible for instrumenting your Kotlin code to enable coroutine call tree tracking.

## How it works

The plugin performs an IR (Intermediate Representation) transformation on all `suspend` functions (unless they are marked with `@NonTracked`).

### Transformation Example

Original code:
```kotlin
suspend fun greet(name: String) {
    println("Hello, $name")
}
```

Transformed code (simplified):
```kotlin
suspend fun greet(name: String) {
    stackTracked("com.example.greet") {
        println("Hello, $name")
    }
}
```

The `stackTracked` function looks for a `StackTrackingContext` in the current `CoroutineContext`. If found, it reports the function entry, exit, and any exceptions.

## Custom Tracking Context

You can provide your own tracking logic by implementing the `StackTrackingContext` interface.

```kotlin
package pkgname

import com.woutwerkman.calltreevisualizer.StackTrackingContext
import com.woutwerkman.calltreevisualizer.NonTracked
import kotlinx.coroutines.withContext

fun foo() = bar()
@NonTracked
fun bar() = baz()
fun baz() = Unit

// Prints:
// Starting: pkgname.foo
// Starting: pkgname.baz
// Finished: pkgname.baz
// Finished: pkgname.foo
@NonTracked
suspend fun main() {
    val tracker = object: StackTrackingContext {
        override suspend fun <T> track(functionFqn: String, child: suspend () -> T): T {
            println("Starting: $functionFqn")
            return child()
                .also { println("Finished: $functionFqn") }
        }
    }
    withContext(tracker) {
        foo()
    }
}
```

## Opting Out

To prevent a function from being instrumented, use the `@NonTracked` annotation. This is useful for:
- Performance-critical internal functions.
- Functions that you don't want to clutter the visualizer.
- Middleware or utility functions that don't represent a meaningful step in the call tree.
