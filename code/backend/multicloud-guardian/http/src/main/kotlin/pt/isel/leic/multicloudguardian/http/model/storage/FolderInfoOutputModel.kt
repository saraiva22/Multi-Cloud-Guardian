package pt.isel.leic.multicloudguardian.http.model.storage

import pt.isel.leic.multicloudguardian.domain.folder.Folder
import pt.isel.leic.multicloudguardian.domain.folder.Type

data class FolderInfoOutputModel(
    val folderId: Int,
    val user: UserInfoOutputModel,
    val parentFolderId: Int?,
    val folderName: String,
    val size: Long,
    val numberFile: Int,
    val path: String,
    val type: Type,
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object {
        fun fromDomain(folder: Folder): FolderInfoOutputModel =
            FolderInfoOutputModel(
                folder.folderId.value,
                UserInfoOutputModel(
                    folder.user.id.value,
                    folder.user.username.value,
                    folder.user.email.value,
                ),
                folder.parentFolderId?.value,
                folder.folderName,
                folder.size,
                folder.numberFiles,
                folder.path,
                folder.type,
                folder.createdAt.epochSeconds,
                folder.updatedAt.epochSeconds,
            )
    }
}
