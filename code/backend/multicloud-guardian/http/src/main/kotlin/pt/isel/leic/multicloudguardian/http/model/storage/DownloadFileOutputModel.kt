package pt.isel.leic.multicloudguardian.http.model.storage

import pt.isel.leic.multicloudguardian.domain.file.FileDownload

data class DownloadFileOutputModel(
    val file: FileDownload,
    val fileKeyEncrypted: String?,
)
