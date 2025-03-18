package pt.isel.leic.multicloudguardian.service.user

import jakarta.inject.Named
import kotlinx.datetime.Clock
import org.springframework.stereotype.Service
import pt.isel.leic.multicloudguardian.domain.preferences.LocationType
import pt.isel.leic.multicloudguardian.domain.preferences.PerformanceType
import pt.isel.leic.multicloudguardian.domain.preferences.Preferences
import pt.isel.leic.multicloudguardian.domain.preferences.PreferencesDomain
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.domain.user.UsersDomain
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Password
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.failure
import pt.isel.leic.multicloudguardian.domain.utils.success
import pt.isel.leic.multicloudguardian.repository.TransactionManager

@Named
class UsersService(
    private val transactionManager: TransactionManager,
    private val usersDomain: UsersDomain,
    private val preferencesDomain: PreferencesDomain,
    private val clock: Clock
) {
    fun createUser(
        username: Username,
        email: Email,
        password: Password,
        performanceType: PerformanceType,
        locationType: LocationType
    ): UserCreationResult {
        if (!usersDomain.isSafePassword(password)) {
            return failure(UserCreationError.InsecurePassword)
        }

        val passwordValidationInfo = usersDomain.createPasswordValidationInformation(password.value)


        val provider = preferencesDomain.associationProvider(performanceType, locationType)

        return transactionManager.run {
            val usersRepository = it.usersRepository

            if (usersRepository.isUserStoredByUsername(username)) {
                failure(UserCreationError.UserNameAlreadyExists)
            } else if (usersRepository.isEmailStoredByEmail(email)) {
                failure(UserCreationError.EmailAlreadyExists)
            } else {
                val id = usersRepository.storeUser(username, email, passwordValidationInfo)
                usersRepository.storagePreferences(performanceType, locationType, provider)
                success(id)
            }
        }
    }


    fun getUserByToken(
        token: String
    ): User? {
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
}
















