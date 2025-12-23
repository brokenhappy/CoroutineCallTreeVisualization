//package com.woutwerkman.calltreevisualizer
//
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.runBlocking
//import kotlin.time.Duration.Companion.seconds
//
////TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
//// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
//fun main() {
//    runBlocking {
//        trackingCallStacks {
//            foo()
//        }.collect {
//            delay(1.seconds)
//            println(it)
//        }
//    }
//}
//
//suspend fun foo(): Int = try {
//    println("Hello foo")
//    bar()
//} finally {
//    println("Bye foo")
//}
//suspend fun bar(): Int = try {
//    println("Hello bar")
//    baz()
//} finally {
//    println("Bye bar")
//}
//suspend fun baz(): Int = try {
//    println("Hello baz")
//    42
//} finally {
//    println("Bye baz")
//}