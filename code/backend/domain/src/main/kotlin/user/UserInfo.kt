package user

import components.Id
import user.components.Username

data class UserInfo(
    val id: Id,
    val username: Username,
    val email: String
)
