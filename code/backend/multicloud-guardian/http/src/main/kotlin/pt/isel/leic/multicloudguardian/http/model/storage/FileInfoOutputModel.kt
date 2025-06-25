package pt.isel.leic.multicloudguardian.http.model.storage

import pt.isel.leic.multicloudguardian.domain.file.File
import pt.isel.leic.multicloudguardian.http.model.user.UserInfoOutputModel

data class FileInfoOutputModel(
    val fileId: Int,
    val user: UserInfoOutputModel,
    val folderInfo: FolderInfoDetailsModel?,
    val name: String,
    val size: Long,
    val contentType: String,
    val createdAt: Long,
    val encryption: Boolean,
    val url: String? = null,
) {
    companion object {
        fun fromDomain(
            file: File,
            url: String? = null,
        ): FileInfoOutputModel =
            FileInfoOutputModel(
                fileId = file.fileId.value,
                user =
                    UserInfoOutputModel(
                        id = file.user.id.value,
                        username = file.user.username.value,
                        email = file.user.email.value,
                    ),
                folderInfo =
                    file.folderInfo?.let {
                        FolderInfoDetailsModel(
                            folderId = it.id.value,
                            folderName = it.folderName,
                            folderType = it.folderType,
                        )
                    },
                name = file.fileName,
                size = file.size,
                contentType = file.contentType,
                createdAt = file.createdAt.epochSeconds,
                encryption = file.encryption,
                url = url,
            )
    }
}
