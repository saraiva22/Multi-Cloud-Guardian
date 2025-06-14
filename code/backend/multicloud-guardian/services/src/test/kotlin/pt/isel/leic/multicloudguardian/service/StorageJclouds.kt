package pt.isel.leic.multicloudguardian.service

import pt.isel.leic.multicloudguardian.domain.provider.ProviderType
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.service.utils.ServiceTests
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

class StorageJclouds : ServiceTests() {
    @Test
    fun `should initialize BlobStoreContext with valid Azure credentials`() {
        // Arrange: set up valid Azure credentials
        val azure = ProviderType.AZURE
        val credentials = getCredentials(azure)
        val identity = getIdentity(azure)

        // Act: attempt to initialize BlobStoreContext
        val result = jcloudsStorage.initializeBlobStoreContext(credentials, identity, azure)

        // Assert: initialization succeeds
        when (result) {
            is Success -> assertNotNull(result.value.blobStore)
            is Failure -> fail("Unexpected $result")
        }
    }

    @Test
    fun `should create BlobStorageContext with valid credentials`() {
        // Arrange: set up valid credentials for a provider
        val provider = ProviderType.AZURE
        val credentials = getCredentials(provider)
        val identity = getIdentity(provider)

        // Act: attempt to initialize BlobStoreContext
        val context = jcloudsStorage.initializeBlobStoreContext(credentials, identity, provider)

        // Assert: context is successfully created
        when (context) {
            is Success -> assertNotNull(context.value.blobStore)
            is Failure -> fail("Unexpected $context")
        }
        // Act: attempt to create BlobStorageContext
        val result = jcloudsStorage.createBucketIfNotExists(context.value, getBucketName(provider))

        // Assert: creation succeeds
        when (result) {
            is Success -> assertNotNull(result.value)
            is Failure -> fail("Unexpected $result")
        }
    }

    @Test
    fun `upload Blob to Azure`() {
        // Arrange: set up valid Azure credentials and a file to upload
        val azure = ProviderType.AZURE
        val credentials = getCredentials(azure)
        val identity = getIdentity(azure)
        val context = jcloudsStorage.initializeBlobStoreContext(credentials, identity, azure)

        // Assert: context is successfully created
        when (context) {
            is Success -> assertNotNull(context.value.blobStore)
            is Failure -> fail("Unexpected $context")
        }

        // Act: attempt to upload a file
        val path = "test-client/test-blob.txt"
        val data = "This is a test blob content".toByteArray()
        val contentType = "text/plain"
        val encryption = false
        val bucketName = getBucketName(azure)
        val fileToUpload = jcloudsStorage.uploadBlob(path, data, contentType, context.value, bucketName, encryption)

        // Assert: upload succeeds
        when (fileToUpload) {
            is Success -> assertEquals(true, fileToUpload.value)
            is Failure -> fail("Unexpected $fileToUpload")
        }

        // Act: delete the uploaded blob
        val deleteResult = jcloudsStorage.deleteBlob(context.value, bucketName, path)

        // Assert: deletion succeeds
        when (deleteResult) {
            is Success -> assertNotNull(deleteResult.value)
            is Failure -> fail("Unexpected $deleteResult")
        }
    }
}
