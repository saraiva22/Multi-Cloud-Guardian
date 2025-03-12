package pt.isel.leic.multicloudguardian.domain.user

import pt.isel.leic.multicloudguardian.domain.components.Id
import pt.isel.leic.multicloudguardian.domain.user.components.Username

data class UserInfo(
    val id: Id,
    val username: Username,
    val email: String
)
