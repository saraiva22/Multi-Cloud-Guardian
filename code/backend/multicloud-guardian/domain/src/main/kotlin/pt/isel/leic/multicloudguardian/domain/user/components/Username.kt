package pt.isel.leic.multicloudguardian.domain.user.components

private const val MAX_USERNAME_LENGTH = 25
private const val MIN_USERNAME_LENGTH = 5

data class Username(
    val value: String,
) {
    companion object {
        @Suppress("ktlint:standard:property-naming")
        const val minLength = MIN_USERNAME_LENGTH

        @Suppress("ktlint:standard:property-naming")
        const val maxLength = MAX_USERNAME_LENGTH
    }
}
