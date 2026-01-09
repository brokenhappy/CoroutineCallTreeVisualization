@file:Suppress("KotlinUnreachableCode", "unused")

package com.woutwerkman.calltreevisualizer

import kotlinhax.shadowroutines.CoroutineScope
import kotlinhax.shadowroutines.coroutineScope
import kotlinhax.shadowroutines.launch
import java.io.BufferedReader


suspend fun mutableVsConfinedConcurrencyComparison() {
    // region Redundant mutability

    data class User(var name: String)

    val user = User("<unknown>")
    user.name = "John"

    val users = mutableListOf<User>()
    users.add(user)

    processUsers(users)

    // region Confined Concurrency counterparts
    var a = 0
    coroutineScope {
        launch {
            a++
        }
    }
    // endregion

    // endregion

    // TODO: Make tier list of best mutability to global, and with confined concurrency

    // region Push mutability up
    // region Scaffolding
    val allUsers = mutableListOf<User>()
    fun parseUser(json: String): User = TODO()
    fun parseUserAndThenAddToList(json: String, list: MutableList<User>) {
        list.add(parseUser(json))
    }
    suspend fun register(user: User): Unit = TODO()
    fun registerUserOn(scope: CoroutineScope, user: User) {
        scope.launch { register(user) }
    }
    val john = parseUser("""{"name": "John"}""")
    val jane = parseUser("""{"name": "Jane"}""")
    // endregion

    parseUserAndThenAddToList("""{"name": "John"}""", allUsers)
    parseUserAndThenAddToList("""{"name": "Jane"}""", allUsers)

    //region Confined Concurrency Counterpart

    coroutineScope {
        registerUserOn(scope = this, user = john)
        registerUserOn(scope = this, user = jane)
    }

    //endregion
    // endregion


    // region Wrap suboptimal functions

    // region Scaffolding
    fun readUsersFromStreamIntoList(fileStream: BufferedReader, list: MutableList<User>): Unit = TODO()
    // endregion

    fun readUsersFrom(bufferedReader: BufferedReader): List<User> {
        readUsersFromStreamIntoList(bufferedReader, TODO())
    }


    //region Confined Concurrency Counterpart

    suspend fun registerUser(user: User) {
        registerUserOn(TODO(), user)
    }

    //endregion
    // endregion
}

// region Scaffolding
fun processUsers(user: Any) {}
fun doSomething() {}
// endregion