package pt.isel.leic.multicloudguardian.domain.utils

sealed class Either<out L, out R> {
    data class Left<out L>(
        val value: L,
    ) : Either<L, Nothing>()

    data class Right<out R>(
        val value: R,
    ) : Either<Nothing, R>()
}

// Functions for when using Either to represent success or failure
fun <R> success(value: R) = Either.Right(value)

fun <L> failure(error: L) = Either.Left(error)

/**
 *  Returns the value of [Either.Right] class, or throws an exception
 */
fun <L, R> Either<L, R>.get(): R =
    when (this) {
        is Failure -> throw IllegalArgumentException("Cannot get value from a failure")
        is Success -> this.value
    }

typealias Success<S> = Either.Right<S>
typealias Failure<F> = Either.Left<F>
