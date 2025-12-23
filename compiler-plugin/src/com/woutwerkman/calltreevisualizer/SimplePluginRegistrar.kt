package com.woutwerkman.calltreevisualizer

import com.woutwerkman.calltreevisualizer.fir.SimpleClassGenerator
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class SimplePluginRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::SimpleClassGenerator
    }
}
