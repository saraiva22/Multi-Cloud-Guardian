package pt.isel.leic.multicloudguardian.service.storage.apis

interface ApiConfig {
    fun generateSignedUrl(
        credentials: String,
        bucketName: String,
        blobPath: String,
        identity: String,
        location: String,
        validDurationInMinutes: Long,
    ): String
}
