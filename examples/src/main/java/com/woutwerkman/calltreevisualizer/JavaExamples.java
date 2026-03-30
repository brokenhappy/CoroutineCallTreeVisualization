package com.woutwerkman.calltreevisualizer;

public class JavaExamples {

    public static void testProgram() {
        linearCalls();
        catchingThrow();
    }

    static void linearCalls() {
        a();
    }

    static void a() {
        b();
    }

    static void b() {
    }

    static void catchingThrow() {
        try {
            throwing();
        } catch (RuntimeException ignored) {
        }
    }

    static void throwing() {
        throw new RuntimeException("Expected");
    }
}
