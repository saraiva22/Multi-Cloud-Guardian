package pt.isel.leic.multicloudguardian.http.model.storage

import pt.isel.leic.multicloudguardian.domain.folder.Folder
import pt.isel.leic.multicloudguardian.domain.folder.FolderType
import pt.isel.leic.multicloudguardian.domain.user.UserInfo
import pt.isel.leic.multicloudguardian.http.model.user.UserInfoOutputModel

data class FolderInfoOutputModel(
    val folderId: Int,
    val user: UserInfoOutputModel,
    val parentFolderInfo: FolderInfoDetailsModel?,
    val folderName: String,
    val size: Long,
    val numberFile: Int,
    val path: String,
    val type: FolderType,
    val createdAt: Long,
    val updatedAt: Long,
    val members: List<UserInfoOutputModel>? = null,
) {
    companion object {
        fun fromDomain(
            folder: Folder,
            members: List<UserInfo>? = null,
        ): FolderInfoOutputModel =
            FolderInfoOutputModel(
                folder.folderId.value,
                UserInfoOutputModel(
                    folder.user.id.value,
                    folder.user.username.value,
                    folder.user.email.value,
                ),
                folder.parentFolderInfo?.let {
                    FolderInfoDetailsModel(
                        it.id.value,
                        it.folderName,
                    )
                },
                folder.folderName,
                folder.size,
                folder.numberFiles,
                folder.path,
                folder.type,
                folder.createdAt.epochSeconds,
                folder.updatedAt.epochSeconds,
                members?.map {
                    UserInfoOutputModel(
                        it.id.value,
                        it.username.value,
                        it.email.value,
                    )
                } ?: emptyList(),
            )
    }
}
