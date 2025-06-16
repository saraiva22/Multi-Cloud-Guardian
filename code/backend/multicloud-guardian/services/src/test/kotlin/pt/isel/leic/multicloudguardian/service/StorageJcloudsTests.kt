package pt.isel.leic.multicloudguardian.service

import pt.isel.leic.multicloudguardian.domain.provider.ProviderType
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.service.utils.ServiceTests
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

class StorageJcloudsTests : ServiceTests() {
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
    fun `should upload and delete blob in Azure`() {
        uploadAndDeleteBlobTest(ProviderType.AZURE)
    }

    @Test
    fun `should upload and delete blob in Amazon S3`() {
        uploadAndDeleteBlobTest(ProviderType.AMAZON)
    }

    @Test
    fun `should upload and delete blob in BackBlaze`() {
        uploadAndDeleteBlobTest(ProviderType.BACK_BLAZE)
    }

    private fun uploadAndDeleteBlobTest(provider: ProviderType) {
        // Arrange: set up valid credentials and a file to upload
        val credentials = getCredentials(provider)
        val identity = getIdentity(provider)
        val context = jcloudsStorage.initializeBlobStoreContext(credentials, identity, provider)

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
        val bucketName = getBucketName(provider)
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

    @Test
    fun `should upload two blobs in a folder and delete them`() {
        val provider = ProviderType.AZURE
        val credentials = getCredentials(provider)
        val identity = getIdentity(provider)
        val contextResult = jcloudsStorage.initializeBlobStoreContext(credentials, identity, provider)
        val bucketName = getBucketName(provider)
        val folder = "test-folder/"
        val blob1 = folder + "file1.txt"
        val blob2 = folder + "file2.txt"
        val data1 = "Content for file 1".toByteArray()
        val data2 = "Content for file 2".toByteArray()
        val contentType = "text/plain"
        val encryption = false

        // Assert context creation
        val context =
            when (contextResult) {
                is Success -> contextResult.value
                is Failure -> fail("Unexpected $contextResult")
            }

        // Upload blob 1
        val upload1 = jcloudsStorage.uploadBlob(folder + blob1, data1, contentType, context, bucketName, encryption)
        when (upload1) {
            is Success -> assertEquals(true, upload1.value)
            is Failure -> fail("Unexpected $upload1")
        }

        // Upload blob 2
        val upload2 = jcloudsStorage.uploadBlob(folder + blob2, data2, contentType, context, bucketName, encryption)
        when (upload2) {
            is Success -> assertEquals(true, upload2.value)
            is Failure -> fail("Unexpected $upload2")
        }

        // Delete both blobs
        val delete = jcloudsStorage.deleteBlobs(context, bucketName, folder)
        when (delete) {
            is Success -> assertNotNull(delete.value)
            is Failure -> fail("Unexpected $delete")
        }
    }

    @Test
    fun `should generate blob URL for a given blob`() {
        // Arrange
        val provider = ProviderType.AZURE
        val credentials = getCredentials(provider)
        val identity = getIdentity(provider)
        val bucketName = getBucketName(provider)
        val location = getLocation(provider)
        val folder = "test-client/"
        val blobName = "test-blob-url.txt"
        val blobPath = folder + blobName
        val data = "Blob content for URL test".toByteArray()
        val contentType = "text/plain"
        val encryption = false
        val minutes = 1L

        // Act:  Initialize context
        val contextResult = jcloudsStorage.initializeBlobStoreContext(credentials, identity, provider)
        val context =
            when (contextResult) {
                is Success -> contextResult.value
                is Failure -> fail("Unexpected $contextResult")
            }

        // Act:  Upload blob
        val uploadResult = jcloudsStorage.uploadBlob(blobPath, data, contentType, context, bucketName, encryption)

        // Assert: upload succeeded
        when (uploadResult) {
            is Success -> assertEquals(true, uploadResult.value)
            is Failure -> fail("Unexpected $uploadResult")
        }

        // Act: Generate URL
        val urlResult = jcloudsStorage.generateBlobUrl(provider, bucketName, credentials, identity, blobPath, location, minutes)

        // Assert: URL is valid
        assertNotNull(urlResult)
        assert(urlResult.contains(blobPath))
        assert(urlResult.contains(bucketName))

        // Act: clean up by deleting the uploaded blob
        val deleteResult = jcloudsStorage.deleteBlob(context, bucketName, blobPath)

        // Assert: deletion succeeded
        when (deleteResult) {
            is Success -> assertNotNull(deleteResult.value)
            is Failure -> fail("Cleanup failed: $deleteResult")
        }
    }

    @Test
    fun `should move blob into a folder - Azure`() {
        // Arrange
        val provider = ProviderType.AZURE
        val credentials = getCredentials(provider)
        val identity = getIdentity(provider)
        val bucketName = getBucketName(provider)
        val folder = "test-isel/"
        val originalBlobName = "move-blob-original.txt"
        val originalBlobPath = originalBlobName // upload to root
        val movedBlobPath = folder + originalBlobName // move to folder
        val data = "Blob content for move test".toByteArray()
        val contentType = "text/plain"
        val encryption = false

        // Act: Initialize context
        val contextResult = jcloudsStorage.initializeBlobStoreContext(credentials, identity, provider)
        val context =
            when (contextResult) {
                is Success -> contextResult.value
                is Failure -> fail("Unexpected $contextResult")
            }

        // Act: Upload blob to root
        val uploadResult = jcloudsStorage.uploadBlob(originalBlobPath, data, contentType, context, bucketName, encryption)
        when (uploadResult) {
            is Success -> assertEquals(true, uploadResult.value)
            is Failure -> fail("Unexpected $uploadResult")
        }

        // Act: Move blob into folder
        val moveResult = jcloudsStorage.moveBlob(context, bucketName, originalBlobPath, movedBlobPath)
        when (moveResult) {
            is Success -> assertEquals(true, moveResult.value)
            is Failure -> fail("Move failed: $moveResult")
        }

        // Assert: Original blob no longer exists
        val originalBlob = context.blobStore.getBlob(bucketName, originalBlobPath)
        assertNull(originalBlob)

        // Assert: Moved blob exists in folder
        val movedBlob = context.blobStore.getBlob(bucketName, movedBlobPath)
        assertNotNull(movedBlob)

        // Clean up: Delete moved blob
        val deleteResult = jcloudsStorage.deleteBlob(context, bucketName, movedBlobPath)
        when (deleteResult) {
            is Success -> assertNotNull(deleteResult.value)
            is Failure -> fail("Cleanup failed: $deleteResult")
        }
    }

    @Test
    fun `should move blob into a folder - BackBlaze`() {
        // Arrange
        val provider = ProviderType.BACK_BLAZE
        val credentials = getCredentials(provider)
        val identity = getIdentity(provider)
        val bucketName = getBucketName(provider)
        val folder = "test-isel/"
        val originalBlobName = "move-blob-original.txt"
        val originalBlobPath = originalBlobName // upload to root
        val movedBlobPath = folder + originalBlobName // move to folder
        val data = "Blob content for move test".toByteArray()
        val contentType = "text/plain"
        val encryption = false

        // Act: Initialize context
        val contextResult = jcloudsStorage.initializeBlobStoreContext(credentials, identity, provider)
        val context =
            when (contextResult) {
                is Success -> contextResult.value
                is Failure -> fail("Unexpected $contextResult")
            }

        // Act: Upload blob to root
        val uploadResult = jcloudsStorage.uploadBlob(originalBlobPath, data, contentType, context, bucketName, encryption)
        when (uploadResult) {
            is Success -> assertEquals(true, uploadResult.value)
            is Failure -> fail("Unexpected $uploadResult")
        }

        // Act: Move blob into folder
        val moveResult = jcloudsStorage.moveBlob(context, bucketName, originalBlobPath, movedBlobPath)
        when (moveResult) {
            is Success -> assertEquals(true, moveResult.value)
            is Failure -> fail("Move failed: $moveResult")
        }

        // Assert: Original blob no longer exists
        val originalBlob = context.blobStore.getBlob(bucketName, originalBlobPath)
        assertNull(originalBlob)

        // Assert: Moved blob exists in folder
        val movedBlob = context.blobStore.getBlob(bucketName, movedBlobPath)
        assertNotNull(movedBlob)

        // Clean up: Delete moved blob
        val deleteResult = jcloudsStorage.deleteBlob(context, bucketName, movedBlobPath)
        when (deleteResult) {
            is Success -> assertNotNull(deleteResult.value)
            is Failure -> fail("Cleanup failed: $deleteResult")
        }
    }

    @Test
    fun `should move blob into a folder - Amazon`() {
        // Arrange
        val provider = ProviderType.AMAZON
        val credentials = getCredentials(provider)
        val identity = getIdentity(provider)
        val bucketName = getBucketName(provider)
        val folder = "test-isel/"
        val originalBlobName = "move-blob-original.txt"
        val originalBlobPath = originalBlobName // upload to root
        val movedBlobPath = folder + originalBlobName // move to folder
        val data = "Blob content for move test".toByteArray()
        val contentType = "text/plain"
        val encryption = false

        // Act: Initialize context
        val contextResult = jcloudsStorage.initializeBlobStoreContext(credentials, identity, provider)
        val context =
            when (contextResult) {
                is Success -> contextResult.value
                is Failure -> fail("Unexpected $contextResult")
            }

        // Act: Upload blob to root
        val uploadResult = jcloudsStorage.uploadBlob(originalBlobPath, data, contentType, context, bucketName, encryption)
        when (uploadResult) {
            is Success -> assertEquals(true, uploadResult.value)
            is Failure -> fail("Unexpected $uploadResult")
        }

        // Act: Move blob into folder
        val moveResult = jcloudsStorage.moveBlob(context, bucketName, originalBlobPath, movedBlobPath)
        when (moveResult) {
            is Success -> assertEquals(true, moveResult.value)
            is Failure -> fail("Move failed: $moveResult")
        }

        // Assert: Original blob no longer exists
        val originalBlob = context.blobStore.getBlob(bucketName, originalBlobPath)
        assertNull(originalBlob)

        // Assert: Moved blob exists in folder
        val movedBlob = context.blobStore.getBlob(bucketName, movedBlobPath)
        assertNotNull(movedBlob)

        // Clean up: Delete moved blob
        val deleteResult = jcloudsStorage.deleteBlob(context, bucketName, movedBlobPath)
        when (deleteResult) {
            is Success -> assertNotNull(deleteResult.value)
            is Failure -> fail("Cleanup failed: $deleteResult")
        }
    }
}
