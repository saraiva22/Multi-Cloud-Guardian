package pt.isel.leic.multicloudguardian.http.model.user

data class UserStorageInfoOutputModel(
    val id: Int,
    val username: String,
    val email: String,
    val locationType: String,
    val performanceType: String,
)
