package pt.isel.leic.multicloudguardian.http.model.user

import pt.isel.leic.multicloudguardian.domain.credentials.Credentials

data class UserCredentialsOutputModel(
    val credentialsId: Int,
    val user: UserInfoOutputModel,
    val salt: String,
    val iterations: Int,
) {
    companion object {
        fun fromDomain(cred: Credentials): UserCredentialsOutputModel =
            UserCredentialsOutputModel(
                credentialsId = cred.credentialsId.value,
                user =
                    UserInfoOutputModel(
                        id = cred.user.id.value,
                        username = cred.user.username.value,
                        email = cred.user.email.value,
                    ),
                salt = cred.salt,
                iterations = cred.iterations,
            )
    }
}
