package com.woutwerkman.calltreevisualizer

import com.woutwerkman.calltreevisualizer.ir.CallTreeVisualizerIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration

class CallTreeVisualizerComponentRegistrar: CompilerPluginRegistrar() {
    override val pluginId: String = BuildConfig.KOTLIN_PLUGIN_ID

    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(CallTreeVisualizerIrGenerationExtension())
    }
}
