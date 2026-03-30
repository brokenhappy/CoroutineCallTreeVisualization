package com.woutwerkman.calltreevisualizer.gui;

import com.woutwerkman.calltreevisualizer.JavaExamples;

import static com.woutwerkman.calltreevisualizer.gui.BreakpointDslKt.*;

public class AppJava {
    static void main() {
        var breakpoints = changeSpeed(30);
        breakpoints = then(breakpoints, breakAfter(functionCall("static void com.woutwerkman.calltreevisualizer.JavaExamples.linearExplosion()")));
        breakpoints = then(breakpoints, changeSpeed(30));
        breakpoints = then(breakpoints, breakBefore(functionThrows("static void com.woutwerkman.calltreevisualizer.JavaExamples.recurse(int)")));
        breakpoints = then(breakpoints, changeSpeed(10));
        breakpoints = then(breakpoints, breakAfter(functionCall("static void com.woutwerkman.calltreevisualizer.JavaExamples.firstStructuredConcurrency()")));
        breakpoints = then(breakpoints, changeSpeed(10));
        breakpoints = then(breakpoints, breakAfter(functionCall("static void com.woutwerkman.calltreevisualizer.JavaExamples.branchingCalls()")));
        breakpoints = then(breakpoints, changeSpeed(10));
        breakpoints = then(breakpoints, breakBefore(functionThrows("static void com.woutwerkman.calltreevisualizer.JavaExamples.baz(boolean)")));
        breakpoints = then(breakpoints, changeSpeed(10));

        AppKt.runDebugger(
            JavaTrackingKt.trackingJavaCallStacks(JavaExamples::testProgram),
            breakpoints
        );
    }
}
