package foo.bar

class MyClass {
    fun foo(): String = "Hello world"
}

fun box(): String {
    val result = MyClass().foo()
    return if (result == "Hello world") { "OK" } else { "Fail: $result" }
}
