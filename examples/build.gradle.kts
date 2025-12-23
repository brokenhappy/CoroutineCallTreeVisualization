plugins {
    kotlin("jvm") version "2.2.20"
    id("com.woutwerkman.calltreevisualizer") version "0.1.0-SNAPSHOT"
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
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}