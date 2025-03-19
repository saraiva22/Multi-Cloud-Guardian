package pt.isel.leic.multicloudguardian.domain.user.components

// Constants
private const val MAX_PASSWORD_LENGTH = 45
private const val MIN_PASSWORD_LENGTH = 10

/**
 * Component that represents a password.
 * A password must have at least one lowercase letter, one uppercase letter, one digit, one special character
 */
data class Password(
    val value: String,
) {
    companion object {
        private val PASSWORD_FORMAT =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\$@\$!%*?_&#])[A-Za-z\\d\$@\$!%*?_&#]{$MIN_PASSWORD_LENGTH,$MAX_PASSWORD_LENGTH}\$"
                .toRegex()

        /**
         * Checks if the password is safe.
         * A safe password must have at least one lowercase letter, one uppercase letter, one digit, one special character
         * and must have a length between [MIN_PASSWORD_LENGTH] and [MAX_PASSWORD_LENGTH].
         */
        private fun isSafePassword(value: String) = value.matches(PASSWORD_FORMAT)

        @Suppress("ktlint:standard:property-naming")
        const val minLength = MIN_PASSWORD_LENGTH

        @Suppress("ktlint:standard:property-naming")
        const val maxLength = MAX_PASSWORD_LENGTH
    }

    fun isSafe(password: Password) = isSafePassword(password.value)
}
