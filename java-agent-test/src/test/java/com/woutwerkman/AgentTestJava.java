package com.woutwerkman;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AgentTestJava {

    static class TestClass {
        public void trackedMethod() {
            anotherTrackedMethod();
        }

        public void anotherTrackedMethod() {
        }
    }

    @Test
    void testAgentTracking() {
        List<String> calls = new ArrayList<>();
        StackTrackerJava trackerImpl = new StackTrackerJava() {
            @Override
            public <T> T track(String functionFqn, kotlin.jvm.functions.Function0<? extends T> child) {
                calls.add(functionFqn);
                return child.invoke();
            }
        };

        ScopedValue.where(TrackingWrapperKt.getTracker(), trackerImpl).run(() ->
            new TestClass().trackedMethod()
        );

        System.out.println("Calls: " + calls);
        assertTrue(calls.stream().anyMatch(it -> it.contains("trackedMethod")));
        assertTrue(calls.stream().anyMatch(it -> it.contains("anotherTrackedMethod")));
    }
}
