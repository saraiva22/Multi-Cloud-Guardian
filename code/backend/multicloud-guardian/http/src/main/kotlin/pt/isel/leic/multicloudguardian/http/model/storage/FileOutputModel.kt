package pt.isel.leic.multicloudguardian.http.model.storage

import pt.isel.leic.multicloudguardian.domain.file.File

data class FileOutputModel(
    val fileId: Int,
    val userId: Int,
    val folderId: Int?,
    val name: String,
    val path: String,
    val size: Int,
    val encryption: Boolean,
    val url: String,
) {
    companion object {
        fun fromDomain(file: File): FileOutputModel =
            FileOutputModel(
                fileId = file.fileId.value,
                userId = file.userId.value,
                folderId = file.folderId?.value,
                name = file.name,
                path = file.path,
                size = file.size,
                encryption = file.encryption,
                url = file.url,
            )
    }
}
