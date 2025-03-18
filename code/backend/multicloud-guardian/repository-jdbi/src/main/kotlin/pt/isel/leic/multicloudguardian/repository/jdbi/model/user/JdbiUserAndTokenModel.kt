package pt.isel.leic.multicloudguardian.repository.jdbi.model.user

import kotlinx.datetime.Instant
import org.jdbi.v3.core.mapper.reflect.ColumnName
import pt.isel.leic.multicloudguardian.domain.components.Id
import pt.isel.leic.multicloudguardian.domain.token.Token
import pt.isel.leic.multicloudguardian.domain.token.TokenValidationInfo
import pt.isel.leic.multicloudguardian.domain.user.PasswordValidationInfo
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.domain.user.UserAndToken
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.get
import pt.isel.leic.multicloudguardian.repository.jdbi.model.JdbiModel

class JdbiUserAndTokenModel(
    val id: Int,
    val username: String,
    val email: String,
    @ColumnName("password_validation")
    val passwordValidation: String,
    @ColumnName("token_validation")
    val tokenValidation: String,
    @ColumnName("created_at")
    val createdAt: Long,
    @ColumnName("last_used_at")
    val lastUsedAt: Long
) : JdbiModel<UserAndToken> {
    override fun toDomainModel(): UserAndToken {
        val userAndToken: Pair<User, Token> = Pair(
            User(
                id = Id(id).get(),
                username = Username(username).get(),
                email = Email(email).get(),
                passwordValidation = PasswordValidationInfo(passwordValidation)
            ),
            Token(
                tokenValidationInfo = TokenValidationInfo(tokenValidation),
                userId = Id(id).get(),
                createdAt = Instant.fromEpochSeconds(createdAt),
                lastUsedAt = Instant.fromEpochSeconds(lastUsedAt),
                userAgent = "default"
            )
        )
        return userAndToken
    }
}