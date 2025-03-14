package pt.isel.leic.multicloudguardian.domain.user

import pt.isel.leic.multicloudguardian.domain.components.Id
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Username

/**
 * Represents a user.
 * @property id The Id of the user.
 * @property username The username of the user.
 * @property email The email of the user.
 * @property passwordValidation The information that can be used to validate the password of the user.
 */
data class User(val id: Id, val username: Username, val email: Email, val passwordValidation: PasswordValidationInfo)

