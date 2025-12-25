plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.binary.compatibility.validator) apply false
    alias(libs.plugins.buildconfig) apply false
}

allprojects {
    group = "com.woutwerkman.calltreevisualizer"
    version = "0.1.0-2.2.20-SNAPSHOT"
}
