package com.woutwerkman.calltreevisualizer.services

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import com.woutwerkman.calltreevisualizer.ir.CallTreeVisualizerIrGenerationExtension
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

fun TestConfigurationBuilder.configurePlugin() {
    useConfigurators(::ExtensionRegistrarConfigurator)
    configureStackTrackingCoreApi()
}

private class ExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        IrGenerationExtension.registerExtension(CallTreeVisualizerIrGenerationExtension())
    }
}
