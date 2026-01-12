// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

class MyClass {
    fun foo(): String = "test"
}

fun test() {
    val s = MyClass().foo()
    s.<!UNRESOLVED_REFERENCE!>inc<!>() // should be an error
}
