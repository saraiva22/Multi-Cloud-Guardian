package pt.isel.leic.multicloudguardian.repository

import kotlinx.datetime.Instant
import pt.isel.leic.multicloudguardian.domain.credentials.Credentials
import pt.isel.leic.multicloudguardian.domain.preferences.LocationType
import pt.isel.leic.multicloudguardian.domain.preferences.PerformanceType
import pt.isel.leic.multicloudguardian.domain.provider.ProviderType
import pt.isel.leic.multicloudguardian.domain.token.Token
import pt.isel.leic.multicloudguardian.domain.token.TokenValidationInfo
import pt.isel.leic.multicloudguardian.domain.user.PasswordValidationInfo
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.domain.user.UserStorageInfo
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.Id

interface UsersRepository {
    fun storeUser(
        username: Username,
        email: Email,
        salt: String,
        iterations: Int,
        passwordValidation: PasswordValidationInfo,
    ): Id

    fun isUserStoredByUsername(username: Username): Boolean

    fun isEmailStoredByEmail(email: Email): Boolean

    fun storagePreferences(
        userId: Id,
        performanceType: PerformanceType,
        locationType: LocationType,
        providerType: ProviderType,
    )

    fun getTokenByTokenValidationInfo(tokenValidationInfo: TokenValidationInfo): Pair<User, Token>?

    fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    )

    fun searchUsers(username: String): List<UserStorageInfo>

    fun createToken(
        token: Token,
        maxTokens: Int,
    )

    fun removeTokenByValidationInfo(tokenValidationInfo: TokenValidationInfo): Int

    fun getUserByUsername(username: Username): User?

    fun getUserInfoByUsername(username: Username): UserStorageInfo?

    fun getUserStorageById(userId: Id): UserStorageInfo?

    fun getUserById(userId: Id): User?

    fun getUserCredentialsById(userId: Id): Credentials?

    fun getProvider(userId: Id): ProviderType
}
