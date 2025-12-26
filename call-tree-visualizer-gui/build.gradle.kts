plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.compose.gradle)
}

group = "com.woutwerkman.calltreevisualizer"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    google()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":examples"))
    implementation(project(":tracked-call-tree-as-flow"))
    implementation(libs.gs.core)
    implementation(compose.desktop.currentOs)
    runtimeOnly(libs.gs.ui.swing)
}

kotlin {
    jvmToolchain(23)
}

tasks.test {
    useJUnitPlatform()
}