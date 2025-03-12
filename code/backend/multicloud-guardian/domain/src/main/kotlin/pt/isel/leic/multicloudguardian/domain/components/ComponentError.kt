package pt.isel.leic.multicloudguardian.domain.components


import pt.isel.leic.multicloudguardian.domain.utils.Either

sealed class IdError(open val value: Int) {
    data class InvalidIdError(override val value: Int) : IdError(value)
}

typealias GetIdResult = Either<IdError, Id>

sealed class PositiveValueError {
    data class InvalidPositiveValue(val value: Int) : PositiveValueError()
}

typealias PositiveValueResult = Either<PositiveValueError,PositiveValue>

