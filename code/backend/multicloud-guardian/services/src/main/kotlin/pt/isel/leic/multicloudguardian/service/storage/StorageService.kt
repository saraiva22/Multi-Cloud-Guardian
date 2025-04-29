package pt.isel.leic.multicloudguardian.service.storage

import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pt.isel.leic.multicloudguardian.domain.file.File
import pt.isel.leic.multicloudguardian.domain.file.FileCreate
import pt.isel.leic.multicloudguardian.domain.file.FileDownload
import pt.isel.leic.multicloudguardian.domain.folder.Folder
import pt.isel.leic.multicloudguardian.domain.provider.ProviderDomainConfig
import pt.isel.leic.multicloudguardian.domain.provider.ProviderType
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Id
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.domain.utils.failure
import pt.isel.leic.multicloudguardian.domain.utils.success
import pt.isel.leic.multicloudguardian.repository.TransactionManager
import pt.isel.leic.multicloudguardian.service.storage.jclouds.CreateBlobStorageContextError
import pt.isel.leic.multicloudguardian.service.storage.jclouds.StorageFileJclouds

@Service
class StorageService(
    private val transactionManager: TransactionManager,
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
                    val fileContentData = file.fileContent

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
                                    path,
                                    user.id,
                                    folderId,
                                    encryption,
                                    clock.now(),
                                )
                            contextStorage.value.close()
                            success(fileId)
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
                success(Pair(file, null))
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
    ): DownloadFileResult = handleFileDownload(user, fileId)

    fun downloadFileInFolder(
        user: User,
        folderId: Id,
        fileId: Id,
    ): DownloadFileResult = handleFileDownload(user, fileId, folderId)

    private fun handleFileDownload(
        user: User,
        fileId: Id,
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
                    val downloadFile =
                        jcloudsStorage.downloadBlob(bucketName, file.path, contextStorage.value)

                    when (downloadFile) {
                        is Failure -> {
                            contextStorage.value.close()
                            return@run failure(DownloadFileError.ErrorDownloadingFile)
                        }

                        is Success -> {
                            val metadata =
                                fileRepository.getMetadataByFile(fileId)
                                    ?: return@run failure(DownloadFileError.MetadataNotFound)
                            val fileDown =
                                FileDownload(
                                    downloadFile.value,
                                    file.fileName,
                                    metadata.contentType,
                                    file.encryption,
                                )
                            if (!file.encryption) {
                                return@run success(Pair(fileDown, null))
                            }

                            // Here I have to search the key in the database
                            val fileKey = file.encryptionKey ?: return@run failure(DownloadFileError.InvalidKey)
                            success(Pair(fileDown, fileKey))
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

    fun getFolder(user: User): List<Folder> =
        transactionManager.run {
            it.storageRepository.getFolders(user.id)
        }

    fun getFolderById(
        user: User,
        folderId: Id,
    ): GetFolderResult =
        transactionManager.run {
            val folder =
                it.storageRepository.getFolderById(user.id, folderId)
                    ?: return@run failure(GetFolderByIdError.FolderNotFound)
            success(folder)
        }

    fun getFilesInFolder(
        user: User,
        folderId: Id,
    ): GetFilesInFolderResult =
        transactionManager.run {
            it.storageRepository.getFolderById(user.id, folderId) ?: return@run failure(
                GetFilesInFolderError.FolderNotFound,
            )

            val files = it.storageRepository.getFilesInFolder(user.id, folderId)
            success(files)
        }

    fun getFileInFolder(
        user: User,
        folderId: Id,
        fileId: Id,
    ): GetFileInFolderResult =
        transactionManager.run {
            val storageRepository = it.storageRepository
            storageRepository.getFolderById(user.id, folderId)
                ?: return@run failure(GetFileInFolderError.FolderNotFound)
            val file =
                storageRepository.getFileInFolder(user.id, folderId, fileId)
                    ?: return@run failure(GetFileInFolderError.FileNotFound)
            success(file)
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
