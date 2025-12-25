plugins {
    alias(libs.plugins.kotlin.jvm)
    id("com.woutwerkman.calltreevisualizer") version "0.1.0-2.2.20-SNAPSHOT"
}

group = "com.woutwerkman.calltreevisualizer"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":plugin-annotations"))
    implementation(libs.kotlinhax.shadowroutines.core)
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}