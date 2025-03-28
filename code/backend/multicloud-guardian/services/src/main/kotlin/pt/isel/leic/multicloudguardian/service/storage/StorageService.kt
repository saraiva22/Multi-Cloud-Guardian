package pt.isel.leic.multicloudguardian.service.storage

import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pt.isel.leic.multicloudguardian.domain.file.FileCreate
import pt.isel.leic.multicloudguardian.domain.provider.ProviderDomainConfig
import pt.isel.leic.multicloudguardian.domain.provider.ProviderType
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Id
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.domain.utils.failure
import pt.isel.leic.multicloudguardian.domain.utils.success
import pt.isel.leic.multicloudguardian.repository.TransactionManager
import pt.isel.leic.multicloudguardian.service.security.SecurityService
import pt.isel.leic.multicloudguardian.service.storage.jclouds.CreateBlobStorageContextError
import pt.isel.leic.multicloudguardian.service.storage.jclouds.StorageFileJclouds

@Service
class StorageService(
    private val transactionManager: TransactionManager,
    private val securityService: SecurityService,
    private val jcloudsStorage: StorageFileJclouds,
    private val providerDomain: ProviderDomainConfig,
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
            val bucketName = providerDomain.getBucketName(provider)
            val checkSum = securityService.calculateChecksum(file.fileContent)

            when (val contextStorage = createContextStorage(provider, bucketName)) {
                is Failure ->
                    when (contextStorage.value) {
                        CreateContextJCloudError.InvalidCredential -> return@run failure(FileCreationError.InvalidCredential)
                        CreateContextJCloudError.ErrorCreatingContext -> return@run failure(FileCreationError.ErrorCreatingContext)
                        CreateContextJCloudError.ErrorCreatingGlobalBucket -> return@run failure(
                            FileCreationError.ErrorCreatingGlobalBucket,
                        )
                    }

                is Success -> {
                    val isFileNameAlreadyExists =
                        fileRepository.getFileNames(user.id).find { fileName -> fileName == file.fileName }
                    if (isFileNameAlreadyExists != null) {
                        contextStorage.value.close()
                        return@run failure(FileCreationError.FileNameAlreadyExists)
                    }

                    val isUploadFile =
                        jcloudsStorage.uploadBlob(
                            file.fileName,
                            file.fileContent,
                            file.contentType,
                            contextStorage.value,
                            bucketName,
                            user.username.value,
                        )

                    val path = "${user.username.value}/${file.fileName}"
                    when (isUploadFile) {
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
                                    "TESTING",
                                    user.id,
                                    false,
                                    clock.now(),
                                )
                            contextStorage.value.close()
                            return@run success(Pair(fileId, "VALUE"))
                        }
                    }
                }
            }
        }
    }

    fun getFileById(
        user: User,
        fileId: Id,
    ): GetFileResult =
        transactionManager.run {
            val usersRepository = it.usersRepository
            val fileRepository = it.fileRepository
            val file =
                fileRepository.getFileById(user.id, fileId) ?: return@run failure(GetFileByIdError.FileNotFound)

            val provider = usersRepository.getProvider(user.id)
            val bucketName = providerDomain.getBucketName(provider)
            val credential = providerDomain.getCredential(provider)
            val identity = providerDomain.getIdentity(provider)
            val location = providerDomain.getLocation(provider)

            when (val contextStorage = createContextStorage(provider, bucketName)) {
                is Failure ->
                    when (contextStorage.value) {
                        CreateContextJCloudError.ErrorCreatingContext -> return@run failure(GetFileByIdError.ErrorCreatingContext)
                        CreateContextJCloudError.ErrorCreatingGlobalBucket -> return@run failure(GetFileByIdError.ErrorCreatingGlobalBucket)
                        CreateContextJCloudError.InvalidCredential -> return@run failure(GetFileByIdError.InvalidCredential)
                    }

                is Success -> {
                    val generatedUrl =
                        jcloudsStorage.generateBlobUrl(provider, bucketName, credential, identity, file.path, location)
                    contextStorage.value.close()
                    return@run success(file.copy(url = generatedUrl))
                }
            }
        }

    private fun createContextStorage(
        provider: ProviderType,
        bucketName: String,
    ): CreateContextJCloudResult {
        val credential = providerDomain.getCredential(provider)
        val identity = providerDomain.getIdentity(provider)

        val contextStorage =
            jcloudsStorage.initializeBlobStoreContext(
                credential,
                identity,
                provider,
            )
        when (contextStorage) {
            is Failure ->
                return when (contextStorage.value) {
                    CreateBlobStorageContextError.InvalidCredential -> failure(CreateContextJCloudError.InvalidCredential)
                    CreateBlobStorageContextError.ErrorCreatingContext -> failure(CreateContextJCloudError.ErrorCreatingContext)
                }

            is Success -> {
                val isBucketExists =
                    jcloudsStorage.createBucketIfNotExists(contextStorage.value, bucketName)

                return when (isBucketExists) {
                    is Failure -> {
                        contextStorage.value.close()
                        failure(CreateContextJCloudError.ErrorCreatingGlobalBucket)
                    }

                    is Success -> success(contextStorage.value)
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StorageService::class.java)
    }
}
