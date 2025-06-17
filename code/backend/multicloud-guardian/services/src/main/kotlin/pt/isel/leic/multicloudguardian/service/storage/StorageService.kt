package pt.isel.leic.multicloudguardian.service.storage

import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pt.isel.leic.multicloudguardian.domain.file.File
import pt.isel.leic.multicloudguardian.domain.file.FileCreate
import pt.isel.leic.multicloudguardian.domain.file.FileDownload
import pt.isel.leic.multicloudguardian.domain.folder.Folder
import pt.isel.leic.multicloudguardian.domain.folder.FolderType
import pt.isel.leic.multicloudguardian.domain.folder.InviteStatus
import pt.isel.leic.multicloudguardian.domain.provider.ProviderDomainConfig
import pt.isel.leic.multicloudguardian.domain.provider.ProviderType
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Id
import pt.isel.leic.multicloudguardian.domain.utils.PageResult
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.domain.utils.failure
import pt.isel.leic.multicloudguardian.domain.utils.success
import pt.isel.leic.multicloudguardian.repository.TransactionManager
import pt.isel.leic.multicloudguardian.service.storage.jclouds.CreateBlobStorageContextError
import pt.isel.leic.multicloudguardian.service.storage.jclouds.MoveBlobError
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
                    fileRepository.getFolderById(folderId)
                        ?: return@run failure(UploadFileError.ParentFolderNotFound)
                }

            if (folder != null && folder.type == FolderType.PRIVATE && folder.user.id != user.id) {
                return@run failure(UploadFileError.ParentFolderNotFound)
            }

            val selectUser = folder?.user?.id ?: user.id
            val provider = usersRepository.getProvider(selectUser)

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
                                    updatedAt = if (folderId != null) clock.now() else null,
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
            val fileRepository = it.storageRepository
            val file =
                fileRepository.getFileById(user.id, fileId) ?: return@run failure(GetFileByIdError.FileNotFound)

            success(file)
        }

    fun generateTemporaryFileUrl(
        user: User,
        fileId: Id,
        expiresIn: Long,
    ): CreateTempUrlFileResult =
        transactionManager.run {
            val usersRepository = it.usersRepository
            val fileRepository = it.storageRepository
            val file =
                fileRepository.getFileById(user.id, fileId) ?: return@run failure(CreateTempUrlFileError.FileNotFound)
            if (file.encryption) return@run failure(CreateTempUrlFileError.EncryptedFile)

            val provider = usersRepository.getProvider(user.id)
            val bucketName = providerDomain.getBucketName(provider)
            val credential = providerDomain.getCredential(provider)
            val identity = providerDomain.getIdentity(provider)
            val location = providerDomain.getLocation(provider)

            when (val contextStorage = createContextStorage(provider, bucketName)) {
                is Failure ->
                    when (contextStorage.value) {
                        CreateContextJCloudError.ErrorCreatingContext -> return@run failure(CreateTempUrlFileError.ErrorCreatingContext)
                        CreateContextJCloudError.ErrorCreatingGlobalBucket -> return@run failure(
                            CreateTempUrlFileError.ErrorCreatingGlobalBucket,
                        )

                        CreateContextJCloudError.InvalidCredential -> return@run failure(CreateTempUrlFileError.InvalidCredential)
                    }

                is Success -> {
                    val generatedUrl =
                        jcloudsStorage.generateBlobUrl(provider, bucketName, credential, identity, file.path, location, expiresIn)
                    contextStorage.value.close()
                    success(
                        Pair(file, generatedUrl),
                    )
                }
            }
        }

    fun moveFile(
        user: User,
        fileId: Id,
        folderId: Id?,
    ): MoveFileResult =
        transactionManager.run {
            val usersRepository = it.usersRepository
            val storageRepository = it.storageRepository
            val file =
                storageRepository.getFileById(user.id, fileId) ?: return@run failure(MoveFileError.FileNotFound)

            if (storageRepository.isFileNameInFolder(user.id, folderId, file.fileName)) {
                return@run failure(MoveFileError.FileNameAlreadyExists)
            }

            val destinationPath =
                if (folderId !== null) {
                    val pathFolder = storageRepository.getFolderById(user.id, folderId) ?: return@run failure(MoveFileError.FolderNotFound)
                    pathFolder.path + file.fileName
                } else {
                    "${user.username.value}/${file.fileName}"
                }

            val provider = usersRepository.getProvider(user.id)
            val bucketName = providerDomain.getBucketName(provider)

            when (val contextStorage = createContextStorage(provider, bucketName)) {
                is Failure ->
                    when (contextStorage.value) {
                        CreateContextJCloudError.ErrorCreatingContext -> return@run failure(MoveFileError.ErrorCreatingContext)
                        CreateContextJCloudError.ErrorCreatingGlobalBucket -> return@run failure(
                            MoveFileError.ErrorCreatingGlobalBucket,
                        )
                        CreateContextJCloudError.InvalidCredential -> return@run failure(MoveFileError.InvalidCredential)
                    }
                is Success -> {
                    val moveFile = jcloudsStorage.moveBlob(contextStorage.value, bucketName, file.path, destinationPath)

                    when (moveFile) {
                        is Failure ->
                            when (moveFile.value) {
                                MoveBlobError.ErrorMoveBlob -> return@run failure(MoveFileError.MoveBlobError)
                                MoveBlobError.BlobNotFound -> return@run failure(MoveFileError.MoveBlobNotFound)
                            }
                        is Success -> {
                            contextStorage.value.close()
                            storageRepository.updateFilePath(user.id, file, destinationPath, clock.now(), folderId)
                            success(true)
                        }
                    }
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
                            contextStorage.value.close()
                            val fileDown =
                                FileDownload(
                                    downloadFile.value,
                                    file.fileName,
                                    file.contentType,
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
                            contextStorage.value.close()
                            fileRepository.deleteFile(user.id, file, if (file.folderId != null) clock.now() else null)
                            success(true)
                        }
                    }
                }
            }
        }

    fun deleteFolder(
        user: User,
        folderId: Id,
    ): DeleteFolderResult =
        transactionManager.run {
            val usersRepository = it.usersRepository
            val fileRepository = it.storageRepository

            val folder =
                fileRepository.getFolderById(user.id, folderId)
                    ?: return@run failure(DeleteFolderError.FolderNotFound)

            val provider = usersRepository.getProvider(user.id)
            val bucketName = providerDomain.getBucketName(provider)

            when (val contextStorage = createContextStorage(provider, bucketName)) {
                is Failure ->
                    when (contextStorage.value) {
                        CreateContextJCloudError.ErrorCreatingContext -> return@run failure(DeleteFolderError.ErrorCreatingContext)
                        CreateContextJCloudError.ErrorCreatingGlobalBucket -> return@run failure(
                            DeleteFolderError.ErrorCreatingGlobalBucket,
                        )

                        CreateContextJCloudError.InvalidCredential -> return@run failure(DeleteFolderError.InvalidCredential)
                    }

                is Success -> {
                    when (jcloudsStorage.deleteBlobs(contextStorage.value, bucketName, folder.path)) {
                        is Failure -> return@run failure(DeleteFolderError.ErrorDeletingFolder)

                        is Success -> {
                            contextStorage.value.close()
                            fileRepository.deleteFolder(user.id, folder)
                            success(true)
                        }
                    }
                }
            }
        }

    fun deleteFileInFolder(
        user: User,
        folderId: Id,
        fileId: Id,
    ): DeleteFileResult =
        transactionManager.run {
            val usersRepository = it.usersRepository
            val fileRepository = it.storageRepository

            val folder =
                fileRepository.getFolderById(user.id, folderId)
                    ?: return@run failure(DeleteFileError.ParentFolderNotFound)

            val file =
                fileRepository.getFileInFolder(user.id, folderId, fileId)
                    ?: return@run failure(DeleteFileError.FileNotFound)

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
                            contextStorage.value.close()
                            fileRepository.deleteFile(user.id, file, if (file.folderId != null) clock.now() else null)
                            success(true)
                        }
                    }
                }
            }
        }

    fun inviteFolder(
        folderId: Id,
        user: User,
        guestName: Username,
    ): InviteFolderResult =
        transactionManager.run {
            val storageRepository = it.storageRepository
            val usersRepository = it.usersRepository

            val folder = storageRepository.getFolderById(user.id, folderId) ?: return@run failure(InviteFolderError.FolderNotFound)

            if (folder.type == FolderType.PRIVATE) return@run failure(InviteFolderError.FolderIsPrivate)

            if (folder.user.id != user.id) return@run failure(InviteFolderError.UserIsNotOwner)

            val guest = usersRepository.getUserByUsername(guestName) ?: return@run failure(InviteFolderError.GuestNotFound)

            if (storageRepository.isMemberOfFolder(guest.id, folderId)) return@run failure(InviteFolderError.UserAlreadyInFolder)

            val inviteId = storageRepository.createInviteFolder(user.id, guest.id, folderId)

            success(inviteId)
        }

    fun validateFolderInvite(
        guest: User,
        folderId: Id,
        inviteId: Id,
        inviteStatus: InviteStatus,
    ): ValidateFolderInviteResult =
        transactionManager.run {
            val storageRepository = it.storageRepository

            val folder = storageRepository.getFolderById(folderId) ?: return@run failure(ValidateFolderInviteError.FolderNotFound)

            if (folder.type == FolderType.PRIVATE) return@run failure(ValidateFolderInviteError.FolderIsPrivate)

            if (storageRepository.isMemberOfFolder(guest.id, folderId)) return@run failure(ValidateFolderInviteError.UserAlreadyInFolder)

            if (!storageRepository.isInviteCodeValid(guest.id, folderId, inviteId)) {
                return@run failure(ValidateFolderInviteError.InvalidInvite)
            }

            storageRepository.folderInviteUpdated(guest.id, inviteId, inviteStatus)

            success(folder)
        }

    fun getFiles(
        user: User,
        limit: Int,
        page: Int,
        sort: String,
    ): PageResult<File> =
        transactionManager.run {
            val storageRep = it.storageRepository
            val offset = page * limit
            val files = storageRep.getFiles(user.id, limit, offset, sort)
            val totalElements = storageRep.countFiles(user.id)
            PageResult.fromPartialResult(files, totalElements, limit, offset)
        }

    fun getFolders(
        user: User,
        limit: Int,
        page: Int,
        sort: String,
    ): PageResult<Folder> =
        transactionManager.run {
            val storageRep = it.storageRepository
            val totalElements = storageRep.countFolder(user.id)
            val offset = page * limit
            val folders = storageRep.getFolders(user.id, limit, offset, sort)

            PageResult.fromPartialResult(folders, totalElements, limit, offset)
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

    fun getFoldersInFolder(
        user: User,
        folderId: Id,
        limit: Int,
        page: Int,
        sort: String,
    ): GetFoldersInFolderResult =
        transactionManager.run {
            val storageRep = it.storageRepository
            storageRep.getFolderById(user.id, folderId) ?: return@run failure(GetFoldersInFolderError.FolderNotFound)
            val offset = page * limit
            val result = storageRep.getFoldersInFolder(user.id, folderId, limit, offset, sort)
            val folders = result.first
            val totalElements = result.second
            success(PageResult.fromPartialResult(folders, totalElements, limit, offset))
        }

    fun getFilesInFolder(
        user: User,
        folderId: Id,
        limit: Int,
        page: Int,
        sort: String,
    ): GetFilesInFolderResult =
        transactionManager.run {
            val folder =
                it.storageRepository.getFolderById(user.id, folderId) ?: return@run failure(
                    GetFilesInFolderError.FolderNotFound,
                )
            val offset = page * limit
            val files = it.storageRepository.getFilesInFolder(user.id, folderId, limit, offset, sort)

            success(PageResult.fromPartialResult(files, folder.numberFiles.toLong(), limit, offset))
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
        folderType: FolderType,
    ): CreationFolderResult = createFolderGeneric(folderName, user, folderType)

    fun createFolderInFolder(
        folderName: String,
        user: User,
        folderType: FolderType,
        folderId: Id,
    ): CreationFolderResult = createFolderGeneric(folderName, user, folderType, folderId)

    private fun createFolderGeneric(
        folderName: String,
        user: User,
        folderType: FolderType,
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
                            contextStorage.value.close()
                            val newFolderId =
                                fileRepository.createFolder(user.id, folderName, folderId, path, folderType, clock.now())
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
