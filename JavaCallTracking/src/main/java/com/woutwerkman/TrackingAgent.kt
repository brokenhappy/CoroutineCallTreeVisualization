package com.woutwerkman

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.Origin
import net.bytebuddy.implementation.bind.annotation.RuntimeType
import net.bytebuddy.implementation.bind.annotation.SuperCall
import net.bytebuddy.matcher.ElementMatchers.*
import java.lang.instrument.Instrumentation
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean

object TrackingAgent {
    private val installed = AtomicBoolean(false)

    @JvmStatic
    fun premain(agentArgs: String?, inst: Instrumentation) {
        if (!installed.compareAndSet(false, true)) return
        System.setProperty("net.bytebuddy.experimental", "true")
        AgentBuilder.Default()
            .ignore(nameStartsWith<TypeDescription>("java.")
                .or(nameStartsWith("javax."))
                .or(nameStartsWith("sun."))
                .or(nameStartsWith("com.sun."))
                .or(nameStartsWith("jdk."))
                .or(nameStartsWith("net.bytebuddy."))
                .or(nameStartsWith("kotlin."))
                .or(nameContains("TrackingAgent"))
                .or(nameContains("TrackingWrapperKt"))
                .or(nameContains("\$auxiliary$"))
                .or(nameStartsWith("org.gradle."))
                .or(nameStartsWith("worker.org.gradle."))
                .or(nameStartsWith("org.junit."))
                .or(nameStartsWith("org.slf4j."))
                .or(nameStartsWith("ch.qos.logback."))
                .or(nameStartsWith("com.intellij."))
                .or(nameStartsWith("com.esotericsoftware.kryo."))
                .or(nameStartsWith("net.rubygrapefruit.platform."))
                .or(nameStartsWith("org.apache.maven."))
                .or(nameStartsWith("com.google.common."))
                .or(nameStartsWith("org.codehaus.groovy."))
                .or(nameStartsWith("com.sun.proxy."))
                .or(nameStartsWith("org.opentest4j."))
                .or(nameStartsWith("org.apache.commons."))
                .or(nameStartsWith("kotlinx."))
                .or(nameStartsWith("com.woutwerkman.calltreevisualizer.gui."))
                .or(nameStartsWith("com.woutwerkman.calltreevisualizer.coroutineintegration."))
                .or(nameStartsWith("androidx.compose.")))
            .type(any())
            .transform { builder, _, _, _, _ ->
                builder.method(isMethod<MethodDescription>()
                    .and(not(isAbstract()))
                    .and(not(isNative()))
                    .and(not(isSynthetic()))
                    .and(not(isDefaultConstructor()))
                    .and(not(isAnnotatedWith(nameEndsWith("NonTrackedJava"))))
                .and(not(named<MethodDescription>("track")
                    .and(isDeclaredBy(isSubTypeOf(StackTrackerJava::class.java)))))
                )
                .intercept(MethodDelegation.to(Interceptor::class.java))
            }
            .installOn(inst)
    }

    object Interceptor {
        @JvmStatic
        @RuntimeType
        fun intercept(
            @Origin method: String,
            @SuperCall callable: Callable<*>
        ): Any? = stackTrackedJava(method) { callable.call() }
    }
}
