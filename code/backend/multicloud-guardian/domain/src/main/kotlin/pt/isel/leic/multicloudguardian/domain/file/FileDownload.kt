package pt.isel.leic.multicloudguardian.domain.file

data class FileDownload(
    val fileContent: ByteArray,
    val fileName: String,
    val mimeType: String,
    val encrypted: Boolean,
)
