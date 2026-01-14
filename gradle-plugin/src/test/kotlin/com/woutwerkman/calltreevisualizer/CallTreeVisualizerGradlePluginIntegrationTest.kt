package com.woutwerkman.calltreevisualizer

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Real integration tests that verify the Gradle plugin correctly resolves compiler plugin versions
 * by running actual Gradle builds with different Kotlin versions.
 *
 * These tests rely on published artifacts in Maven Local:
 * - gradle-plugin: com.woutwerkman.calltreevisualizer:gradle-plugin:0.0.2
 * - compiler-plugin: com.woutwerkman.calltreevisualizer:compiler-plugin:0.0.2-2.2.20
 */
class CallTreeVisualizerGradlePluginIntegrationTest {

    @Test
    fun `build succeeds with kotlin 2_2_20 when compiler plugin 0_0_2-2_2_20 exists`(@TempDir projectDir: File) {
        // Given: A project with Kotlin 2.2.20 (matching published compiler plugin 0.0.2-2.2.20)
        setupTestProject(projectDir, kotlinVersion = "2.2.20")

        // When: Running compileKotlin
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("compileKotlin", "--stacktrace")
            .build()

        // Then: Build should succeed because 0.0.2-2.2.20 compiler plugin exists in Maven Local
        assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlin")?.outcome)
        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
    }

    @Test
    fun `build fails with kotlin 2_1_0 when compiler plugin 0_0_2-2_1_0 does not exist`(@TempDir projectDir: File) {
        // Given: A project with Kotlin 2.1.0 (NO matching published compiler plugin 0.0.2-2.1.0)
        setupTestProject(projectDir, kotlinVersion = "2.1.0")

        // When: Running compileKotlin
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("compileKotlin", "--stacktrace")
            .buildAndFail()

        // Then: Build should fail because 0.0.2-2.1.0 compiler plugin doesn't exist
        assertTrue(
            result.output.contains("Could not find com.woutwerkman.calltreevisualizer:compiler-plugin:0.0.2-2.1.0") ||
            result.output.contains("Could not resolve com.woutwerkman.calltreevisualizer:compiler-plugin:0.0.2-2.1.0") ||
            result.output.contains("0.0.2-2.1.0"),
            "Expected failure message about missing version 0.0.2-2.1.0 in output:\n${result.output}"
        )
    }

    @Test
    fun `build fails with kotlin 2_0_0 when compiler plugin 0_0_2-2_0_0 does not exist`(@TempDir projectDir: File) {
        // Given: A project with Kotlin 2.0.0 (NO matching published compiler plugin 0.0.2-2.0.0)
        setupTestProject(projectDir, kotlinVersion = "2.0.0")

        // When: Running compileKotlin
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("compileKotlin", "--stacktrace")
            .buildAndFail()

        // Then: Build should fail because 0.0.2-2.0.0 compiler plugin doesn't exist
        assertTrue(
            result.output.contains("Could not find com.woutwerkman.calltreevisualizer:compiler-plugin:0.0.2-2.0.0") ||
            result.output.contains("Could not resolve com.woutwerkman.calltreevisualizer:compiler-plugin:0.0.2-2.0.0") ||
            result.output.contains("0.0.2-2.0.0"),
            "Expected failure message about missing version 0.0.2-2.0.0 in output:\n${result.output}"
        )
    }

    @Test
    fun `build fails with kotlin 1_9_0 when compiler plugin 0_0_2-1_9_0 does not exist`(@TempDir projectDir: File) {
        // Given: A project with Kotlin 1.9.0 (NO matching published compiler plugin 0.0.2-1.9.0)
        setupTestProject(projectDir, kotlinVersion = "1.9.0")

        // When: Running compileKotlin
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("compileKotlin", "--stacktrace")
            .buildAndFail()

        // Then: Build should fail because 0.0.2-1.9.0 compiler plugin doesn't exist
        assertTrue(
            result.output.contains("Could not find com.woutwerkman.calltreevisualizer:compiler-plugin:0.0.2-1.9.0") ||
            result.output.contains("Could not resolve com.woutwerkman.calltreevisualizer:compiler-plugin:0.0.2-1.9.0") ||
            result.output.contains("0.0.2-1.9.0"),
            "Expected failure message about missing version 0.0.2-1.9.0 in output:\n${result.output}"
        )
    }

    @Test
    fun `different kotlin versions attempt to resolve different compiler plugin versions`(@TempDir tempDir: File) {
        // Test 1: Kotlin 2.2.20 should succeed (published)
        val projectDir1 = tempDir.resolve("project-kotlin-2-2-20").also { it.mkdirs() }
        setupTestProject(projectDir1, kotlinVersion = "2.2.20")
        val result1 = GradleRunner.create()
            .withProjectDir(projectDir1)
            .withArguments("compileKotlin", "--stacktrace")
            .build()

        // Verify it succeeded (compiler plugin 0.0.2-2.2.20 was resolved)
        assertEquals(TaskOutcome.SUCCESS, result1.task(":compileKotlin")?.outcome)

        // Test 2: Kotlin 2.1.0 should fail (not published)
        val projectDir2 = tempDir.resolve("project-kotlin-2-1-0").also { it.mkdirs() }
        setupTestProject(projectDir2, kotlinVersion = "2.1.0")
        val result2 = GradleRunner.create()
            .withProjectDir(projectDir2)
            .withArguments("compileKotlin")
            .buildAndFail()

        // Verify it failed with the right version
        assertTrue(result2.output.contains("0.0.2-2.1.0"))

        // Test 3: Kotlin 2.0.0 should fail (not published)
        val projectDir3 = tempDir.resolve("project-kotlin-2-0-0").also { it.mkdirs() }
        setupTestProject(projectDir3, kotlinVersion = "2.0.0")
        val result3 = GradleRunner.create()
            .withProjectDir(projectDir3)
            .withArguments("compileKotlin")
            .buildAndFail()

        // Verify it failed with the right version
        assertTrue(result3.output.contains("0.0.2-2.0.0"))
    }

    private fun setupTestProject(projectDir: File, kotlinVersion: String) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "test-project"

            pluginManagement {
                repositories {
                    mavenLocal()
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            """.trimIndent()
        )

        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                kotlin("jvm") version "$kotlinVersion"
                id("com.woutwerkman.calltreevisualizer") version "0.0.2"
            }

            repositories {
                mavenLocal()
                mavenCentral()
            }
            """.trimIndent()
        )

        // Create a simple Kotlin source file with a suspend function to trigger the plugin
        projectDir.resolve("src/main/kotlin").mkdirs()
        projectDir.resolve("src/main/kotlin/Main.kt").writeText(
            """
            suspend fun main() {
                println("Hello World")
            }
            """.trimIndent()
        )
    }
}
