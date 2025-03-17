package pt.isel.leic.multicloudguardian.domain.user.components

import pt.isel.leic.multicloudguardian.domain.components.Component
import pt.isel.leic.multicloudguardian.domain.utils.Either
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Success

// Constants
private const val MAX_PASSWORD_LENGTH = 45
private const val MIN_PASSWORD_LENGTH = 10

/**
 * Component that represents a password.
 * A password must have at least one lowercase letter, one uppercase letter, one digit, one special character
 */
class Password private constructor(
    val value: String
) : Component {
    companion object {
        private val PASSWORD_FORMAT =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\$@\$!%*?_&#])[A-Za-z\\d\$@\$!%*?_&#]{$MIN_PASSWORD_LENGTH,$MAX_PASSWORD_LENGTH}\$".toRegex()

        /**
         * Checks if the password is safe.
         * A safe password must have at least one lowercase letter, one uppercase letter, one digit, one special character
         * and must have a length between [MIN_PASSWORD_LENGTH] and [MAX_PASSWORD_LENGTH].
         */
        private fun isSafePassword(value: String) = value.matches(PASSWORD_FORMAT)

        operator fun invoke(value: String): Either<PasswordError, Password> =
            when {
                value.isBlank() -> Failure(PasswordError.PasswordBlack)
                value.length !in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH -> Failure(PasswordError.InvalidLength)
                isSafePassword(value) -> Failure(PasswordError.PasswordNotSafe)
                else -> Success(Password(value))
            }

        const val minLength = MIN_PASSWORD_LENGTH
        const val maxLength = MAX_PASSWORD_LENGTH

    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Password) return false
        if (value != other.value) return false
        return true
    }

    fun isSafe(password: Password) = isSafePassword(password.value)

}


sealed class PasswordError {
    data object PasswordNotSafe : PasswordError()
    data object PasswordBlack : PasswordError()
    data object InvalidLength : PasswordError()
}

typealias GetPasswordResult = Either<PasswordError, Password>
