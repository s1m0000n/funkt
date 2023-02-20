package funkt

sealed interface Maybe<T> {
    val isJust: Boolean
    infix fun <R> fmap(f: (T) -> R): Maybe<R>
    infix fun <R> bind(f: (T) -> Maybe<R>): Maybe<R>
    infix fun ifNil(f: (Exception?) -> T): T

    val isNil get() = !isJust
    infix fun <R> then(other: Maybe<R>) = other
    infix fun <R> inherit(v: R) = fmap { v }
}

data class Just<T>(val value: T): Maybe<T> {
    override val isJust = true
    override inline fun <R> fmap(f: (T) -> R) = Just(f(value))
    override inline fun <R> bind(f: (T) -> Maybe<R>) = f(value)
    override inline fun ifNil(f: (Exception?) -> T): T = value
}

data class Nil<T>(val reason: Exception? = null): Maybe<T> {
    override val isJust = false
    override inline fun <R> fmap(f: (T) -> R) = Nil<R>(reason) 
    override inline fun <R> bind(f: (T) -> Maybe<R>) = Nil<R>(reason)
    override inline fun ifNil(f: (Exception?) -> T): T = f(reason)
}


inline fun <T> catchMBind(f: () -> Maybe<T>): Maybe<T> = try { f() } catch(e: Exception) { Nil(e) }
inline fun <T> catchM(f: () -> T) = catchMBind { Just(f()) }
fun <T> Maybe<T>.throwNil(defaultReason: Exception = Exception()): T = ifNil { throw it ?: defaultReason }
open class DoMaybeContext {
    operator fun <T> Maybe<T>.not(): T = throwNil()
}
inline fun <T> doM(f: DoMaybeContext.() -> T): Maybe<T> = catchM { DoMaybeContext().f() }

inline fun <reified T> Any?.castM() = if (this is T) Just(this) else Nil()

inline fun <T> Iterable<Maybe<T>>.liftElemMaybe(): Maybe<List<T>> = catchM {
    map {
        it.throwNil(Exception("Nil (with no exception) found during liftElemMaybe"))
    }
}
    
inline fun <T> Sequence<Maybe<T>>.liftElemMaybe(): Maybe<Sequence<T>> = catchM {
    map {
        it.throwNil(Exception("Nil (with no exception) found during liftElemMaybe"))
    }
}
