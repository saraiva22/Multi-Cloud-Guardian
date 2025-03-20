package pt.isel.leic.multicloudguardian.service.user

import jakarta.inject.Named
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import pt.isel.leic.multicloudguardian.domain.preferences.LocationType
import pt.isel.leic.multicloudguardian.domain.preferences.PerformanceType
import pt.isel.leic.multicloudguardian.domain.preferences.PreferencesDomain
import pt.isel.leic.multicloudguardian.domain.token.Token
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.domain.user.UsersDomain
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Password
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.Id
import pt.isel.leic.multicloudguardian.domain.utils.failure
import pt.isel.leic.multicloudguardian.domain.utils.success
import pt.isel.leic.multicloudguardian.repository.TransactionManager

@Named
class UsersService(
    private val transactionManager: TransactionManager,
    private val usersDomain: UsersDomain,
    private val preferencesDomain: PreferencesDomain,
    private val clock: Clock,
) {
    fun createUser(
        username: String,
        email: String,
        password: String,
        performanceType: PerformanceType,
        locationType: LocationType,
    ): UserCreationResult {
        if (!usersDomain.isSafePassword(Password(password))) {
            return failure(UserCreationError.InsecurePassword)
        }

        val passwordValidationInfo = usersDomain.createPasswordValidationInformation(password)

        val provider = preferencesDomain.associationProvider(performanceType, locationType)

        return transactionManager.run {
            val usersRepository = it.usersRepository

            if (usersRepository.isUserStoredByUsername(Username(username))) {
                failure(UserCreationError.UserNameAlreadyExists)
            } else if (usersRepository.isEmailStoredByEmail(Email(email))) {
                failure(UserCreationError.EmailAlreadyExists)
            } else {
                val id = usersRepository.storeUser(Username(username), Email(email), passwordValidationInfo)
                usersRepository.storagePreferences(id, performanceType, locationType, provider)
                success(id)
            }
        }
    }

    fun getUserById(id: Int): UserSearchResult =
        transactionManager.run {
            val usersRepository = it.usersRepository
            val user = usersRepository.getUserById(Id(id)) ?: return@run failure(UserSearchError.UserNotFound)
            success(user)
        }

    fun createToken(
        username: String,
        password: String,
        userAgent: String,
    ): TokenCreationResult {
        if (!usersDomain.isSafePassword(Password(password))) {
            return failure(TokenCreationError.InsecurePassword)
        }
        return transactionManager.run {
            val usersRepository = it.usersRepository
            val user: User =
                usersRepository.getUserByUsername(Username(username))
                    ?: return@run failure(TokenCreationError.UsernameNotFound)
            if (!usersDomain.validatePassword(password, user.passwordValidation)) {
                return@run failure(TokenCreationError.PasswordDoesNotMatch)
            }
            val tokenValue = usersDomain.generateTokenValue()
            val now = clock.now()
            val newToken =
                Token(
                    usersDomain.createTokenValidationInformation(tokenValue),
                    user.id,
                    now,
                    now,
                    userAgent,
                )
            usersRepository.createToken(newToken, usersDomain.maxNumberOfTokensPerUser)
            success(TokenExternalInfo(tokenValue, usersDomain.getTokenExpiration(newToken)))
        }
    }

    fun getUserByToken(token: String): User? {
        if (!usersDomain.canBeToken(token)) {
            return null
        }
        return transactionManager.run {
            val usersRepository = it.usersRepository
            val tokenValidationInfo = usersDomain.createTokenValidationInformation(token)
            val userAndToken = usersRepository.getTokenByTokenValidationInfo(tokenValidationInfo)
            if (userAndToken != null && usersDomain.isTokenTimeValid(clock, userAndToken.second)) {
                usersRepository.updateTokenLastUsed(userAndToken.second, clock.now())
                userAndToken.first
            } else {
                null
            }
        }
    }

    fun revokeToken(
        userId: Id,
        token: String,
    ): TokenRevocationResult {
        val tokenValidationInfo = usersDomain.createTokenValidationInformation(token)
        return transactionManager.run {
            val res = it.usersRepository.removeTokenByValidationInfo(tokenValidationInfo)
            if (res == 0) failure(TokenRevocationError.TokenDoesNotExist)
            logger.info("Token Revoked")
            success(true)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UsersService::class.java)
    }
}
