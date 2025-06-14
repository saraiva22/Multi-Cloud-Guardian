package pt.isel.leic.multicloudguardian.service

import pt.isel.leic.multicloudguardian.domain.provider.ProviderType
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.service.storage.jclouds.CreateBlobStorageContextError
import pt.isel.leic.multicloudguardian.service.utils.ServiceTests
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
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
    fun `should fail to initialize BlobStoreContext with invalid Azure credentials`() {
        // Arrange: set up invalid Azure credentials
        val azure = ProviderType.AZURE
        val invalidCredentials = "invalid-credentials"
        val invalidIdentity = "invalid-identity"

        // Act: attempt to initialize BlobStoreContext
        val result = jcloudsStorage.initializeBlobStoreContext(invalidCredentials, invalidIdentity, azure)

        // Assert: initialization fails
        when (result) {
            is Success -> fail("Unexpected $result")
            is Failure -> assertTrue(result.value is CreateBlobStorageContextError.ErrorCreatingContext)
        }
    }
}
