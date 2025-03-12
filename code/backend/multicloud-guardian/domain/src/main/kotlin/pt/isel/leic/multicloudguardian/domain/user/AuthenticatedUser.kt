package pt.isel.leic.multicloudguardian.domain.user

class AuthenticatedUser(
    val user: User,
    val token: String
)