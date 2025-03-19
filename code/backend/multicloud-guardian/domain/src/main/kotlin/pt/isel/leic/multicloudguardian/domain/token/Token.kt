package pt.isel.leic.multicloudguardian.domain.token

import kotlinx.datetime.Instant
import pt.isel.leic.multicloudguardian.domain.utils.Id

/**
 * Represents a token that can be used to authenticate a user.
 * @property tokenValidationInfo The information that can be used to validate the token.
 * @property userId The Id of the user that the token belongs to.
 * @property createdAt The [Instant] when the token was created.
 * @property lastUsedAt The [Instant] when the token was last used.
 * @property userAgent The user agent of the client that requested the token.
 */

class Token(
    val tokenValidationInfo: TokenValidationInfo,
    val userId: Id,
    val userAgent: String,
    val createdAt: Instant,
    val lastUsedAt: Instant,
)
