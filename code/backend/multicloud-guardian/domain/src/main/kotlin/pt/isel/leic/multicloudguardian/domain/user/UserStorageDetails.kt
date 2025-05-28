package pt.isel.leic.multicloudguardian.domain.user

data class UserStorageDetails(
    val totalSize: Long,
    val images: Long,
    val video: Long,
    val documents: Long,
    val others: Long,
)
