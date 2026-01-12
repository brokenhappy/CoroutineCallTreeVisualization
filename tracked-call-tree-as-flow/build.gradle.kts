@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.binary.compatibility.validator)
    alias(libs.plugins.vanniktech.publish)
}

dependencies {
    commonMainApi(project(":stack-tracking-core-api"))
    commonMainApi(libs.kotlinx.coroutines.core)
}

kotlin {
    explicitApi()

    androidNativeArm32()
    androidNativeArm64()
    androidNativeX64()
    androidNativeX86()

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    js().nodejs()

    jvm()

    linuxArm64()
    linuxX64()

    macosArm64()
    macosX64()

    mingwX64()

    tvosArm64()
    tvosSimulatorArm64()
    tvosX64()

    wasmJs().nodejs()
    wasmWasi().nodejs()

    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    watchosX64()

    applyDefaultHierarchyTemplate()
}

version = "0.0.1"

configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
    coordinates(rootProject.group.toString(), "tracked-call-tree-as-flow", version.toString())
    pom {
        name.set("Coroutine Call Tree Visualization - Tracked Call Tree as Flow")
        description.set("Flow-based integration for capturing and tracking coroutine call trees")
    }
}
