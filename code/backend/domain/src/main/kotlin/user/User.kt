package user

import components.Id
import user.components.Email

data class User(val id: Id, val username: String, val email: Email, val passwordValidation: PasswordValidationInfo )
