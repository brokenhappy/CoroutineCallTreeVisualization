plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "com.woutwerkman"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":JavaCallTracking"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}


tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }

    // Pass the Java Agent
    val agentJar = project(":JavaCallTracking").tasks.jar.get().archiveFile.get().asFile
    dependsOn(project(":JavaCallTracking").tasks.jar)
    
    jvmArgs("-javaagent:${agentJar.absolutePath}", "--enable-preview")
    
    // We need to allow the agent to instrument classes
    jvmArgs("-XX:+AllowRedefinitionToAddDeleteMethods")
}

