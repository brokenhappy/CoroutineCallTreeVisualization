/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.woutwerkman.calltreevisualizer.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.declarations.createExpressionBody
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class CallStackTrackingTransformer(private val context: IrPluginContext) : IrVisitorVoid() {
    private val factory = context.irFactory
    private val stackTrackedFunction = context.referenceFunctions(
        CallableId(
            packageName = FqName("com.woutwerkman.calltreevisualizer"),
            className = null,
            callableName = Name.identifier("stackTracked"),
        ),
    ).single()

    override fun visitElement(element: IrElement) {
        super.visitElement(element)
        element.acceptChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction) {
        if (!declaration.isSuspend) return
        if (declaration.isInline) return
        val statements = when (val body = declaration.body) {
            is IrBlockBody -> body.statements.toMutableList()
            is IrExpressionBody -> mutableListOf(body.expression)
            is IrSyntheticBody, null -> return // Don't care
        }

        declaration.body = factory.createExpressionBody(
            expression = createCallExpression(
                type = declaration.returnType,
                symbol = stackTrackedFunction,
                typeArguments = listOf(declaration.returnType),
                arguments = listOf(
                    IrConstImpl.string(
                        SYNTHETIC_OFFSET,
                        SYNTHETIC_OFFSET,
                        context.irBuiltIns.stringType,
                        declaration.fqNameWhenAvailable?.asString() ?: "<anonymous>",
                    ),
                    IrFunctionExpressionImpl(
                        startOffset = SYNTHETIC_OFFSET,
                        endOffset = SYNTHETIC_OFFSET,
                        origin = IrStatementOrigin.LAMBDA,
                        type = context.irBuiltIns.suspendFunctionN(0).typeWith(declaration.returnType),
                        function = factory.buildFun {
                            startOffset = SYNTHETIC_OFFSET
                            endOffset = SYNTHETIC_OFFSET
                            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                            name = Name.special("<anonymous>")
                            visibility = DescriptorVisibilities.LOCAL
                            returnType = declaration.returnType
                            modality = Modality.FINAL
                            isSuspend = true
                        }.apply {
                            parent = declaration

                            val visitor = ReturnStatementTargetReplacer(old = declaration.symbol, new = symbol)
                            for (statement in statements) {
                                visitor.visitElement(statement)
                            }

                            this.body = this.factory.createBlockBody(
                                SYNTHETIC_OFFSET,
                                SYNTHETIC_OFFSET,
                                statements,
                            )
                        },
                    )
                ),
            )
        )
    }
}

class ReturnStatementTargetReplacer(
    private val old: IrReturnTargetSymbol,
    private val new: IrReturnTargetSymbol
) : IrVisitorVoid() {
    override fun visitElement(element: IrElement) {
        if (element is IrReturn) visitReturn(element)
        element.acceptChildrenVoid(this)
    }

    override fun visitReturn(expression: IrReturn) {
        if (expression.returnTargetSymbol == old) {
            expression.returnTargetSymbol = new
        }
        expression.acceptChildrenVoid(this)
    }
}

private fun createCallExpression(
    type: IrType,
    symbol: IrSimpleFunctionSymbol,
    extensionReceiver: IrExpression? = null,
    dispatchReceiver: IrExpression? = null,
    arguments: List<IrExpression>,
    argumentsCount: Int = arguments.size,
    typeArguments: List<IrType> = emptyList(),
    startOffset: Int = SYNTHETIC_OFFSET,
    endOffset: Int = SYNTHETIC_OFFSET,
): IrCall = IrCallImplWithShape(
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
    typeArgumentsCount = typeArguments.size,
    valueArgumentsCount = argumentsCount,
    origin = GeneratedByCallTreeVisualizer,
    superQualifierSymbol = null,
    contextParameterCount = 0,
    hasDispatchReceiver = dispatchReceiver != null,
    hasExtensionReceiver = extensionReceiver != null,
).also { call ->
    (listOfNotNull(dispatchReceiver, extensionReceiver) + arguments).forEachIndexed { index, argument ->
        call.arguments[index] = argument
    }
    typeArguments.forEachIndexed { index, typeArgument ->
        call.typeArguments[index] = typeArgument
    }
}

private val GeneratedByCallTreeVisualizer by IrStatementOriginImpl