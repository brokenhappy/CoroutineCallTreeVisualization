plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.calltreevisualizer)
}

group = "com.woutwerkman.calltreevisualizer"
version = "0.0.1"

repositories {
    mavenCentral()
    mavenLocal()
    google()
}

dependencies {
    implementation(project(":stack-tracking-core-api"))
    implementation(libs.kotlinhax.shadowroutines.core)
    implementation(libs.kotlinx.coroutines.core)
}

kotlin {
    jvmToolchain(23)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}
