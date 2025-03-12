package pt.isel.leic.multicloudguardian.domain.user.components

import pt.isel.leic.multicloudguardian.domain.components.Component
import pt.isel.leic.multicloudguardian.domain.utils.Either
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Success


class Email private constructor(
    val value: String
) : Component {
    companion object {
        private val EMAIL_FORMAT = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+$".toRegex()

        operator fun invoke(value: String): Either<EmailError, Email> =
            when {
                value.isBlank() -> Failure(EmailError.EmailBlank)
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


sealed class EmailError {
    data object EmailBlank : EmailError()
    data object InvalidEmail : EmailError()
}

typealias GetEmailResult = Either<EmailError, Email>