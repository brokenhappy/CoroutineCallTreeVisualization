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
    implementation(compose.desktop.currentOs)
    implementation(compose.components.resources)
    implementation(libs.kotlinx.immutable.collections)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.resources {
    generateResClass = always
}

kotlin {
    jvmToolchain(23)
}

tasks.test {
    useJUnitPlatform()
}