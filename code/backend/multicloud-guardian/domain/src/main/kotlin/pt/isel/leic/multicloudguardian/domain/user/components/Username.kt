package pt.isel.leic.multicloudguardian.domain.user.components

private const val MAX_USERNAME_LENGTH = 25
private const val MIN_USERNAME_LENGTH = 5

data class Username(
    val value: String,
) {
    companion object {
        const val MIN_LENGTH = MIN_USERNAME_LENGTH

        const val MAX_LENGTH = MAX_USERNAME_LENGTH
    }
}
