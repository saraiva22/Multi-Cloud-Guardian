package pt.isel.leic.multicloudguardian.repository.jdbi

import kotlinx.datetime.Instant
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.slf4j.LoggerFactory
import pt.isel.leic.multicloudguardian.domain.preferences.LocationType
import pt.isel.leic.multicloudguardian.domain.preferences.PerformanceType
import pt.isel.leic.multicloudguardian.domain.provider.ProviderType
import pt.isel.leic.multicloudguardian.domain.token.Token
import pt.isel.leic.multicloudguardian.domain.token.TokenValidationInfo
import pt.isel.leic.multicloudguardian.domain.user.PasswordValidationInfo
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.Id
import pt.isel.leic.multicloudguardian.repository.UsersRepository

class JdbiUsersRepository(
    private val handle: Handle,
) : UsersRepository {
    override fun storeUser(
        username: Username,
        email: Email,
        passwordValidation: PasswordValidationInfo,
    ): Id {
        val userId =
            handle
                .createUpdate(
                    """
                    insert into dbo.Users (username, email, password_validation) values (:username, :email, :password_validation)
                    """.trimIndent(),
                ).bind("username", username.value)
                .bind("email", email.value)
                .bind("password_validation", passwordValidation.validationInfo)
                .executeAndReturnGeneratedKeys()
                .mapTo<Int>()
                .one()
        return Id(userId)
    }

    override fun isUserStoredByUsername(username: Username): Boolean =
        handle
            .createQuery("select count(*) from dbo.Users where username = :username")
            .bind("username", username.value)
            .mapTo<Int>()
            .single() == 1

    override fun isEmailStoredByEmail(email: Email): Boolean =
        handle
            .createQuery("select count(*) from dbo.Users where email = :email")
            .bind("email", email.value)
            .mapTo<Int>()
            .single() == 1

    override fun getUserById(id: Id): User? =
        handle
            .createQuery("select * from dbo.Users where id = :id")
            .bind("id", id.value)
            .mapTo<User>()
            .singleOrNull()

    override fun getUserByEmail(email: Email): User? =
        handle
            .createQuery("select * from dbo.Users where email = :email")
            .bind("email", email.value)
            .mapTo<User>()
            .singleOrNull()

    override fun storagePreferences(
        userId: Id,
        performanceType: PerformanceType,
        locationType: LocationType,
        providerType: ProviderType,
    ) {
        handle
            .createUpdate(
                """
                insert into dbo.Preferences (user_id,performance,location,storage_provider) values (:user_id, :performance, :location, :storage_provider)
                """.trimIndent(),
            ).bind("user_id", userId.value)
            .bind("performance", performanceType.ordinal)
            .bind("location", locationType.ordinal)
            .bind("storage_provider", providerType.ordinal)
            .execute()
    }

    override fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>? =
        handle
            .createQuery(
                """
                select id, username, email, password_validation, token_validation, created_at, last_used_at, user_agent
                from dbo.Users as users 
                inner join dbo.Tokens as tokens 
                on users.id = tokens.user_id
                where token_validation = :validation_information
            """,
            ).bind("validation_information", tokenValidationInfo.validationInfo)
            .mapTo<UserAndTokenModel>()
            .singleOrNull()
            ?.userAndToken

    override fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    ) {
        handle
            .createUpdate(
                """
                update dbo.Tokens
                set last_used_at = :last_used_at
                where token_validation = :validation_information
                """.trimIndent(),
            ).bind("last_used_at", now.epochSeconds)
            .bind("validation_information", token.tokenValidationInfo.validationInfo)
            .execute()
    }

    override fun createToken(
        token: Token,
        maxTokens: Int,
    ) {
        val deletions =
            handle
                .createUpdate(
                    """
                    delete from dbo.Tokens 
                    where user_id = :user_id 
                        and token_validation in (
                            select token_validation from dbo.Tokens where user_id = :user_id 
                                order by last_used_at desc offset :offset
                        )
                    """.trimIndent(),
                ).bind("user_id", token.userId.value)
                .bind("offset", maxTokens - 1)
                .execute()

        logger.info("{} tokens deleted when creating new token", deletions)

        handle
            .createUpdate(
                """
                insert into dbo.Tokens(user_id, token_validation, user_agent, created_at, last_used_at) 
                values (:user_id, :token_validation, :user_agent, :created_at, :last_used_at)
                """.trimIndent(),
            ).bind("user_id", token.userId.value)
            .bind("token_validation", token.tokenValidationInfo.validationInfo)
            .bind("user_agent", token.userAgent)
            .bind("created_at", token.createdAt.epochSeconds)
            .bind("last_used_at", token.lastUsedAt.epochSeconds)
            .execute()
    }

    override fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int =
        handle
            .createUpdate(
                """
                delete from dbo.Tokens
                where token_validation = :validation_information
                """.trimIndent(),
            ).bind("validation_information", tokenValidationInfo.validationInfo)
            .execute()

    override fun getUserByUsername(username: Username): User? =
        handle
            .createQuery(
                """
                select * from dbo.Users where username = :username
                """.trimIndent(),
            ).bind("username", username.value)
            .mapTo<User>()
            .singleOrNull()

    private data class UserAndTokenModel(
        val id: Int,
        val username: String,
        val email: String,
        val userAgent: String,
        val passwordValidation: PasswordValidationInfo,
        val tokenValidation: TokenValidationInfo,
        val createdAt: Long,
        val lastUsedAt: Long,
    ) {
        val userAndToken: Pair<User, Token>
            get() =
                Pair(
                    User(Id(id), Username(username), Email(email), passwordValidation),
                    Token(
                        tokenValidation,
                        Id(id),
                        Instant.fromEpochSeconds(createdAt),
                        Instant.fromEpochSeconds(lastUsedAt),
                        userAgent,
                    ),
                )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JdbiUsersRepository::class.java)
    }
}
