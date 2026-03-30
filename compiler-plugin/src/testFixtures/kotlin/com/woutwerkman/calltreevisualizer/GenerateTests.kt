package com.woutwerkman.calltreevisualizer

import com.woutwerkman.calltreevisualizer.runners.AbstractJvmBoxTest
import com.woutwerkman.calltreevisualizer.runners.AbstractJvmDiagnosticTest
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testDataRoot = "compiler-plugin/src/test/resources", testsRoot = "compiler-plugin/test-gen") {
            testClass(AbstractJvmDiagnosticTest::class.java) {
                model("diagnostics")
            }

            testClass(AbstractJvmBoxTest::class.java) {
                model("box")
            }
        }
    }
}
