package pt.isel.leic.multicloudguardian.domain.sse

import pt.isel.leic.multicloudguardian.domain.user.UserInfo

data class UserInfoOutput(
    val id: Int,
    val username: String,
    val email: String,
) {
    companion object {
        fun fromDomain(user: UserInfo): UserInfoOutput =
            UserInfoOutput(
                user.id.value,
                user.username.value,
                user.email.value,
            )
    }
}
