# Stack Tracking Core API

The `stack-tracking-core-api` module provides the basic building blocks for the Coroutine Call Tree Visualizer. It contains the annotations and interfaces used by both the compiler plugin and the runtime.

## Key Components

### `@NonTracked` Annotation
Use this annotation to mark functions that should not be instrumented by the compiler plugin.

```kotlin
@NonTracked
suspend fun doInternalWork() { ... }
```

### `StackTrackingContext` Interface
This is a `CoroutineContext.Element` that defines how function calls are tracked.

```kotlin
interface StackTrackingContext : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> get() = StackTrackingContext

    suspend fun <T> track(functionFqn: String, child: suspend () -> T): T

    companion object Key : CoroutineContext.Key<StackTrackingContext>
}
```

### `stackTracked` Function
This is the low-level function that the compiler plugin injects into your code. It's usually not called manually.

```kotlin
suspend fun <T> stackTracked(functionFqn: String, body: suspend () -> T): T
```

## Multiplatform Support
This module is a Kotlin Multiplatform (KMP) library, supporting:
- JVM
- JS (Node.js)
- WASM (JS & WASI)
- Native (iOS, macOS, Linux, Windows, Android Native, etc.)
- WatchOS, TvOS

This allows you to instrument code across almost any Kotlin target.
