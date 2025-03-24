package pt.isel.leic.multicloudguardian.service.storage.jclouds

import jakarta.inject.Named
import org.jclouds.Context
import org.jclouds.ContextBuilder
import org.jclouds.blobstore.BlobStoreContext
import org.jclouds.blobstore.options.ListContainerOptions
import org.jclouds.googlecloud.GoogleCredentialsFromJson
import org.jclouds.openstack.swift.v1.SwiftApiMetadata
import org.slf4j.LoggerFactory
import pt.isel.leic.multicloudguardian.domain.provider.ProviderType
import pt.isel.leic.multicloudguardian.domain.utils.failure
import pt.isel.leic.multicloudguardian.domain.utils.success
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

@Named
class StorageFileJclouds {
    fun createBucketIfNotExists(
        context: BlobStoreContext,
        bucketName: String,
    ): CreateGlobalBucketResult {
        val blobStore = context.blobStore

        if (blobStore.containerExists(bucketName)) {
            return success(false)
        }

        logger.info("Creating bucket: $bucketName")
        return try {
            val apiMetadata = context.unwrap<Context>().providerMetadata.apiMetadata
            val location =
                if (apiMetadata is SwiftApiMetadata) {
                    blobStore.listAssignableLocations().firstOrNull()
                } else {
                    null
                }

            blobStore.createContainerInLocation(location, bucketName)
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
            return failure(CreateBlobStorageContext.InvalidCredential)
        }

        val context =
            ContextBuilder
                .newBuilder(ProviderType.convertString(providerType))
                .credentials(identity, finalCredential)
                .buildView(BlobStoreContext::class.java)

        return when (context) {
            is BlobStoreContext -> success(context)
            else -> failure(CreateBlobStorageContext.ErrorCreatingContext)
        }
    }

    fun uploadBlob(
        blobName: String,
        data: ByteArray,
        contentType: String,
        context: BlobStoreContext,
        bucketName: String,
        username: String,
    ) {
        val blobStore = context.blobStore
        val blob =
            blobStore
                .blobBuilder(blobName)
                .payload(data)
                .contentLength(data.size.toLong())
                .contentType(contentType)
                .build()

        blobStore.putBlob(bucketName, blob)
        logger.info("Blob $blobName uploaded successfully")
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
