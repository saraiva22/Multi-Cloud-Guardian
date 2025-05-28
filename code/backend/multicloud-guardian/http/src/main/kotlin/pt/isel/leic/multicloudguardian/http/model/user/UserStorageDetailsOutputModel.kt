package pt.isel.leic.multicloudguardian.http.model.user

import pt.isel.leic.multicloudguardian.domain.user.UserStorageDetails

data class UserStorageDetailsOutputModel(
    val totalSize: Long,
    val images: Long,
    val video: Long,
    val documents: Long,
    val others: Long,
) {
    companion object {
        fun toDomain(userStorageDetails: UserStorageDetails): UserStorageDetails =
            UserStorageDetails(
                totalSize = userStorageDetails.totalSize,
                images = userStorageDetails.images,
                video = userStorageDetails.video,
                documents = userStorageDetails.documents,
                others = userStorageDetails.others,
            )
    }
}
