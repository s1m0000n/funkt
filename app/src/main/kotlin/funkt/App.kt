package funkt

fun main() {
    doMut {
        val a = mut(0)
        val b = -a
        !b to !a.isNil
    }().let(::println)
}
