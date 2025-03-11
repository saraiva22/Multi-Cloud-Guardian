package user

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import token.Token
import token.TokenEncoder
import token.TokenValidationInfo
import user.components.Password
import java.security.SecureRandom
import java.util.*

@Component
class UsersDomain(
    private val passwordEncoder: PasswordEncoder,
    private val tokenEncoder: TokenEncoder,
    private val config: UsersDomainConfig,
) {
    fun generateTokenValue(): String =
        ByteArray(config.tokenSizeInBytes).let { byteArray ->
            SecureRandom.getInstanceStrong().nextBytes(byteArray)
            Base64.getUrlEncoder().encodeToString(byteArray)
        }

    fun canBeToken(token: String): Boolean = try {
        Base64.getUrlDecoder()
            .decode(token).size == config.tokenSizeInBytes
    } catch (ex: IllegalArgumentException) {
        false
    }

    fun validatePassword(password: String, validationInfo: PasswordValidationInfo) = passwordEncoder.matches(
        password,
        validationInfo.validationInfo,
    )

    fun createPasswordValidationInformation(password: String) = PasswordValidationInfo(
        validationInfo = passwordEncoder.encode(password),
    )

    fun isTokenTimeValid(
        clock: Clock,
        token: Token,
    ): Boolean {
        val now = clock.now()
        return token.createdAt <= now &&
                (now - token.createdAt) <= config.tokenTtl &&
                (now - token.lastUsedAt) <= config.tokenRollingTtl
    }

    fun getTokenExpiration(token: Token): Instant {
        val absoluteExpiration = token.createdAt + config.tokenTtl
        val rollingExpiration = token.lastUsedAt + config.tokenRollingTtl
        return if (absoluteExpiration < rollingExpiration) {
            absoluteExpiration
        } else {
            rollingExpiration
        }
    }

    fun createTokenValidationInformation(token: String): TokenValidationInfo =
        tokenEncoder.createValidationInformation(token)

    fun isSafePassword(password: Password) = password.isSafe(password)

    val maxNumberOfTokensPerUser = config.maxTokensPerUser
}