package user.components

import components.Component
import components.UsernameError
import utils.Either
import utils.Failure
import utils.Success


private const val MAX_USERNAME_LENGTH = 25
private const val MIN_USERNAME_LENGTH = 5


class Username private constructor(
    val value: String
) : Component {
    companion object {
        operator fun invoke(value: String): Either<UsernameError, Username> =
            when {
                value.isBlank() -> Failure(UsernameError.UsernameBlank)
                value.length !in MIN_USERNAME_LENGTH..MAX_USERNAME_LENGTH -> Failure(UsernameError.InvalidLength)
                else -> Success(Username(value))
            }

        const val minLength = MIN_USERNAME_LENGTH
        const val maxLength = MAX_USERNAME_LENGTH
    }

    override fun toString(): String = value

    override fun hashCode(): Int = value.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Username) return false
        if (value != other.value) return false
        return true
    }
}