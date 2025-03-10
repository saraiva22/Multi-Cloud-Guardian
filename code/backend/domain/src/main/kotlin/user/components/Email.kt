package user.components

import components.Component
import components.EmailError
import utils.Either
import utils.Failure
import utils.Success


class Email private constructor(
    val value: String
) : Component {
    companion object {
        private val EMAIL_FORMAT = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+$".toRegex()

        operator fun invoke(value: String): Either<EmailError, Email> =
            when {
                value.isBlank() -> Failure(EmailError.BlankEmail)
                !EMAIL_FORMAT.matches(value) -> Failure(EmailError.InvalidEmail)
                else -> Success(Email(value))
            }

    }

    override fun hashCode(): Int = value.hashCode()


    override fun toString(): String = value


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Email) return false

        if (value != other.value) return false

        return true
    }
}
