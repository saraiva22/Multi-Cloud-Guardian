package pt.isel.leic.multicloudguardian.http.model.storage

data class DownloadFileInputModel(
    val encryption: Boolean,
    val encryptedKey: String,
    val pathSaveFile: String,
)
