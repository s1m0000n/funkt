package funkt

sealed interface Either<L, R> {
    val isLeft: Boolean
    val maybeLeft: Maybe<L>
    val maybeRight: Maybe<R>
    infix fun <L2> fmapL(f: (L) -> L2): Either<L2, R>
    infix fun <R2> fmapR(f: (R) -> R2): Either<L, R2>

    val isRight get() = !isLeft
    infix fun <L2, R2> then(other: Either<L2, R2>): Either<L2, R2> = other
    infix fun <L2> inheritL(other: L2) = fmapL { other }
    infix fun <R2> inheritR(other: R2) = fmapR { other }
}

data class Left<L, R>(val value: L): Either<L, R> {
    override val isLeft = true
    override val maybeLeft get() = Just(value)
    override val maybeRight get() = Nil<R>()
    override fun <L2> fmapL(f: (L) -> L2) = Left<L2, R>(f(value))
    override fun <R2> fmapR(f: (R) -> R2) = Left<L, R2>(value)
}

data class Right<L, R>(val value: R): Either<L, R> {
    override val isLeft = false 
    override val maybeLeft get() = Nil<L>() 
    override val maybeRight get() = Just(value)
    override fun <L2> fmapL(f: (L) -> L2) = Right<L2, R>(value)
    override fun <R2> fmapR(f: (R) -> R2) = Right<L, R2>(f(value))
}
