package com.woutwerkman.calltreevisualizer

import com.woutwerkman.calltreevisualizer.runners.AbstractJvmBoxTest
import com.woutwerkman.calltreevisualizer.runners.AbstractJvmDiagnosticTest
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testDataRoot = "compiler-plugin/src/test/resources", testsRoot = "compiler-plugin/src/test/kotlin") {
            testClass<AbstractJvmDiagnosticTest> {
                model("diagnostics")
            }

            testClass<AbstractJvmBoxTest> {
                model("box")
            }
        }
    }
}
