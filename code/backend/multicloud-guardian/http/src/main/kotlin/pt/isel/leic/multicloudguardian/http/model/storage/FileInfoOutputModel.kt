package pt.isel.leic.multicloudguardian.http.model.storage

import pt.isel.leic.multicloudguardian.domain.file.File

data class FileInfoOutputModel(
    val fileId: Int,
    val userId: Int,
    val folderId: Int?,
    val name: String,
    val size: Int,
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
                userId = file.userId.value,
                folderId = file.folderId?.value,
                name = file.fileName,
                size = file.size,
                encryption = file.encryption,
                url = url,
            )
    }
}
