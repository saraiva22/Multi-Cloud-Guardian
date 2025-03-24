package pt.isel.leic.multicloudguardian.service.storage

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pt.isel.leic.multicloudguardian.domain.file.FileCreate
import pt.isel.leic.multicloudguardian.domain.provider.ProviderDomain
import pt.isel.leic.multicloudguardian.domain.provider.ProviderType
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Id
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.domain.utils.failure
import pt.isel.leic.multicloudguardian.domain.utils.success
import pt.isel.leic.multicloudguardian.repository.TransactionManager
import pt.isel.leic.multicloudguardian.service.security.SecurityService
import pt.isel.leic.multicloudguardian.service.storage.jclouds.StorageFileJclouds

@Service
class StorageService(
    private val transactionManager: TransactionManager,
    private val securityService: SecurityService,
    private val jcloudsStorage: StorageFileJclouds,
    private val providerDomain: ProviderDomain,
) {
    fun createFile(
        file: FileCreate,
        encryption: Boolean,
        user: User,
    ): FileCreationResult {
        val provider = ProviderType.GOOGLE
        val bucketName = providerDomain.getBucketName(provider) ?: ""
        val pathOrigin = "$bucketName/${user.username.value}"
        val credential = providerDomain.getCredential(provider) ?: ""
        val identity = providerDomain.getIdentity(provider) ?: ""
        val contextStorage = jcloudsStorage.initializeBlobStoreContext(credential, identity, provider)

        return when (contextStorage) {
            is Success -> {
                jcloudsStorage.createBucketIfNotExists(contextStorage.value, bucketName)

                jcloudsStorage.uploadBlob(
                    file.fileName,
                    file.fileContent,
                    file.contentType,
                    contextStorage.value,
                    bucketName,
                    user.username.value,
                )
                contextStorage.value.close()
                return success(Id(1))
            }

            is Failure ->
                failure(
                    FileCreationError.FileError,
                )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StorageService::class.java)
    }
}
