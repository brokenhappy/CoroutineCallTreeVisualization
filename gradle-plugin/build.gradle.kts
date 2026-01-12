plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.gradle.plugin)
    alias(libs.plugins.vanniktech.publish)
}

dependencies {
    compileOnly(libs.kotlin.gradle.plugin.api)
    testImplementation(libs.kotlin.test.junit5)
}

buildConfig {
    packageName(project.group.toString())

    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.group}\"")

    buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${libs.plugins.calltreevisualizer.compilerPlugin.get().pluginId.substringBeforeLast(".")}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"compiler-plugin\"") // Artifact ID is not in PluginDependency
    buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${libs.plugins.calltreevisualizer.compilerPlugin.get().version.requiredVersion}\"")

    buildConfigField(
        type = "String",
        name = "ANNOTATIONS_LIBRARY_COORDINATES",
        expression = "\"${libs.calltreevisualizer.coreApi.get().group}:${libs.calltreevisualizer.coreApi.get().name}:${libs.calltreevisualizer.coreApi.get().versionConstraint.requiredVersion}\""
    )
}


gradlePlugin {
    website.set("https://github.com/brokenhappy/CoroutineCallTreeVisualization")
    vcsUrl.set("https://github.com/brokenhappy/CoroutineCallTreeVisualization.git")

    plugins {
        create("callTreeVisualizerPlugin") {
            id = rootProject.group.toString()
            displayName = "Coroutine Tracker Compiler plugin"
            description = "A Gradle plugin that applies a Coroutine tracking Compiler plugin"
            implementationClass = "com.woutwerkman.calltreevisualizer.CallTreeVisualizerGradlePlugin"
            tags.set(listOf("kotlin", "coroutines", "tracking", "debugging"))
        }
    }
}

configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
    coordinates(rootProject.group.toString(), "gradle-plugin", rootProject.version.toString())
    pom {
        name.set("Coroutine Call Tree Visualization - Gradle Plugin")
        description.set("Gradle plugin to easily apply the Coroutine Call Tree Visualization compiler plugin")
    }
}
