plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.calltreevisualizer)
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(project(":stack-tracking-core-api"))
    implementation(libs.kotlinhax.shadowroutines.core)
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
}