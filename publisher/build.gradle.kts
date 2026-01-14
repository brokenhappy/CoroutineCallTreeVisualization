plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

application {
    mainClass.set("com.woutwerkman.calltreevisualizer.publisher.PublisherKt")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.woutwerkman.calltreevisualizer.publisher.PublisherKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
