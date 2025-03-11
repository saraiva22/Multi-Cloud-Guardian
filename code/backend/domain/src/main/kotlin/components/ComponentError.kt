package components

import user.components.Password
import user.components.Username
import utils.Either

sealed class IdError(open val value: Int) {
    data class InvalidIdError(override val value: Int) : IdError(value)
}

typealias GetIdResult = Either<IdError, Id>

sealed class EmailError {
    data object EmailBlank : EmailError()
    data object InvalidEmail : EmailError()
}

sealed class PositiveValueError {
    data class InvalidPositiveValue(val value: Int) : PositiveValueError()
}

sealed class UsernameError {
    data object InvalidLength : UsernameError()
    data object UsernameBlank : UsernameError()
}

typealias GetUsernameResult = Either<UsernameError, Username>

sealed class PasswordError {
    data object PasswordNotSafe : PasswordError()
    data object PasswordBlack : PasswordError()
    data object InvalidLength : PasswordError()
}

typealias GetPasswordResult = Either<PasswordError, Password>
