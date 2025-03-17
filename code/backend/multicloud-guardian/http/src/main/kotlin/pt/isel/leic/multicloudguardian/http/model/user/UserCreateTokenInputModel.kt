package pt.isel.leic.multicloudguardian.http.model.user


data class UserCreateTokenInputModel(
    val username: String,
    val password: String
)
