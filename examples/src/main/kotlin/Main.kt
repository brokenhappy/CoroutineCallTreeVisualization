package com.woutwerkman.calltreevisualizer

import kotlinx.coroutines.runBlocking

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    runBlocking {
        trackingCallStacks {
            foo()
        }.collect {
            println(it)
        }
    }
}

suspend fun foo(): Int = bar()
suspend fun bar(): Int = baz()
suspend fun baz(): Int = 42