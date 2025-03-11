package user

import components.Id
import user.components.Email
import user.components.Username

data class User(val id: Id, val username: Username, val email: Email, val passwordValidation: PasswordValidationInfo)
