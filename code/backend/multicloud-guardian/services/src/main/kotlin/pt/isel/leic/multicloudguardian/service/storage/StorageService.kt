package pt.isel.leic.multicloudguardian.service.storage

import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pt.isel.leic.multicloudguardian.domain.file.File
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
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.Arrays

@Service
class StorageService(
    private val transactionManager: TransactionManager,
    private val securityService: SecurityService,
    private val jcloudsStorage: StorageFileJclouds,
    private val providerDomain: ProviderDomainConfig,
    private val clock: Clock,
) {
    fun uploadFile(
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

            val isFileNameAlreadyExists =
                fileRepository.getFileNames(user.id).find { fileName -> fileName == file.blobName }
            if (isFileNameAlreadyExists != null) {
                return@run failure(FileCreationError.FileNameAlreadyExists)
            }

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
                    val secretKey = securityService.generationKeyAES()
                    val iv = securityService.generationIV()

                    val fileContentData =
                        if (encryption) {
                            val encryptedData =
                                securityService.encrypt(file.fileContent, secretKey, iv) ?: return@run failure(
                                    FileCreationError.ErrorEncryptingFile,
                                )
                            iv + encryptedData // Prepend IV to the encrypted data
                        } else {
                            file.fileContent
                        }

                    val isUploadFile =
                        jcloudsStorage.uploadBlob(
                            file.blobName,
                            fileContentData,
                            file.contentType,
                            contextStorage.value,
                            bucketName,
                            user.username.value,
                            encryption,
                        )

                    val path = "${user.username.value}/${file.blobName}"
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
                                    path,
                                    user.id,
                                    encryption,
                                    clock.now(),
                                )
                            contextStorage.value.close()
                            val key =
                                if (encryption) {
                                    securityService.secretKeyToString(secretKey)
                                } else {
                                    ""
                                }

                            success(Pair(fileId, key))
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
                    success(
                        Pair(file, generatedUrl),
                    )
                }
            }
        }

    fun downloadFile(
        fileId: Id,
        encryption: Boolean,
        encryptionKey: String,
        pathSaveFile: String,
        user: User,
    ): DownloadFileResult =
        transactionManager.run {
            val usersRepository = it.usersRepository
            val fileRepository = it.fileRepository
            val file =
                fileRepository.getFileById(user.id, fileId) ?: return@run failure(DownloadFileError.FileNotFound)

            val provider = usersRepository.getProvider(user.id)
            val bucketName = providerDomain.getBucketName(provider)

            when (val contextStorage = createContextStorage(provider, bucketName)) {
                is Failure ->
                    when (contextStorage.value) {
                        CreateContextJCloudError.ErrorCreatingContext -> return@run failure(DownloadFileError.ErrorCreatingContext)
                        CreateContextJCloudError.ErrorCreatingGlobalBucket -> return@run failure(
                            DownloadFileError.ErrorCreatingGlobalBucket,
                        )

                        CreateContextJCloudError.InvalidCredential -> return@run failure(DownloadFileError.InvalidCredential)
                    }

                is Success -> {
                    val pathFile = "$pathSaveFile/${file.fileName}"

                    val downloadFile =
                        jcloudsStorage.downloadBlob(bucketName, file.path, pathFile, contextStorage.value)

                    when (downloadFile) {
                        is Failure -> {
                            contextStorage.value.close()
                            return@run failure(DownloadFileError.ErrorDownloadingFile)
                        }

                        is Success -> {
                            // Read the encrypted file
                            val downloadedFile = java.io.File(pathFile)
                            val downloadedBytes: ByteArray = Files.readAllBytes(downloadedFile.toPath())

                            if (!encryption) {
                                FileOutputStream(pathFile).use { outputStream ->
                                    outputStream.write(downloadedBytes)
                                }
                                return@run success(true)
                            }

                            // Extract IV and encrypted data
                            val iv: ByteArray =
                                Arrays.copyOfRange(downloadedBytes, 0, 12) // The first 12 bytes are the IV
                            val encryptedData: ByteArray =
                                Arrays.copyOfRange(downloadedBytes, 12, downloadedBytes.size)

                            val key =
                                securityService.convertStringToSecretKey(encryptionKey) ?: return@run failure(
                                    DownloadFileError.ErrorDecryptingFile,
                                )

                            val decryptedData: ByteArray =
                                securityService.decrypt(encryptedData, key, iv) ?: return@run failure(
                                    DownloadFileError.ErrorDecryptingFile,
                                )

                            FileOutputStream(pathFile).use { outputStream ->
                                outputStream.write(decryptedData)
                            }

                            success(true)
                        }
                    }
                }
            }
        }

    fun deleteFile(
        user: User,
        fileId: Id,
    ): DeleteFileResult =
        transactionManager.run {
            val usersRepository = it.usersRepository
            val fileRepository = it.fileRepository

            val file = fileRepository.getFileById(user.id, fileId) ?: return@run failure(DeleteFileError.FileNotFound)

            val provider = usersRepository.getProvider(user.id)
            val bucketName = providerDomain.getBucketName(provider)

            when (val contextStorage = createContextStorage(provider, bucketName)) {
                is Failure ->
                    when (contextStorage.value) {
                        CreateContextJCloudError.ErrorCreatingContext -> return@run failure(DeleteFileError.ErrorCreatingContext)
                        CreateContextJCloudError.ErrorCreatingGlobalBucket -> return@run failure(
                            DeleteFileError.ErrorCreatingGlobalBucket,
                        )

                        CreateContextJCloudError.InvalidCredential -> return@run failure(DeleteFileError.InvalidCredential)
                    }

                is Success -> {
                    when (jcloudsStorage.deleteBlob(contextStorage.value, bucketName, file.path)) {
                        is Failure -> return@run failure(DeleteFileError.ErrorDeletingFile)

                        is Success -> {
                            fileRepository.deleteFile(file)
                            success(true)
                        }
                    }
                }
            }
        }

    fun getFiles(user: User): List<File> =
        transactionManager.run {
            it.fileRepository.getFiles(user.id)
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
