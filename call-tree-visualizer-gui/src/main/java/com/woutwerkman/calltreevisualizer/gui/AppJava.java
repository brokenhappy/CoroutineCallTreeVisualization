package com.woutwerkman.calltreevisualizer.gui;

import com.woutwerkman.calltreevisualizer.JavaExamples;

public class AppJava {
    static void main() {
        var breakpoints = BreakpointDslKt.changeSpeed(30);
        breakpoints = BreakpointDslKt.then(breakpoints, BreakpointDslKt.breakAfter(BreakpointDslKt.functionCall("static void com.woutwerkman.calltreevisualizer.JavaExamples.linearCalls()")));
        breakpoints = BreakpointDslKt.then(breakpoints, BreakpointDslKt.changeSpeed(5));
        breakpoints = BreakpointDslKt.then(breakpoints, BreakpointDslKt.breakBefore(BreakpointDslKt.functionThrows("static void com.woutwerkman.calltreevisualizer.JavaExamples.throwing()")));

        AppKt.runDebugger(
            JavaTrackingKt.trackingJavaCallStacks(JavaExamples::testProgram),
            breakpoints
        );
    }
}
