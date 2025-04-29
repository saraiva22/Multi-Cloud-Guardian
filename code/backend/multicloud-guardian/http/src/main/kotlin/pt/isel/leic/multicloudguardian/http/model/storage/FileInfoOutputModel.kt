package pt.isel.leic.multicloudguardian.http.model.storage

import pt.isel.leic.multicloudguardian.domain.file.File

data class FileInfoOutputModel(
    val fileId: Int,
    val user: UserInfoOutputModel,
    val folderId: Int?,
    val name: String,
    val size: Long,
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
                folderId = file.folderId?.value,
                name = file.fileName,
                size = file.size,
                encryption = file.encryption,
                url = url,
            )
    }
}
