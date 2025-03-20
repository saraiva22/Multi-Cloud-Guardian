package pt.isel.leic.multicloudguardian.domain.user

import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.Id

data class UserInfo(
    val id: Id,
    val username: Username,
    val email: Email,
)
