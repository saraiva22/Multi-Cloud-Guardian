package pt.isel.leic.multicloudguardian.domain.user

import pt.isel.leic.multicloudguardian.domain.components.Id
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Username

data class User(val id: Id, val username: Username, val email: Email, val passwordValidation: PasswordValidationInfo)

