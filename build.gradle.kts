plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.binary.compatibility.validator) apply false
    alias(libs.plugins.buildconfig) apply false
    id("signing")
}

allprojects {
    group = "com.woutwerkman.calltreevisualizer"
    version = "0.0.1-2.2.20"

    repositories {
        mavenCentral()
    }
}

subprojects {
    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "sonatype"
                    val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                    val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                    url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                    credentials {
                        username = findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
                        password = findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
                    }
                }
            }

            publications.withType<MavenPublication> {
                pom {
                    name.set("Coroutine Call Tree Visualization")
                    description.set("Tools to visualize the execution of Kotlin suspend functions in real-time")
                    url.set("https://github.com/brokenhappy")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("woutwo")
                            name.set("Wout Werkman")
                            email.set("wout_werkman@hotmail.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/brokenhappy/CoroutineCallTreeVisualization.git")
                        developerConnection.set("scm:git:ssh://github.com/brokenhappy/CoroutineCallTreeVisualization.git")
                        url.set("https://github.com/brokenhappy")
                    }
                }
            }
        }

        plugins.withId("signing") {
            extensions.configure<SigningExtension> {
                val signingKey = findProperty("signing.keyId") as String? ?: System.getenv("SIGNING_KEY_ID")
                val signingPassword = findProperty("signing.password") as String? ?: System.getenv("SIGNING_PASSWORD")
                val secretKeyRingFile = findProperty("signing.secretKeyRingFile") as String? ?: System.getenv("SIGNING_SECRET_KEY_RING_FILE")

                if (signingKey != null && signingPassword != null) {
                    sign(extensions.getByType<PublishingExtension>().publications)
                }
            }
        }
    }
}
