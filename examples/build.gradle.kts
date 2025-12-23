plugins {
    kotlin("jvm") version "2.2.20"
}

group = "com.woutwerkman.calltreevisualizer"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}