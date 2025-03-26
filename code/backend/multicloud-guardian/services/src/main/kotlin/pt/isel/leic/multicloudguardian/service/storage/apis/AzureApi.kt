package pt.isel.leic.multicloudguardian.service.storage.apis

import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.sas.BlobSasPermission
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues
import com.azure.storage.common.StorageSharedKeyCredential
import jakarta.inject.Named
import java.time.OffsetDateTime

@Named
class AzureApi {
    private val endPointFormat = "https://%s.blob.core.windows.net/"

    fun generateAzureSignedUrl(
        accountKey: String,
        accountName: String,
        bucketName: String,
        blobPath: String,
    ): String {
        val credential = StorageSharedKeyCredential(accountName, accountKey)
        val endpointFormatUrl = String.format(endPointFormat, accountName)
        val blobServiceClient =
            BlobServiceClientBuilder()
                .endpoint(endpointFormatUrl) // Troubleshooting version conflicts: https://aka.ms/azsdk/java/dependency/troubleshoot
                .credential(credential)
                .buildClient()

        // Create a SAS token
        val blobClient =
            blobServiceClient
                .getBlobContainerClient(bucketName)
                .getBlobClient(blobPath)

        // Create a SAS token that's valid for 15 minutes
        val expiryTime = OffsetDateTime.now().plusMinutes(15)
        val sasPermission = BlobSasPermission().setReadPermission(true)
        val sasSignatureValues =
            BlobServiceSasSignatureValues(expiryTime, sasPermission)
                .setStartTime(OffsetDateTime.now().plusSeconds(2))
        val sasToken = blobClient.generateSas(sasSignatureValues)
        return "$endpointFormatUrl$bucketName/$blobPath?$sasToken"
    }
}
