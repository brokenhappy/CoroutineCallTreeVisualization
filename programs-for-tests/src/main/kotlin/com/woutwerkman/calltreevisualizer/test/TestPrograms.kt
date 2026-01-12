package com.woutwerkman.calltreevisualizer.test

import kotlinx.coroutines.*
import kotlin.time.Duration

suspend fun simpleCall() {
    foo()
}

suspend fun foo() {
    bar()
}

suspend fun bar() {
    yield()
}

suspend fun branchingCall() {
    coroutineScope {
        launch { foo() }
        launch { bar() }
    }
}

suspend fun sequentialBranchingCall() {
    foo()
    bar()
}

suspend fun throwingCall() {
    foo()
    error("Boom")
}

suspend fun foobs(bool: Boolean) {
    if (bool) error("Aaaah!")
    yield()
}

suspend fun foobsForever() {
    while (true) {
        yield()
    }
}

suspend fun persistentBranchingCall() {
    coroutineScope {
        launch { foobsForever() }
        launch { foobsForever() }
    }
}

suspend fun cancellingCall() {
    coroutineScope {
        val job = launch {
            foobsForever()
        }
        yield()
        job.cancelAndJoin()
    }
}

suspend fun trackedDelay(delay: Duration) {
    delay(delay)
}
