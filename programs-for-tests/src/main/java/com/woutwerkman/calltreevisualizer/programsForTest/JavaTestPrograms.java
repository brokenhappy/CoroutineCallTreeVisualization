package com.woutwerkman.calltreevisualizer.programsForTest;

public class JavaTestPrograms {

    public static void linearCall() {
        a();
    }

    static void a() {
        b();
    }

    static void b() {}

    public static void throwingCall() {
        try {
            throwing();
        } catch (RuntimeException ignored) {}
    }

    static void throwing() {
        throw new RuntimeException("Expected");
    }
}
