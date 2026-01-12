# GUI Visualizer

This module contains a Compose Multiplatform desktop application that visualizes coroutine call trees in real-time.

## Features

- **Real-time Tree View**: Watch as functions are called and return across different coroutines.
- **State Highlighting**: See which coroutines are currently running, suspended, or have finished.
- **Exception Visualization**: Clear indicators when a function throws an exception.
- **Cancellation Tracking**: See where coroutine cancellation originated and how it propagated.

## How to use

The simplest way to use the visualizer is to use the `CallTreeUI` within your own Compose application, or run the provided `examples` project.

### Integration with `tracked-call-tree-as-flow`

Refer to `com.woutwerkman.calltreevisualizer.gui.main` to see how to run a program and how to set up a breakpointProgram.
The breakpointProgram can be used to pre configure breakpoints, such that you can use the program for a lecture, talk, or workshop.

## Running the Examples

Check the [`examples`](../examples) directory for a full demonstration of how to launch the GUI and track a sample coroutine application.
