package pt.isel.leic.multicloudguardian.repository.jdbi

import kotlinx.datetime.Instant
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.slf4j.LoggerFactory
import pt.isel.leic.multicloudguardian.domain.components.Id
import pt.isel.leic.multicloudguardian.domain.preferences.LocationType
import pt.isel.leic.multicloudguardian.domain.preferences.PerformanceType
import pt.isel.leic.multicloudguardian.domain.provider.ProviderType
import pt.isel.leic.multicloudguardian.domain.token.Token
import pt.isel.leic.multicloudguardian.domain.token.TokenValidationInfo
import pt.isel.leic.multicloudguardian.domain.user.PasswordValidationInfo
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.domain.user.UserAndToken
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.repository.UsersRepository
import pt.isel.leic.multicloudguardian.repository.jdbi.model.JdbiIdModel
import pt.isel.leic.multicloudguardian.repository.jdbi.model.user.JdbiUserAndTokenModel

class JdbiUsersRepository(private val handle: Handle) : UsersRepository {

    override fun storeUser(
        username: Username,
        email: Email,
        passwordValidation: PasswordValidationInfo,
    ): Id {
        val userId = handle.createUpdate(
            """
            insert into dbo.Users (username, email, password_validation) values (:username, :email, :password_validation)
            """.trimIndent()
        )
            .bind("username", username.value)
            .bind("email", email.value)
            .bind("password_validation", passwordValidation.validationInfo)
            .executeAndReturnGeneratedKeys()
            .mapTo<JdbiIdModel>()
            .one()
            .toDomainModel()
        return userId
    }

    override fun isUserStoredByUsername(username: Username): Boolean =
        handle.createQuery("select count(*) from dbo.Users where username = :username")
            .bind("username", username.value)
            .mapTo<Int>()
            .single() == 1


    override fun isEmailStoredByEmail(email: Email): Boolean =
        handle.createQuery("select count(*) from dbo.Users email = :email")
            .bind("email", email.value)
            .mapTo<Int>()
            .single() == 1

    override fun storagePreferences(
        performanceType: PerformanceType,
        locationType: LocationType,
        providerType: ProviderType
    ) {
        handle.createUpdate(
            """
            insert into dbo.Preferences (performance,location,storage_provider) values (:performance, location, storage_provider)
        """.trimIndent()
        )
            .bind("performance", performanceType.performance)
            .bind("location", locationType.location)
            .bind("storage_provider", providerType.provider)
            .execute()
    }

    override fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): UserAndToken? =
        handle.createQuery(
            """
                select id, username, email, password_validation, token_validation, created_at, last_used_at
                from dbo.Users as users 
                inner join dbo.Tokens as tokens 
                on users.id = tokens.user_id
                where token_validation = :validation_information
            """,
        )
            .bind("validation_information", tokenValidationInfo.validationInfo)
            .mapTo<JdbiUserAndTokenModel>()
            .singleOrNull()
            ?.toDomainModel()

    override fun updateTokenLastUsed(token: Token, now: Instant) {
        handle.createUpdate(
            """
               update dbo.Tokens
               set last_used_at = :last_used_at
               where token_validation = :validation_inform
           """.trimIndent()
        )
            .bind("last_used_at", now.epochSeconds)
            .bind("validation_information", token.tokenValidationInfo.validationInfo)
            .execute()
    }

    override fun createToken(token: Token, maxTokens: Int) {
        val deletions = handle.createUpdate(
            """
            delete from dbo.Tokens 
            where user_id = :user_id 
                and token_validation in (
                    select token_validation from dbo.Tokens where user_id = :user_id 
                        order by last_used_at desc offset :offset
                )
            """.trimIndent(),
        )
            .bind("user_id", token.userId.value)
            .bind("offset", maxTokens - 1)
            .execute()



        logger.info("{} tokens deleted when creating new token", deletions)

        handle.createUpdate(
            """
                insert into dbo.Tokens(user_id, token_validation, created_at, last_used_at) 
                values (:user_id, :token_validation, :created_at, :last_used_at)
            """.trimIndent()
        )
            .bind("user_id", token.userId.value)
            .bind("token_validation", token.tokenValidationInfo.validationInfo)
            .bind("created_at", token.createdAt.epochSeconds)
            .bind("last_used_at", token.lastUsedAt.epochSeconds)
            .execute()
    }

    override fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int {
        TODO("Not yet implemented")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JdbiUsersRepository::class.java)
    }
}