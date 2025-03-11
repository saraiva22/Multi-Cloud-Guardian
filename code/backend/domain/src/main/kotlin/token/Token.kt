package token

import kotlinx.datetime.Instant

class Token(
    val tokenValidationInfo: TokenValidationInfo,
    val userId: Int,
    val userAgent: String,
    val createdAt: Instant,
    val lastUsedAt: Instant,
)