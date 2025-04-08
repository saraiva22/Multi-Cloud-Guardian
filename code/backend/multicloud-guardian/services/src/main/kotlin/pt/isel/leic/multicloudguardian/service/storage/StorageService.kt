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
    ): UploadFileResult = handleFileUpload(file, encryption, user)

    fun uploadFileInFolder(
        file: FileCreate,
        encryption: Boolean,
        user: User,
        folderId: Id,
    ): UploadFileResult = handleFileUpload(file, encryption, user, folderId)

    private fun handleFileUpload(
        file: FileCreate,
        encryption: Boolean,
        user: User,
        folderId: Id? = null,
    ): UploadFileResult =
        transactionManager.run {
            val usersRepository = it.usersRepository
            val fileRepository = it.storageRepository

            if (fileRepository.isFileNameInFolder(user.id, folderId, file.blobName)) {
                return@run failure(UploadFileError.FileNameAlreadyExists)
            }

            val folder =
                folderId?.let {
                    fileRepository.getFolderById(user.id, folderId)
                        ?: return@run failure(UploadFileError.ParentFolderNotFound)
                }

            val provider = usersRepository.getProvider(user.id)
            val bucketName = providerDomain.getBucketName(provider)
            val checkSum = securityService.calculateChecksum(file.fileContent)
            when (val contextStorage = createContextStorage(provider, bucketName)) {
                is Failure ->
                    when (contextStorage.value) {
                        CreateContextJCloudError.InvalidCredential -> return@run failure(UploadFileError.InvalidCredential)
                        CreateContextJCloudError.ErrorCreatingContext -> return@run failure(UploadFileError.ErrorCreatingContextUpload)
                        CreateContextJCloudError.ErrorCreatingGlobalBucket -> return@run failure(
                            UploadFileError.ErrorCreatingGlobalBucketUpload,
                        )
                    }

                is Success -> {
                    val secretKey = securityService.generationKeyAES()
                    val iv = securityService.generationIV()

                    val fileContentData =
                        if (encryption) {
                            val encryptedData =
                                securityService.encrypt(file.fileContent, secretKey, iv) ?: return@run failure(
                                    UploadFileError.ErrorEncryptingUploadFile,
                                )
                            iv + encryptedData // Prepend IV to the encrypted data
                        } else {
                            file.fileContent
                        }

                    val basePath =
                        folderId?.let { folder?.path } ?: "${user.username.value}/"
                    val path = "$basePath${file.blobName}"

                    val isUploadFile =
                        jcloudsStorage.uploadBlob(
                            path,
                            fileContentData,
                            file.contentType,
                            contextStorage.value,
                            bucketName,
                            encryption,
                        )

                    when (isUploadFile) {
                        is Failure -> {
                            contextStorage.value.close()
                            return@run failure(UploadFileError.FileStorageError)
                        }

                        is Success -> {
                            val fileId =
                                fileRepository.storeFile(
                                    file,
                                    path,
                                    checkSum,
                                    path,
                                    user.id,
                                    folderId,
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

    fun getFileById(
        user: User,
        fileId: Id,
    ): GetFileResult =
        transactionManager.run {
            val usersRepository = it.usersRepository
            val fileRepository = it.storageRepository
            val file =
                fileRepository.getFileById(user.id, fileId) ?: return@run failure(GetFileByIdError.FileNotFound)

            if (file.encryption) {
                return@run failure(GetFileByIdError.FileIsEncrypted)
            }

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
        user: User,
        fileId: Id,
        pathSaveFile: String,
        encryptionKey: String?,
    ): DownloadFileResult = handleFileDownload(user, fileId, pathSaveFile, encryptionKey)

    fun downloadFileInFolder(
        user: User,
        folderId: Id,
        fileId: Id,
        pathSaveFile: String,
        encryptionKey: String?,
    ): DownloadFileResult = handleFileDownload(user, fileId, pathSaveFile, encryptionKey, folderId)

    private fun handleFileDownload(
        user: User,
        fileId: Id,
        pathSaveFile: String,
        encryptionKey: String? = null,
        folderId: Id? = null,
    ): DownloadFileResult =
        transactionManager.run {
            val usersRepository = it.usersRepository
            val fileRepository = it.storageRepository

            val folder =
                folderId?.let {
                    fileRepository.getFolderById(user.id, folderId)
                        ?: return@run failure(DownloadFileError.ParentFolderNotFound)
                }

            val file =
                fileRepository.getFileById(user.id, fileId) ?: return@run failure(DownloadFileError.FileNotFound)

            if (folder != null && folder.folderId != file.folderId) {
                return@run failure(DownloadFileError.FileNotFound)
            }

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
                    val saveFilePath = "$pathSaveFile/${file.fileName}"
                    val downloadFile =
                        jcloudsStorage.downloadBlob(bucketName, file.path, saveFilePath, contextStorage.value)

                    when (downloadFile) {
                        is Failure -> {
                            contextStorage.value.close()
                            return@run failure(DownloadFileError.ErrorDownloadingFile)
                        }

                        is Success -> {
                            // Read the encrypted file
                            val downloadedFile = java.io.File(saveFilePath)
                            val downloadedBytes: ByteArray = Files.readAllBytes(downloadedFile.toPath())

                            if (!file.encryption) {
                                FileOutputStream(saveFilePath).use { outputStream ->
                                    outputStream.write(downloadedBytes)
                                }
                                return@run success(true)
                            }

                            if (encryptionKey == null) return@run failure(DownloadFileError.InvalidKey)
                            val key =
                                securityService.convertStringToSecretKey(encryptionKey) ?: return@run failure(
                                    DownloadFileError.ErrorDecryptingFile,
                                )

                            // Extract IV and encrypted

                            val iv: ByteArray =
                                Arrays.copyOfRange(downloadedBytes, 0, 12) // The first 12 bytes are the IV
                            val encryptedData: ByteArray =
                                Arrays.copyOfRange(downloadedBytes, 12, downloadedBytes.size)

                            val decryptedData: ByteArray =
                                securityService.decrypt(encryptedData, key, iv) ?: return@run failure(
                                    DownloadFileError.ErrorDecryptingFile,
                                )

                            FileOutputStream(saveFilePath).use { outputStream ->
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
            val fileRepository = it.storageRepository

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
            it.storageRepository.getFiles(user.id)
        }

    fun createFolder(
        folderName: String,
        user: User,
    ): CreationFolderResult = createFolderGeneric(folderName, user)

    fun createFolderInFolder(
        folderName: String,
        user: User,
        folderId: Id,
    ): CreationFolderResult = createFolderGeneric(folderName, user, folderId)

    private fun createFolderGeneric(
        folderName: String,
        user: User,
        folderId: Id? = null,
    ): CreationFolderResult =
        transactionManager.run {
            val usersRepository = it.usersRepository
            val fileRepository = it.storageRepository

            val folder =
                folderId?.let {
                    fileRepository.getFolderById(user.id, folderId)
                        ?: return@run failure(CreationFolderError.ParentFolderNotFound)
                }

            if (fileRepository.isFolderNameExists(user.id, folderId, folderName)) {
                return@run failure(CreationFolderError.FolderNameAlreadyExists)
            }

            val provider = usersRepository.getProvider(user.id)
            val bucketName = providerDomain.getBucketName(provider)

            when (val contextStorage = createContextStorage(provider, bucketName)) {
                is Failure ->
                    when (contextStorage.value) {
                        CreateContextJCloudError.ErrorCreatingContext -> return@run failure(CreationFolderError.ErrorCreatingContext)
                        CreateContextJCloudError.ErrorCreatingGlobalBucket -> return@run failure(
                            CreationFolderError.ErrorCreatingGlobalBucket,
                        )

                        CreateContextJCloudError.InvalidCredential -> return@run failure(CreationFolderError.InvalidCredential)
                    }

                is Success -> {
                    val basePath =
                        folderId?.let { folder?.path } ?: "${user.username.value}/"
                    val path = "$basePath$folderName/"
                    val folderJclouds =
                        jcloudsStorage.createFolder(contextStorage.value, path, bucketName, folderName)

                    when (folderJclouds) {
                        is Failure -> return@run failure(CreationFolderError.ErrorCreatingFolder)

                        is Success -> {
                            val newFolderId =
                                fileRepository.createFolder(user.id, folderName, folderId, path, clock.now())
                            success(newFolderId)
                        }
                    }
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
