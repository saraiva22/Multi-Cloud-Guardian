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
import java.io.InputStream
import java.nio.charset.StandardCharsets

@Named
class StorageFileJclouds(
    private val azureApi: AzureApi,
    private val googleApi: GoogleApi,
    private val amazonApi: AmazonApi,
    private val blazeApi: BackBlazeApi,
) {
    // In JClouds, the default location is null
    private val defaultLocation = null

    fun createBucketIfNotExists(
        context: BlobStoreContext,
        bucketName: String,
    ): CreateGlobalBucketResult {
        return try {
            val blobStore = context.blobStore
            if (blobStore.containerExists(bucketName)) {
                return success(false)
            }
            blobStore.createContainerInLocation(defaultLocation, bucketName)
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
        path: String,
        data: ByteArray,
        contentType: String,
        context: BlobStoreContext,
        bucketName: String,
        encryption: Boolean,
    ): UploadBlobResult {
        try {
            val blobStore = context.blobStore
            val builder =
                blobStore
                    .blobBuilder(path)
                    .payload(data)
                    .contentLength(data.size.toLong())

            if (!encryption) {
                builder.contentType(contentType)
            }

            val blob = builder.build()

            blobStore.putBlob(bucketName, blob)
            return success(true)
        } catch (e: Exception) {
            logger.info("Failed to upload blob: $path", e)
            return failure(UploadBlobError.ErrorUploadingBlob)
        }
    }

    fun downloadBlob(
        bucketName: String,
        path: String,
        context: BlobStoreContext,
    ): DownloadBlobResult {
        try {
            val blobStore = context.blobStore
            val downloadedBlob = blobStore.getBlob(bucketName, path)
            val inputStream: InputStream = downloadedBlob.payload.openStream()

            val byteArrayOutputStream = java.io.ByteArrayOutputStream()
            inputStream.use { input ->
                byteArrayOutputStream.use { output ->
                    input.copyTo(output)
                }
            }

            logger.info("Blob $path downloaded successfully as ByteArray")
            return success(byteArrayOutputStream.toByteArray())
        } catch (e: Exception) {
            logger.info("Failed to upload blob: $path", e)
            return failure(DownloadBlobError.ErrorDownloadingBlob)
        }
    }

    fun createFolder(
        context: BlobStoreContext,
        path: String,
        bucketName: String,
        folderName: String,
    ): CreateFolderResult {
        try {
            val blobStore = context.blobStore

            val blob =
                blobStore
                    .blobBuilder(path)
                    .payload(ByteArray(0))
                    .contentLength(0)
                    .build()

            blobStore.putBlob(bucketName, blob)
            return success(true)
        } catch (e: Exception) {
            logger.info("Failed to create folder: $folderName", e)
            return failure(CreateFolderError.ErrorCreatingFolder)
        }
    }

    fun listBlobs(
        context: BlobStoreContext,
        folderName: String?,
        path: String,
    ): List<String> {
        val blobStore = context.blobStore
        val folderNameValid = folderName?.let { "$it/" } ?: ""
        val blobs = blobStore.list(path, ListContainerOptions.Builder.prefix(folderNameValid))
        return blobs.map { blob -> blob.name }
    }

    fun deleteBlob(
        context: BlobStoreContext,
        bucketName: String,
        blobName: String,
    ): DeleteBlobResult {
        try {
            val blobStore = context.blobStore
            blobStore.removeBlob(bucketName, blobName)
            logger.info("Blob $blobName deleted successfully")
            return success(Unit)
        } catch (e: Exception) {
            logger.info("Failed to delete blob: $blobName", e)
            return failure(DeleteBlobError.ErrorDeletingBlob)
        }
    }

    fun deleteBlobs(
        context: BlobStoreContext,
        bucketName: String,
        folderPath: String,
    ): DeleteBlobResult {
        try {
            val blobStore = context.blobStore

            val blobs =
                blobStore
                    .list(bucketName, ListContainerOptions.Builder.prefix(folderPath))
                    .map { it.name }
            logger.info("Blobs to delete: $blobs")
            if (blobs.isNotEmpty()) {
                blobStore.removeBlobs(bucketName, blobs)
                logger.info("Deleted folder $folderPath and contents: $blobs")
            } else {
                logger.info("No blobs found for folder $folderPath")
            }
            return success(Unit)
        } catch (e: Exception) {
            logger.error("Error deleting folder $folderPath", e)
            return failure(DeleteBlobError.ErrorDeletingBlob)
        }
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
