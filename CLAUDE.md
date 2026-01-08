# Agent Documentation: Coroutine Call Tree Visualization

This project provides a set of tools to visualize the execution of Kotlin suspend functions in real-time. It consists of a Kotlin compiler plugin, a coroutine integration library, and a Compose Multiplatform GUI.

## Project Structure

- `compiler-plugin`: An IR-based Kotlin compiler plugin that instruments suspend functions.
- `stack-tracking-core-api`: Public-facing APIs and annotations (e.g., `@NonTracked`) used by the plugin and integration.
- `gradle-plugin`: Integrates the compiler plugin into Gradle builds.
- `tracked-call-tree-as-flow`: Logic to capture instrumented calls and expose them as a `Flow` of events.
- `call-tree-visualizer-gui`: A Compose Multiplatform application for real-time visualization.
- `examples`: Example code (e.g., `highlyBranchingCalls`) that uses the visualization.

## Architecture & Data Flow

1.  **Instrumentation (Compiler Plugin):**
    - The `CallStackTrackingTransformer` visits every `suspend` function.
    - If the function is not `@NonTracked`, not `inline`, and has a body, it wraps the entire body in a call to `com.woutwerkman.calltreevisualizer.stackTracked`.
    - `stackTracked` takes the function's Fully Qualified Name (FQN) and the original body as a lambda.

2.  **Tracking Context (`stack-tracking-core-api`):**
    - `stackTracked` looks up a `StackTrackingContext` in the current `coroutineContext`.
    - It delegates the tracking to `StackTrackingContext.track(functionFqn, body)`.

3.  **Event Emission (`tracked-call-tree-as-flow`):**
    - `trackingCallStacks` provides a `StackTrackingContext` implementation.
    - When `track` is called:
        - A new `CallTreeNode` is created.
        - A `CallStackPushType` event is sent through a `channelFlow`.
        - The `child` lambda (original function body) is executed within a new `CoroutineContext` containing the `childNode`.
        - Upon completion, a `CallStackPopType` event is sent.
        - If an exception occurs, a `CallStackThrowType` or `CallStackCancelled` event is sent.

4.  **Visualization (GUI):**
    - `CallTreeUI` collects events from the `Flow` produced by `trackingCallStacks`.
    - It maintains a `CallTree` data structure (using `kotlinx.collections.immutable`).
    - The UI renders the tree, showing active calls, completed calls, and exceptions/cancellations.

## Key Components

### `StackTrackedTransformer.kt`
This is the heart of the instrumentation. It uses Kotlin IR to rewrite functions. It specifically handles:
- Avoiding `inline` functions (as they don't have a stable call site in the same sense).
- Replacing `IrReturn` targets to point to the new lambda instead of the original function.
- Injecting the function FQN as a constant string.

### `CallTreeCoroutineIntegration.kt`
Manages the coroutine state and event generation.
- Uses `AtomicInt` for unique node IDs.
- Handles cancellation specifically by checking `Job.isActive` when a `CancellationException` is caught.
- Uses `NonCancellable` context to ensure "Pop" or "Error" events are sent even if the flow scope is cancelled.

### `Main.kt` (Examples)
Contains interesting test cases:
- `highlyBranchingCalls`: Uses `kotlinhax.shadowroutines` (a fork of `kotlinx.coroutines`) to demonstrate complex branching and yielding.
- `measureLinearly`: Demonstrates unstructured concurrency and deep call stacks.

## Development & Testing

- **Shadowroutines:** The project depends on `kotlinhax.shadowroutines`, which must be available in `mavenLocal()`.
- **Compiler Plugin Testing:** The `compiler-plugin` module has `test-fixtures` and `testData` for JVM box tests and diagnostic tests.
- **Gradle Plugin:** To use the visualizer in a project, apply the `com.woutwerkman.calltreevisualizer` plugin.

## Technical Notes

- The project relies on `kotlin-compiler` internal APIs for the IR transformation.
- The UI uses `androidx.compose` and `org.jetbrains.compose` components.
- The `shadowroutines` fork is likely used to provide deeper integration or "shadowing" of standard coroutine behaviors for better visualization (e.g., custom `yield`, `launch`, `async` that interact with the tracker).
