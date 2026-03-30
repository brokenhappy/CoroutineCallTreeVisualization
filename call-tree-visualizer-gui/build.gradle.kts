plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.compose.gradle)
}

repositories {
    mavenCentral()
    mavenLocal()
    google()
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(project(":examples"))
    implementation(project(":tracked-call-tree-as-flow"))
    implementation(project(":JavaCallTracking"))
    implementation(compose.desktop.currentOs)
    implementation(compose.components.resources)
    implementation(libs.kotlinx.immutable.collections)
    implementation(libs.kotlinx.coroutines.swing)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":programs-for-tests"))
}

compose.resources {
    generateResClass = always
}

kotlin {
    jvmToolchain(25)
}

tasks.register<org.jetbrains.compose.reload.gradle.ComposeHotRun>("hotRunAppJava") {
    group = "application"
    description = "Run AppJava with the Java tracking agent (Hot Reload enabled)"
    mainClass.set("com.woutwerkman.calltreevisualizer.gui.AppJava")
}

gradle.projectsEvaluated {
    val jarTask = project(":JavaCallTracking").tasks.named<org.gradle.api.tasks.bundling.Jar>("jar")

    tasks.withType<JavaExec>().matching { it.name.startsWith("hot") }.configureEach {
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(25))
        })
        dependsOn(jarTask)
        doFirst {
            jvmArgs("-javaagent:${jarTask.get().archiveFile.get().asFile.absolutePath}")
        }
    }
}

tasks.test {
    useJUnitPlatform()

    val agentJar = project(":JavaCallTracking").tasks.jar.get().archiveFile.get().asFile
    dependsOn(project(":JavaCallTracking").tasks.jar)
    jvmArgs("-javaagent:${agentJar.absolutePath}", "--enable-preview")
    jvmArgs("-XX:+AllowRedefinitionToAddDeleteMethods")
}