package com.woutwerkman.calltreevisualizer.services

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

private val stackTrackingCoreApiRuntimeClasspath =
    System.getProperty("coreApiRuntime.classpath")?.split(File.pathSeparator)?.map(::File)
        ?: error("Unable to get a valid classpath from 'coreApiRuntime.classpath' property")

fun TestConfigurationBuilder.configureStackTrackingCoreApi() {
    useConfigurators(::StackTrackingCoreApiProvider)
    useCustomRuntimeClasspathProviders(::StackTrackingCoreApiClasspathProvider)
}

private class StackTrackingCoreApiProvider(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.addJvmClasspathRoots(stackTrackingCoreApiRuntimeClasspath)
    }
}

private class StackTrackingCoreApiClasspathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule) = stackTrackingCoreApiRuntimeClasspath
}