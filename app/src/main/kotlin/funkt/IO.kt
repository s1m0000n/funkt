package funkt

class IO<T>(private val value: Lazy<Maybe<T>> = { Nil() }) {
    infix fun <R> fmap(f: (T) -> R): IO<R> = IO(value.fmap { it.fmap(f) })
    infix fun <R> bind(f: (T) -> IO<R>): IO<R> = IO(value.bind { maybe -> { maybe.bind { f(it).value() } } } )
    infix fun <R> fmapMaybe(f: (T) -> Maybe<R>): IO<R> = IO(value.fmap { it.bind(f) })
    infix fun <R> then(nextAction: IO<R>): IO<R> = IO { value().let { nextAction() } }
    infix fun <R> also(nextAction: IO<R>): IO<T> = IO { value().also { nextAction() } }
    operator fun invoke() = value()
}

interface Reader { fun <T> read(): IO<T> }
interface RReader<T> { fun read(): IO<T> }
interface Writer { fun <T> write(value: T): IO<T> }
interface RWriter<T> { fun write(value: T): IO<T> }

object stdio: RReader<String>, Writer {
    override fun read(): IO<String> = IO {
        catchM { readln() }
    }
    override fun <T> write(value: T): IO<T> = IO {
        catchM { print(value); value }
    }
    fun <T> writeln(value: T) = write(value.toString() + "\n") then value.toIO()
}

open class DoIOContext(): DoMaybeContext() {
    operator fun <T> IO<T>.not(): T = invoke().throwNil()
}
fun <T> doIO(f: DoIOContext.() -> T): IO<T> = IO { catchM { DoIOContext().f() } }


// pure functional mutable state
class Mut<T>(private var variable: Maybe<T> = Nil(NullPointerException("Value not initialized"))): RReader<T>, RWriter<T> {
    val isNil get() = IO { Just(variable.isNil) }
    override fun read(): IO<T> = IO { variable }
    override fun write(value: T): IO<T> = writeM(Just(value))
    fun writeM(value: Maybe<T>): IO<T> = IO {
        variable = value
        variable
    }
}

class DoMutContext(): DoIOContext() {
    fun <T> Mut<T>.get(): T = !read()
    infix fun <T> Mut<T>.set(value: T) = !write(value)
    fun <T> Mut<T>.copy() = mut(get())
    fun <T> Mut<T>.move() = mut(!copy().read().also(writeM(Nil(NullPointerException("Value has been moved")))))

    operator fun <T> Mut<T>.not() = get()
    operator fun <T> Mut<T>.invoke(valueProvider: (T) -> T) = set(valueProvider(get()))
    operator fun <T> Mut<T>.unaryPlus() = copy()
    operator fun <T> Mut<T>.unaryMinus() = move()
}

fun <T> doMut(f: DoMutContext.() -> T) = IO { catchM { DoMutContext().f() } }
fun <T> mut(initialValue: T? = null) = Mut(initialValue?.let(::Just) ?: Nil())

// minimal batteries:
fun <T1, R> fmap(io1: IO<T1>, f: (T1) -> R): IO<R> = io1.fmap(f)
fun <T1, T2, R> fmap(io1: IO<T1>, io2: IO<T2>, f: (T1, T2) -> R): IO<R> = io1.bind { v1 -> io2.fmap { v2 -> f(v1, v2) } }
fun <T1, T2, T3, R> fmap(io1: IO<T1>, io2: IO<T2>, io3: IO<T3>, f: (T1, T2, T3) -> R): IO<R> = io1.bind { v1 -> io2.bind { v2 -> io3.fmap { v3 -> f(v1, v2, v3) } } }

fun IO<String>.toInt(): IO<Int> = fmapMaybe { catchM(it::toInt) }
fun IO<String>.toFloat(): IO<Float> = fmapMaybe { catchM(it::toFloat) }
fun IO<String>.toDouble(): IO<Double> = fmapMaybe { catchM(it::toDouble) }
fun IO<String>.toChar(): IO<Char> = fmapMaybe { s -> catchM { s[0] } }

fun <T> Lazy<Maybe<T>>.toIO(): IO<T> = IO(this)
fun <T> Maybe<Lazy<T>>.toIO() = liftLazy().toIO()
@JvmName("toIOT")
fun <T> Maybe<T>.toIO() = toLazy().toIO()
@JvmName("toIOT")
fun <T> Lazy<T>.toIO() = fmap { Just(it) }.toIO()
fun <T> T.toIO() = let { Just(it) }.toLazy().toIO()

fun <T> Iterable<IO<T>>.liftElemIO() =
    fold(IO(Just(emptyList<T>()).toLazy())) { ioAcc, ioElem ->
        fmap(ioAcc, ioElem) { acc, elem -> acc + elem }
    }
fun <T> Sequence<IO<T>>.liftElemIO(): IO<Sequence<T>> = IO { catchM {
    map {
        it().throwNil(Exception("Nil (with no default exception) found during liftElemIO"))
    }    
}}

