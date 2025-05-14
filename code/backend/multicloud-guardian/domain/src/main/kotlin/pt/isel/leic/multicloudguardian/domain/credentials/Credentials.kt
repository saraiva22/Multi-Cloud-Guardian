package pt.isel.leic.multicloudguardian.domain.credentials

import pt.isel.leic.multicloudguardian.domain.user.UserInfo
import pt.isel.leic.multicloudguardian.domain.utils.Id

/**
 * Represents the credentials of a user.
 * @property credentialsId The unique identifier for the credentials.
 * @property user The unique identifier for the user.
 * @property salt The salt used for hashing the password.
 * @property iterations The number of iterations used for hashing the password.
 */
data class Credentials(
    val credentialsId: Id,
    val user: UserInfo,
    val salt: String,
    val iterations: Int,
)
