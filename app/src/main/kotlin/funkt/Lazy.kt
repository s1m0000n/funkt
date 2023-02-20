package funkt

typealias Lazy<T> = () -> T
fun<T> T.toLazy() = { this }
inline infix fun <T, R> Lazy<T>.fmap(crossinline f: (T) -> R): Lazy<R> = { f(this()) }
inline infix fun <T, R> Lazy<T>.bind(crossinline f: (T) -> Lazy<R>): Lazy<R> = { f(this())() }
inline infix fun <T, R> Lazy<T>.then(crossinline f: Lazy<R>): Lazy<R> = { this(); f() }

fun <T> Maybe<Lazy<T>>.liftLazy(): Lazy<Maybe<T>> = { fmap { it() } }
fun <T> Iterable<Lazy<T>>.liftElemLazy() = { map { it() } }
fun <T> Sequence<Lazy<T>>.liftElemLazy(): Lazy<Sequence<T>> = { map { it() } }
