package pt.isel.leic.multicloudguardian.service.storage

import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pt.isel.leic.multicloudguardian.domain.file.FileCreate
import pt.isel.leic.multicloudguardian.domain.provider.ProviderDomain
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.domain.utils.Failure
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
    private val clock: Clock,
) {
    fun createFile(
        file: FileCreate,
        encryption: Boolean,
        user: User,
    ): FileCreationResult {
        return transactionManager.run {
            val usersRepository = it.usersRepository
            val fileRepository = it.fileRepository

            val provider = usersRepository.getProvider(user.id)
            val bucketName = providerDomain.getBucketName(provider) ?: ""
            val folderOrigin = "$bucketName/${user.username.value}"
            val credential = providerDomain.getCredential(provider) ?: ""
            val identity = providerDomain.getIdentity(provider) ?: ""
            val checkSum = securityService.calculateChecksum(file.fileContent)
            when (val contextStorage = jcloudsStorage.initializeBlobStoreContext(credential, identity, provider)) {
                is Failure ->
                    failure(
                        FileCreationError.ErrorCreatingContext,
                    )

                is Success -> {
                    when (jcloudsStorage.createBucketIfNotExists(contextStorage.value, bucketName)) {
                        is Failure -> {
                            contextStorage.value.close()
                            return@run failure(FileCreationError.ErrorCreatingGlobalBucket)
                        }

                        is Success -> {
                            val isFileNameAlreadyExists =
                                fileRepository.getFileNames(user.id).find { fileName -> fileName == file.fileName }
                            if (isFileNameAlreadyExists != null) {
                                contextStorage.value.close()
                                return@run failure(FileCreationError.FileNameAlreadyExists)
                            }

                            val publicUrl =
                                jcloudsStorage.uploadBlob(
                                    file.fileName,
                                    provider,
                                    file.fileContent,
                                    file.contentType,
                                    contextStorage.value,
                                    bucketName,
                                    credential,
                                    identity,
                                    user.username.value,
                                )

                            val path = folderOrigin + "/" + file.fileName
                            when (publicUrl) {
                                is Failure -> {
                                    contextStorage.value.close()
                                    return@run failure(FileCreationError.FileStorageError)
                                }

                                is Success -> {
                                    val fileId =
                                        fileRepository.storeFile(
                                            file,
                                            path,
                                            checkSum,
                                            publicUrl.value,
                                            user.id,
                                            false,
                                            clock.now(),
                                        )
                                    contextStorage.value.close()
                                    return@run success(fileId)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StorageService::class.java)
    }
}
