package com.woutwerkman.calltreevisualizer

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CallTreeVisualizerGradlePluginTest {

    @Test
    fun `test plugin artifact version uses gradle plugin version and user kotlin version`() {
        // Given: A project with Kotlin JVM plugin applied
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.kotlin.jvm")

        // When: CallTreeVisualizerGradlePlugin is applied
        val plugin = CallTreeVisualizerGradlePlugin()
        plugin.apply(project)

        // Then: The plugin artifact version should be gradlePluginVersion-kotlinVersion
        val artifact = plugin.getPluginArtifact()

        assertEquals(BuildConfig.KOTLIN_PLUGIN_GROUP, artifact.groupId)
        assertEquals(BuildConfig.KOTLIN_PLUGIN_NAME, artifact.artifactId)

        // The version should follow the pattern: gradlePluginVersion-kotlinVersion
        val expectedPrefix = "${BuildConfig.KOTLIN_PLUGIN_VERSION}-"
        val version = artifact.version ?: error("Version should not be null")
        assertTrue(
            version.startsWith(expectedPrefix),
            "Expected version to start with '$expectedPrefix' but was '$version'"
        )

        // Extract and verify the Kotlin version part
        val kotlinVersion = version.removePrefix(expectedPrefix)
        assertTrue(
            kotlinVersion.matches(Regex("""\d+\.\d+\.\d+""")),
            "Expected Kotlin version to match pattern X.Y.Z but was '$kotlinVersion'"
        )
    }

    @Test
    fun `test plugin artifact version with multiplatform project`() {
        // Given: A project with Kotlin Multiplatform plugin applied
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.kotlin.multiplatform")

        // When: CallTreeVisualizerGradlePlugin is applied
        val plugin = CallTreeVisualizerGradlePlugin()
        plugin.apply(project)

        // Then: The plugin should successfully resolve the Kotlin version
        val artifact = plugin.getPluginArtifact()

        val expectedPrefix = "${BuildConfig.KOTLIN_PLUGIN_VERSION}-"
        val version = artifact.version ?: error("Version should not be null")
        assertTrue(
            version.startsWith(expectedPrefix),
            "Expected version to start with '$expectedPrefix' but was '$version'"
        )
    }

    @Test
    fun `test compiler plugin id matches build config`() {
        // Given: A plugin instance
        val project = ProjectBuilder.builder().build()
        val plugin = CallTreeVisualizerGradlePlugin()
        plugin.apply(project)

        // Then: The compiler plugin ID should match BuildConfig
        assertEquals(BuildConfig.KOTLIN_PLUGIN_ID, plugin.getCompilerPluginId())
    }

    @Test
    fun `compiler plugin version follows pattern gradlePluginVersion-kotlinVersion`() {
        // Given: A Gradle project with Kotlin JVM plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.kotlin.jvm")

        // When: CallTreeVisualizerGradlePlugin is applied
        val plugin = CallTreeVisualizerGradlePlugin()
        plugin.apply(project)

        // Then: The compiler plugin version should follow the pattern: X.Y.Z-A.B.C
        val artifact = plugin.getPluginArtifact()
        val version = artifact.version ?: error("Version should not be null")

        // Verify it matches the pattern: version-kotlinVersion
        val versionPattern = Regex("""(\d+\.\d+\.\d+)-(\d+\.\d+\.\d+)""")
        assertTrue(
            versionPattern.matches(version),
            "Expected version to match pattern 'X.Y.Z-A.B.C' but was '$version'"
        )

        // Extract and verify the parts
        val match = versionPattern.matchEntire(version)!!
        val gradlePluginVersion = match.groupValues[1]
        val kotlinVersion = match.groupValues[2]

        // The gradle plugin version should be BuildConfig.KOTLIN_PLUGIN_VERSION
        assertEquals(BuildConfig.KOTLIN_PLUGIN_VERSION, gradlePluginVersion,
            "Expected gradle plugin version to be '${BuildConfig.KOTLIN_PLUGIN_VERSION}'"
        )

        // The kotlin version should be a valid version string
        assertTrue(
            kotlinVersion.matches(Regex("""\d+\.\d+\.\d+""")),
            "Expected Kotlin version to be valid but was '$kotlinVersion'"
        )
    }

    @Test
    fun `changing kotlin version changes compiler plugin artifact version`() {
        // Given: A project with a stubbed Kotlin version resolver
        val project = ProjectBuilder.builder().build()
        val plugin = CallTreeVisualizerGradlePlugin()
        plugin.apply(project)

        // When: Using Kotlin 2.1.0
        plugin.versionResolver = object : KotlinVersionResolver {
            override fun resolveKotlinVersion(project: org.gradle.api.Project) = "2.1.0"
        }
        val artifact1 = plugin.getPluginArtifact()

        // Then: Compiler plugin version should be gradlePluginVersion-2.1.0
        assertEquals("${BuildConfig.KOTLIN_PLUGIN_VERSION}-2.1.0", artifact1.version)

        // When: Changing to Kotlin 2.2.0
        plugin.versionResolver = object : KotlinVersionResolver {
            override fun resolveKotlinVersion(project: org.gradle.api.Project) = "2.2.0"
        }
        val artifact2 = plugin.getPluginArtifact()

        // Then: Compiler plugin version should be gradlePluginVersion-2.2.0
        assertEquals("${BuildConfig.KOTLIN_PLUGIN_VERSION}-2.2.0", artifact2.version)

        // When: Changing to Kotlin 99.1.0
        plugin.versionResolver = object : KotlinVersionResolver {
            override fun resolveKotlinVersion(project: org.gradle.api.Project) = "99.1.0"
        }
        val artifact3 = plugin.getPluginArtifact()

        // Then: Compiler plugin version should be gradlePluginVersion-99.1.0
        assertEquals("${BuildConfig.KOTLIN_PLUGIN_VERSION}-99.1.0", artifact3.version)

        // Verify all three versions are different
        assertTrue(artifact1.version != artifact2.version)
        assertTrue(artifact2.version != artifact3.version)
        assertTrue(artifact1.version != artifact3.version)
    }

    @Test
    fun `plugin with version 98_2_1 and kotlin 99_1_0 produces correct artifact`() {
        // Given: A project
        val project = ProjectBuilder.builder().build()
        val plugin = CallTreeVisualizerGradlePlugin()
        plugin.apply(project)

        // When: Stubbing Kotlin version 99.1.0 (simulating user's Kotlin version)
        plugin.versionResolver = object : KotlinVersionResolver {
            override fun resolveKotlinVersion(project: org.gradle.api.Project) = "99.1.0"
        }

        // Then: For our plugin version 0.0.1, it should produce artifact version 0.0.1-99.1.0
        val artifact = plugin.getPluginArtifact()
        assertEquals("${BuildConfig.KOTLIN_PLUGIN_VERSION}-99.1.0", artifact.version,
            "Expected artifact version to be '${BuildConfig.KOTLIN_PLUGIN_VERSION}-99.1.0' " +
            "to match the pattern gradlePluginVersion-kotlinVersion"
        )
    }
}
