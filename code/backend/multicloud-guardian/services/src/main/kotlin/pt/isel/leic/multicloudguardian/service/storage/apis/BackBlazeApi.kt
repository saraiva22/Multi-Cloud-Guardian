package pt.isel.leic.multicloudguardian.service.storage.apis

import com.backblaze.b2.client.B2StorageClientFactory
import com.backblaze.b2.client.structures.B2GetDownloadAuthorizationRequest
import jakarta.inject.Named
import org.slf4j.LoggerFactory
import java.time.Duration

@Named
class BackBlazeApi : ApiConfig {
    private val userAgent = "FileSync Multi-Cloud"

    private fun minutesToSeconds(minutes: Long): Int = Duration.ofMinutes(minutes).toSeconds().toInt()

    /** API - b2_get_download_authorization - https://www.backblaze.com/apidocs/b2-get-download-authorization
     *
     * Create a request to obtain the download authorization with the defined expiration
     *
     * validDurationInSeconds - The validity time of the authorization token in seconds
     *
     * getDownloadAuthorization - Method to obtain the download authorization token
     */
    override fun generateSignedUrl(
        credentials: String,
        bucketName: String,
        blobPath: String,
        identity: String,
        location: String,
        validDurationInMinutes: Long,
    ): String {
        try {
            val client = B2StorageClientFactory.createDefaultFactory().create(identity, credentials, userAgent)
            val downloadUrl = client.getDownloadByNameUrl(bucketName, blobPath)
            val bucketId = client.getBucketOrNullByName(bucketName).bucketId
            val request =
                B2GetDownloadAuthorizationRequest
                    .builder(bucketId, blobPath, minutesToSeconds(validDurationInMinutes))
                    .build()

            val downloadAuthorization = client.getDownloadAuthorization(request)
            val signedUrl = "$downloadUrl?Authorization=${downloadAuthorization.authorizationToken}"
            client.close()
            return signedUrl
        } catch (error: Exception) {
            logger.info("Info error :{}", error.toString())
            return ""
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BackBlazeApi::class.java)
    }
}
