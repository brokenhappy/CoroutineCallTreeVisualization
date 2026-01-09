plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.benchmark)
}

sourceSets {
    create("benchmark") {
        dependencies {
            implementation(libs.kotlinx.benchmark.runtime)
        }
    }
}

kotlin {
    target {
        compilations.getByName("benchmark")
            .associateWith(compilations.getByName("main"))
    }
}


group = "com.woutwerkman"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(libs.kotlinx.coroutines.core)
}

benchmark {
    targets {
        register("benchmark")
    }

    configurations {
        all {
            iterations = 3
            warmups = 3
            iterationTime = 2500
            iterationTimeUnit = "ms"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}