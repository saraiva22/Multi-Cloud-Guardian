package pt.isel.leic.multicloudguardian.service.user

import kotlinx.datetime.Instant
import pt.isel.leic.multicloudguardian.domain.utils.Either

data class TokenExternalInfo(
    val tokenValue: String,
    val tokenExpiration: Instant,
)

sealed class TokenCreationError {
    data object UserOrPasswordAreInvalid : TokenCreationError()

    data object InvalidToken : TokenCreationError()
}

typealias TokenCreationResult = Either<TokenCreationError, TokenExternalInfo>

sealed class TokenRevocationError {
    data object TokenDoesNotExist : TokenRevocationError()
}

typealias TokenRevocationResult = Either<TokenRevocationError, Boolean>
