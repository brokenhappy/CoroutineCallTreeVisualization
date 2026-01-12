plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.gradle.plugin)
    `maven-publish`
    signing
}

dependencies {
    compileOnly(libs.kotlin.gradle.plugin.api)
    testImplementation(libs.kotlin.test.junit5)
}

buildConfig {
    packageName(project.group.toString())

    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.group}\"")

    val pluginProject = project(":compiler-plugin")
    buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${pluginProject.group}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"${pluginProject.name}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${pluginProject.version}\"")

    val annotationsProject = project(":stack-tracking-core-api")
    buildConfigField(
        type = "String",
        name = "ANNOTATIONS_LIBRARY_COORDINATES",
        expression = "\"${annotationsProject.group}:${annotationsProject.name}:${annotationsProject.version}\""
    )
}

java {
    withJavadocJar()
    withSourcesJar()
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
