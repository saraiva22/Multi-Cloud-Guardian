package pt.isel.leic.multicloudguardian.domain.file

data class FileCreate(
    val fileName: String,
    val fileContent: ByteArray,
    val contentType: String,
    val size: Long,
    val encryption: Boolean,
)
