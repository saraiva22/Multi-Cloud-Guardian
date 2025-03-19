package pt.isel.leic.multicloudguardian.domain.token

interface TokenEncoder {
    fun createValidationInformation(token: String): TokenValidationInfo
}
