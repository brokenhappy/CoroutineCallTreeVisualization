package com.woutwerkman.calltreevisualizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Collectors;

@SuppressWarnings("preview")
public class JavaExamples {

    public static void testProgram() {
        measureAverageTemperatures();
        linearExplosion();
        unstructuredConcurrency();
        firstStructuredConcurrency();
        branchingCalls();
    }

    static void measureAverageTemperatures() {
        try {
            var resource = Path.of("/Users/Wout.Werkman/IdeaProjects/CoroutineCallTreeVisualization/examples/src/main/resources/measurements.txt");
            try (var lines = Files.lines(resource)) {
                var result = lines
                    .map(line -> line.split(": "))
                    .map(parts -> new Measurement(parts[0], Double.parseDouble(parts[1]), 1))
                    .collect(Collectors.toMap(
                        Measurement::city,
                        m -> m,
                        Measurement::add
                    ))
                    .values()
                    .stream()
                    .sorted((a, b) -> Double.compare(
                        b.totalTemperature() / b.count(),
                        a.totalTemperature() / a.count()
                    ))
                    .map(m -> m.city() + ": " + (m.totalTemperature() / m.count()))
                    .collect(Collectors.joining("\n"));
                System.out.println(result);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void linearExplosion() {
        try {
            recurse(10);
        } catch (RuntimeException ignored) {}
    }

    static void recurse(int a) {
        if (a <= 0) throw new RuntimeException("AAAH");
        recurse(a - 1);
    }

    static void unstructuredConcurrency() {
        var threads = new ArrayList<Thread>();
        for (int i = 0; i < 4; i++) {
            threads.add(Thread.ofVirtual().start(JavaExamples::measureAverageTemperatures));
        }
        try {
            for (var thread : threads) {
                try { thread.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
        } finally {
            for (var thread : threads) thread.interrupt();
        }
    }

    static void firstStructuredConcurrency() {
        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.<Void>awaitAll())) {
            scope.fork(JavaExamples::yeeld);
            yeeld();
            scope.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void yeeld() {
        Thread.yield();
    }

    static void branchingCalls() {
        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {
            scope.fork(() -> { bar(false); return null; });
            scope.fork(() -> { bar(true); return null; });
            scope.join();
        } catch (StructuredTaskScope.FailedException | InterruptedException ignored) {}
    }

    static void bar(boolean shouldThrow) {
        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {
            scope.fork(() -> { baz(shouldThrow); return null; });
            scope.fork(() -> { baz(false); return null; });
            scope.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (StructuredTaskScope.FailedException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    static void baz(boolean shouldThrow) {
        if (shouldThrow) throw new RuntimeException("Aaaah!");
    }

    record Measurement(String city, double totalTemperature, int count) {
        Measurement add(Measurement other) {
            return new Measurement(city, totalTemperature + other.totalTemperature(), count + other.count());
        }
    }
}
