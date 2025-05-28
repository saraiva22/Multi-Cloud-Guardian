package pt.isel.leic.multicloudguardian.service.user

import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pt.isel.leic.multicloudguardian.domain.preferences.LocationType
import pt.isel.leic.multicloudguardian.domain.preferences.PerformanceType
import pt.isel.leic.multicloudguardian.domain.preferences.PreferencesDomain
import pt.isel.leic.multicloudguardian.domain.token.Token
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.domain.user.UserStorageInfo
import pt.isel.leic.multicloudguardian.domain.user.UsersDomain
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Password
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.Id
import pt.isel.leic.multicloudguardian.domain.utils.PageResult
import pt.isel.leic.multicloudguardian.domain.utils.failure
import pt.isel.leic.multicloudguardian.domain.utils.success
import pt.isel.leic.multicloudguardian.repository.TransactionManager

@Service
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
        salt: String,
        iterations: Int,
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
                val id =
                    usersRepository.storeUser(
                        Username(username),
                        Email(email),
                        salt,
                        iterations,
                        passwordValidationInfo,
                    )
                usersRepository.storagePreferences(id, performanceType, locationType, provider)
                success(id)
            }
        }
    }

    fun getUserStorageById(id: Int): UserStorageSearchResult =
        transactionManager.run {
            val usersRepository = it.usersRepository
            val user = usersRepository.getUserStorageById(Id(id)) ?: return@run failure(UserSearchError.UserNotFound)
            success(user)
        }

    fun getUserById(userId: Int): UserSearchResult =
        transactionManager.run {
            val usersRepository = it.usersRepository
            val user = usersRepository.getUserById(Id(userId)) ?: return@run failure(UserSearchError.UserNotFound)
            success(user)
        }

    fun getUserByUsername(username: Username): UserStorageSearchResult =
        transactionManager.run {
            val usersRepository = it.usersRepository
            val user =
                usersRepository.getUserInfoByUsername(username) ?: return@run failure(UserSearchError.UserNotFound)
            success(user)
        }

    fun getUserCredentialsById(userId: Id): UserCredentialsResult =
        transactionManager.run {
            val usersRepository = it.usersRepository
            val credentials =
                usersRepository.getUserCredentialsById(userId)
                    ?: return@run failure(UserCredentialsError.UserNotFound)
            success(credentials)
        }

    fun getStorageDetailsByUser(userId: Id): UserStorageDetailsResult =
        transactionManager.run {
            val usersRepository = it.usersRepository
            usersRepository.getUserById(userId) ?: return@run failure(UserStorageDetailsError.UserNotFound)
            val storageDetails =
                usersRepository.getUserStorageDetails(userId)
            success(storageDetails)
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
            val user =
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

    fun searchUsers(
        username: String,
        limit: Int,
        page: Int,
        sort: String,
    ): PageResult<UserStorageInfo> =
        transactionManager.run {
            val usersRep = it.usersRepository
            val offset = page * limit
            val users = usersRep.searchUsers(username, limit, offset, sort)
            val totalElements = usersRep.countUsersByUsername(username)
            PageResult.fromPartialResult(users, totalElements, limit, offset)
        }

    fun revokeToken(token: String): TokenRevocationResult {
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
