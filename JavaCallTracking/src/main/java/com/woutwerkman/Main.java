package com.woutwerkman;

import java.util.concurrent.StructuredTaskScope;

@SuppressWarnings("preview")
public class Main {
    static void main() {
        try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {
            scope.fork(() -> {
                System.out.println("Hello, world!");
            });
            scope.fork(() -> {
                System.out.println("Hello, world!");
            });
        }
    }
}
