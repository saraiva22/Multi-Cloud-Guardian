package pt.isel.leic.multicloudguardian.http.model.storage

data class DownloadFileInputModel(
    val pathSaveFile: String,
    val encryptedKey: String?,
)
