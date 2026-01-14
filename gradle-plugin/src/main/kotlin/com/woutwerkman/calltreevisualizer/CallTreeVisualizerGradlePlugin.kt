package com.woutwerkman.calltreevisualizer

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import com.woutwerkman.calltreevisualizer.BuildConfig.ANNOTATIONS_LIBRARY_COORDINATES
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@Suppress("unused") // Used via reflection.
class CallTreeVisualizerGradlePlugin : KotlinCompilerPluginSupportPlugin {
    private lateinit var project: Project

    // Exposed for testing - allows injecting a custom version resolver
    internal var versionResolver: KotlinVersionResolver = DefaultKotlinVersionResolver

    override fun apply(target: Project) {
        project = target
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = BuildConfig.KOTLIN_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact {
        val kotlinVersion = versionResolver.resolveKotlinVersion(project)
        return SubpluginArtifact(
            groupId = BuildConfig.KOTLIN_PLUGIN_GROUP,
            artifactId = BuildConfig.KOTLIN_PLUGIN_NAME,
            version = "${BuildConfig.KOTLIN_PLUGIN_VERSION}-$kotlinVersion",
        )
    }

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        kotlinCompilation.dependencies { implementation(ANNOTATIONS_LIBRARY_COORDINATES) }
        if (kotlinCompilation.implementationConfigurationName == "metadataCompilationImplementation") {
            project.dependencies.add("commonMainImplementation", ANNOTATIONS_LIBRARY_COORDINATES)
        }

        return project.provider {
            emptyList()
        }
    }
}

internal interface KotlinVersionResolver {
    fun resolveKotlinVersion(project: Project): String
}

internal object DefaultKotlinVersionResolver : KotlinVersionResolver {
    override fun resolveKotlinVersion(project: Project): String {
        // Try to get version from the Kotlin plugin using the plugin's getKotlinPluginVersion() method
        val kotlinPlugin = project.plugins.findPlugin("org.jetbrains.kotlin.multiplatform")
            ?: project.plugins.findPlugin("org.jetbrains.kotlin.jvm")
            ?: error("Kotlin plugin not found. Please apply either kotlin-jvm or kotlin-multiplatform plugin.")

        return try {
            // Try to use the public getKotlinPluginVersion method that exists on Kotlin plugins
            val method = kotlinPlugin::class.java.getMethod("getKotlinPluginVersion")
            method.invoke(kotlinPlugin) as String
        } catch (e: NoSuchMethodException) {
            // Fallback: use org.jetbrains.kotlin.config.KotlinCompilerVersion
            try {
                val versionClass = kotlinPlugin::class.java.classLoader
                    .loadClass("org.jetbrains.kotlin.config.KotlinCompilerVersion")
                val versionField = versionClass.getDeclaredField("VERSION")
                versionField.get(null) as String
            } catch (e2: Exception) {
                error("Unable to determine Kotlin version: ${e2.message}")
            }
        }
    }
}
