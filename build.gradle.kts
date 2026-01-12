plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.binary.compatibility.validator) apply false
    alias(libs.plugins.buildconfig) apply false
    alias(libs.plugins.vanniktech.publish) apply false
}

allprojects {
    group = "com.woutwerkman.calltreevisualizer"
    version = "0.0.1-2.2.20"

    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
            publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
            
            val hasSigningKey = providers.gradleProperty("signing.keyId").isPresent || 
                                providers.gradleProperty("signing.secretKey").isPresent
            if (hasSigningKey) {
                signAllPublications()
            }

            pom {
                name.set("Coroutine Call Tree Visualization")
                description.set("Tools to visualize the execution of Kotlin suspend functions in real-time")
                url.set("https://github.com/brokenhappy/CoroutineCallTreeVisualization")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("Wout Werkman")
                        name.set("Wout Werkman")
                        email.set("wout_werkman@hotmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/brokenhappy/CoroutineCallTreeVisualization.git")
                    developerConnection.set("scm:git:ssh://github.com/brokenhappy/CoroutineCallTreeVisualization.git")
                    url.set("https://github.com/brokenhappy/CoroutineCallTreeVisualization")
                }
            }
        }
    }
}
