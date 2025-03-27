package pt.isel.leic.multicloudguardian.service.storage.apis

import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.sas.BlobSasPermission
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues
import com.azure.storage.common.StorageSharedKeyCredential
import jakarta.inject.Named
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

@Named
class AzureApi : ApiConfig {
    private val endPointFormat = "https://%s.blob.core.windows.net/"

    override fun generateSignedUrl(
        credentials: String,
        bucketName: String,
        blobPath: String,
        identity: String,
        location: String,
        validDurationInMinutes: Long,
    ): String {
        try {
            val credential = StorageSharedKeyCredential(identity, credentials)
            val endpointFormatUrl = String.format(endPointFormat, identity)
            val blobServiceClient =
                BlobServiceClientBuilder()
                    .endpoint(endpointFormatUrl) // Troubleshooting version conflicts: https://aka.ms/azsdk/java/dependency/troubleshoot
                    .credential(credential)
                    .buildClient()

            val blobClient =
                blobServiceClient
                    .getBlobContainerClient(bucketName)
                    .getBlobClient(blobPath)

            val expiryTime = OffsetDateTime.now().plusMinutes(validDurationInMinutes)
            val sasPermission = BlobSasPermission().setReadPermission(true)
            val sasSignatureValues =
                BlobServiceSasSignatureValues(expiryTime, sasPermission)
                    .setStartTime(OffsetDateTime.now())
            val sasToken = blobClient.generateSas(sasSignatureValues)
            return "$endpointFormatUrl$bucketName/$blobPath?$sasToken"
        } catch (error: Exception) {
            logger.info("Info error $error")
            return ""
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AzureApi::class.java)
    }
}
