package pt.isel.leic.multicloudguardian.service.storage.jclouds

import jakarta.inject.Named
import org.jclouds.ContextBuilder
import org.jclouds.blobstore.BlobStoreContext
import org.jclouds.blobstore.options.ListContainerOptions
import org.jclouds.googlecloud.GoogleCredentialsFromJson
import org.slf4j.LoggerFactory
import pt.isel.leic.multicloudguardian.domain.provider.ProviderType
import pt.isel.leic.multicloudguardian.domain.utils.failure
import pt.isel.leic.multicloudguardian.domain.utils.success
import pt.isel.leic.multicloudguardian.service.storage.apis.AmazonApi
import pt.isel.leic.multicloudguardian.service.storage.apis.AzureApi
import pt.isel.leic.multicloudguardian.service.storage.apis.BackBlazeApi
import pt.isel.leic.multicloudguardian.service.storage.apis.GoogleApi
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

@Named
class StorageFileJclouds(
    private val azureApi: AzureApi,
    private val googleApi: GoogleApi,
    private val amazonApi: AmazonApi,
    private val blazeApi: BackBlazeApi,
) {
    fun createBucketIfNotExists(
        context: BlobStoreContext,
        bucketName: String,
    ): CreateGlobalBucketResult {
        return try {
            val blobStore = context.blobStore
            if (blobStore.containerExists(bucketName)) {
                return success(false)
            }
            blobStore.createContainerInLocation(null, bucketName)
            logger.info("Creating bucket: $bucketName")

            success(true)
        } catch (e: Exception) {
            logger.info("Failed to create bucket: $bucketName", e)
            failure(CreateGlobalBucketError.ErrorCreatingGlobalBucket)
        }
    }

    fun initializeBlobStoreContext(
        credential: String,
        identity: String,
        providerType: ProviderType,
    ): CreateBlobStorageContextResult {
        val finalCredential =
            if (providerType == ProviderType.GOOGLE) {
                getCredentialFromJsonKeyFile(credential)
            } else {
                credential
            }
        if (finalCredential == null) {
            return failure(CreateBlobStorageContextError.InvalidCredential)
        }

        try {
            val context =
                ContextBuilder
                    .newBuilder(ProviderType.convertString(providerType))
                    .credentials(identity, finalCredential)
                    .buildView(BlobStoreContext::class.java)
            return success(context)
        } catch (e: Exception) {
            logger.info("Failed to create blob store context", e)
            return failure(CreateBlobStorageContextError.ErrorCreatingContext)
        }
    }

    fun uploadBlob(
        blobName: String,
        data: ByteArray,
        contentType: String,
        context: BlobStoreContext,
        bucketName: String,
        username: String,
    ): UploadBlobResult {
        try {
            val blobStore = context.blobStore
            val blob =
                blobStore
                    .blobBuilder("$username/$blobName")
                    .payload(data)
                    .contentLength(data.size.toLong())
                    .contentType(contentType)
                    .build()

            blobStore.putBlob(bucketName, blob)
            return success(true)
        } catch (e: Exception) {
            logger.info("Failed to upload blob: $blobName", e)
            return failure(UploadBlobError.ErrorUploadingBlob)
        }
    }

    fun downloadBlob(
        blobName: String,
        outputFilePath: String,
        context: BlobStoreContext,
        bucketName: String,
        username: String,
    ) {
        val blobStore = context.blobStore
        val downloadedBlob = blobStore.getBlob(bucketName + username, blobName)
        val inputStream: InputStream = downloadedBlob.payload.openStream()
        val fileOutput = File(outputFilePath)

        FileOutputStream(fileOutput).use { outputStream ->
            inputStream.copyTo(outputStream)
        }

        logger.info("Blob $blobName downloaded successfully to $outputFilePath")
    }

    fun listBlobs(
        context: BlobStoreContext,
        username: String,
        folderName: String?,
        bucketName: String,
    ): List<String> {
        val blobStore = context.blobStore
        val folderNameValid = folderName?.let { "$it/" } ?: ""
        val blobs = blobStore.list(bucketName + username, ListContainerOptions.Builder.prefix(folderNameValid))
        return blobs.map { blob -> blob.name }
    }

    fun generateBlobUrl(
        providerId: ProviderType,
        bucketName: String,
        credential: String,
        identity: String,
        blobPath: String,
        location: String,
    ): String =
        when (providerId) {
            ProviderType.GOOGLE -> googleApi.generateSignedUrl(credential, bucketName, blobPath, identity, location)
            ProviderType.AMAZON -> amazonApi.generateSignedUrl(credential, bucketName, blobPath, identity, location)
            ProviderType.AZURE -> azureApi.generateSignedUrl(credential, bucketName, blobPath, identity, location)
            ProviderType.BACK_BLAZE -> blazeApi.generateSignedUrl(credential, bucketName, blobPath, identity, location)
        }

    private fun getCredentialFromJsonKeyFile(filename: String): String? =
        try {
            val fileContents = File(filename).readText(StandardCharsets.UTF_8)
            val credentialSupplier = GoogleCredentialsFromJson(fileContents)
            credentialSupplier.get().credential
        } catch (e: Exception) {
            logger.error("Exception reading private key from '$filename'")
            null
        }

    private val logger = LoggerFactory.getLogger(StorageFileJclouds::class.java)
}
