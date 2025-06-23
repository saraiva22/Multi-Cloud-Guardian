package pt.isel.leic.multicloudguardian.http.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import pt.isel.leic.multicloudguardian.domain.user.AuthenticatedUser
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Id
import pt.isel.leic.multicloudguardian.domain.utils.PageResult
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.http.Uris
import pt.isel.leic.multicloudguardian.http.media.Problem
import pt.isel.leic.multicloudguardian.http.model.storage.DownloadFileOutputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FileCreateInputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FileInfoOutputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FilesListOutputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FolderCreateInputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FolderInfoOutputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FolderInviteInput
import pt.isel.leic.multicloudguardian.http.model.storage.FolderInviteStatusInputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FolderPrivateInviteListOutputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FolderPrivateInviteOutputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FoldersListOutputModel
import pt.isel.leic.multicloudguardian.http.model.utils.IdOutputModel
import pt.isel.leic.multicloudguardian.service.storage.CreationFolderError
import pt.isel.leic.multicloudguardian.service.storage.DeleteFileError
import pt.isel.leic.multicloudguardian.service.storage.DeleteFolderError
import pt.isel.leic.multicloudguardian.service.storage.DownloadFileError
import pt.isel.leic.multicloudguardian.service.storage.GetFileInFolderError
import pt.isel.leic.multicloudguardian.service.storage.GetFilesInFolderError
import pt.isel.leic.multicloudguardian.service.storage.GetFolderByIdError
import pt.isel.leic.multicloudguardian.service.storage.GetFoldersInFolderError
import pt.isel.leic.multicloudguardian.service.storage.InviteFolderError
import pt.isel.leic.multicloudguardian.service.storage.LeaveFolderError
import pt.isel.leic.multicloudguardian.service.storage.StorageService
import pt.isel.leic.multicloudguardian.service.storage.UploadFileError
import pt.isel.leic.multicloudguardian.service.storage.ValidateFolderInviteError

@RestController
class FoldersController(
    private val storageService: StorageService,
) {
    @PostMapping(Uris.Folders.CREATE)
    fun createFolder(
        @Validated @RequestBody input: FolderCreateInputModel,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Folders.register()
        return when (val res = storageService.createFolder(input.folderName, authenticatedUser.user, input.folderType)) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header(
                        "Location",
                        Uris.Folders.folderById(res.value.value).toASCIIString(),
                    ).body(IdOutputModel(res.value.value))

            is Failure ->
                when (res.value) {
                    CreationFolderError.ErrorCreatingContext -> Problem.invalidCreateContext(instance)
                    CreationFolderError.ErrorCreatingGlobalBucket -> Problem.invalidCreateContext(instance)
                    CreationFolderError.InvalidCredential -> Problem.invalidCredential(instance)
                    CreationFolderError.ErrorCreatingFolder -> Problem.invalidFolderCreation(instance)
                    CreationFolderError.FolderNameAlreadyExists ->
                        Problem.folderNameAlreadyExists(input.folderName, instance)

                    CreationFolderError.ParentFolderNotFound ->
                        Problem.parentFolderNotFound(0, instance)
                    CreationFolderError.FolderIsShared ->
                        Problem.folderIsShared(0, instance)
                }
        }
    }

    @GetMapping(Uris.Folders.GET_FOLDERS)
    fun getFolder(
        authenticatedUser: AuthenticatedUser,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) shared: Boolean = false,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<*> {
        val setLimit = size ?: DEFAULT_LIMIT
        val setPage = page ?: DEFAULT_PAGE
        val setSort = sort ?: DEFAULT_SORT
        val res = storageService.getFolders(authenticatedUser.user, setLimit, setPage, setSort, shared, search)
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                PageResult(
                    content = FoldersListOutputModel(res.content.map { FolderInfoOutputModel.fromDomain(it) }).folders,
                    pageable = res.pageable,
                    totalElements = res.totalElements,
                    totalPages = res.totalPages,
                    last = res.last,
                    first = res.first,
                    size = res.size,
                    number = res.number,
                ),
            )
    }

    @PostMapping(Uris.Folders.CREATE_FOLDER_IN_FOLDER)
    fun createFolderInFolder(
        @Validated @RequestBody input: FolderCreateInputModel,
        @PathVariable folderId: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Folders.createFolderInFolder(folderId)
        return when (
            val res =
                storageService.createFolderInFolder(
                    input.folderName,
                    authenticatedUser.user,
                    input.folderType,
                    Id(folderId),
                )
        ) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header(
                        "Location",
                        Uris.Folders.createFolderInFolder(res.value.value).toASCIIString(),
                    ).body(IdOutputModel(res.value.value))

            is Failure ->
                when (res.value) {
                    CreationFolderError.ErrorCreatingContext -> Problem.invalidCreateContext(instance)
                    CreationFolderError.ErrorCreatingGlobalBucket -> Problem.invalidCreateContext(instance)
                    CreationFolderError.InvalidCredential -> Problem.invalidCredential(instance)
                    CreationFolderError.ErrorCreatingFolder -> Problem.invalidFolderCreation(instance)
                    CreationFolderError.FolderNameAlreadyExists ->
                        Problem.folderNameAlreadyExists(input.folderName, instance)
                    CreationFolderError.ParentFolderNotFound ->
                        Problem.parentFolderNotFound(folderId, instance)
                    CreationFolderError.FolderIsShared ->
                        Problem.folderIsShared(folderId, instance)
                }
        }
    }

    @GetMapping(Uris.Folders.GET_FOLDER_BY_ID)
    fun getFolder(
        @PathVariable @Validated folderId: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Folders.folderById(folderId)
        val res = storageService.getFolderById(authenticatedUser.user, Id(folderId))
        return when (res) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(FolderInfoOutputModel.fromDomain(res.value))

            is Failure ->
                when (res.value) {
                    GetFolderByIdError.FolderNotFound -> Problem.folderNotFound(folderId, instance)
                }
        }
    }

    @GetMapping(Uris.Folders.GET_FOLDERS_IN_FOLDER)
    fun getFoldersInFolder(
        @PathVariable @Validated folderId: Int,
        authenticatedUser: AuthenticatedUser,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<*> {
        val instance = Uris.Folders.folderById(folderId)
        val setLimit = size ?: DEFAULT_LIMIT
        val setPage = page ?: DEFAULT_PAGE
        val setSort = sort ?: DEFAULT_SORT
        val res = storageService.getFoldersInFolder(authenticatedUser.user, Id(folderId), setLimit, setPage, setSort)
        return when (res) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(
                        PageResult(
                            content = FoldersListOutputModel(res.value.content.map { FolderInfoOutputModel.fromDomain((it)) }).folders,
                            pageable = res.value.pageable,
                            totalElements = res.value.totalElements,
                            totalPages = res.value.totalPages,
                            last = res.value.last,
                            first = res.value.first,
                            size = res.value.size,
                            number = res.value.number,
                        ),
                    )
            is Failure ->
                when (res.value) {
                    GetFoldersInFolderError.FolderNotFound -> Problem.folderNotFound(folderId, instance)
                    GetFoldersInFolderError.FolderIsShared -> Problem.folderIsShared(folderId, instance)
                }
        }
    }

    @GetMapping(Uris.Folders.GET_FILES_IN_FOLDER)
    fun getFilesInFolder(
        @PathVariable @Validated folderId: Int,
        authenticatedUser: AuthenticatedUser,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<*> {
        val instance = Uris.Folders.filesInFolder(folderId)
        val setLimit = size ?: DEFAULT_LIMIT
        val setPage = page ?: DEFAULT_PAGE
        val setSort = sort ?: DEFAULT_SORT
        val res = storageService.getFilesInFolder(authenticatedUser.user, Id(folderId), setLimit, setPage, setSort)
        return when (res) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(
                        PageResult(
                            content = FilesListOutputModel(res.value.content.map { FileInfoOutputModel.fromDomain(it) }).files,
                            pageable = res.value.pageable,
                            totalElements = res.value.totalElements,
                            totalPages = res.value.totalPages,
                            last = res.value.last,
                            first = res.value.first,
                            size = res.value.size,
                            number = res.value.number,
                        ),
                    )

            is Failure ->
                when (res.value) {
                    GetFilesInFolderError.FolderNotFound -> Problem.folderNotFound(folderId, instance)
                    GetFilesInFolderError.NotMemberOfFolder -> Problem.notMemberOfFolder(folderId, instance)
                }
        }
    }

    @GetMapping(Uris.Folders.GET_FILE_IN_FOLDER)
    fun getFileInFolder(
        @PathVariable @Validated folderId: Int,
        @PathVariable @Validated fileId: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Folders.fileInFolder(folderId, fileId)
        val res = storageService.getFileInFolder(authenticatedUser.user, Id(folderId), Id(fileId))
        return when (res) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(FileInfoOutputModel.fromDomain(res.value))

            is Failure ->
                when (res.value) {
                    GetFileInFolderError.FolderNotFound -> Problem.folderNotFound(folderId, instance)
                    GetFileInFolderError.FileNotFound -> Problem.fileNotFound(fileId, instance)
                }
        }
    }

    @PostMapping(Uris.Folders.UPLOAD_FILE_IN_FOLDER)
    fun uploadFileInFolder(
        @RequestParam("file") fileMultiPart: MultipartFile,
        @RequestParam("encryption") encryption: Boolean,
        @RequestParam("encryptedKey") encryptedKey: String,
        @Validated @PathVariable folderId: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Folders.uploadFileInFolder(folderId)
        val fileCreateInputModel = FileCreateInputModel(fileMultiPart, encryption, encryptedKey)
        val fileDomain = fileCreateInputModel.toDomain()
        val file =
            storageService.uploadFileInFolder(
                fileDomain,
                encryption,
                authenticatedUser.user,
                Id(folderId),
            )
        return when (file) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header(
                        "Location",
                        Uris.Files.byId(file.value.value).toASCIIString(),
                    ).body(IdOutputModel(file.value.value))

            is Failure ->
                when (file.value) {
                    UploadFileError.FileStorageError ->
                        Problem.invalidCreationStorage(
                            instance,
                        )

                    UploadFileError.ErrorCreatingGlobalBucketUpload ->
                        Problem.invalidCreationGlobalBucket(
                            instance,
                        )

                    UploadFileError.ErrorCreatingContextUpload ->
                        Problem.invalidCreateContext(instance)

                    UploadFileError.FileNameAlreadyExists ->
                        Problem.invalidFileName(fileDomain.blobName, instance)

                    UploadFileError.FileStorageError ->
                        Problem.invalidCreationStorage(instance)

                    UploadFileError.InvalidCredential ->
                        Problem.invalidCredential(instance)

                    UploadFileError.ParentFolderNotFound -> Problem.parentFolderNotFound(folderId, instance)
                }
        }
    }

    @GetMapping(Uris.Folders.DOWNLOAD_FILE_IN_FOLDER)
    fun downloadFileInFolder(
        @Validated @PathVariable folderId: Int,
        @Validated @PathVariable fileId: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Folders.downloadFileInFolder(folderId, fileId)
        val user = authenticatedUser.user
        return when (
            val res =
                storageService.downloadFileInFolder(
                    user,
                    Id(folderId),
                    Id(fileId),
                )
        ) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(
                        DownloadFileOutputModel(
                            res.value.first,
                            res.value.second,
                        ),
                    )

            is Failure ->
                when (res.value) {
                    DownloadFileError.ErrorDownloadingFile -> Problem.invalidDownloadFile(instance)
                    DownloadFileError.FileNotFound -> Problem.fileNotFound(fileId, instance)
                    DownloadFileError.ErrorCreatingContext -> Problem.invalidCreateContext(instance)
                    DownloadFileError.ErrorCreatingGlobalBucket -> Problem.invalidCreationGlobalBucket(instance)
                    DownloadFileError.InvalidCredential -> Problem.invalidCredential(instance)
                    DownloadFileError.ParentFolderNotFound -> Problem.parentFolderNotFound(folderId, instance)
                    DownloadFileError.InvalidKey -> Problem.invalidKey(instance)
                }
        }
    }

    @DeleteMapping(Uris.Folders.DELETE_FOLDER)
    fun deleteFolder(
        @PathVariable @Validated folderId: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Folders.deleteFolder(folderId)
        val user = authenticatedUser.user
        return when (val res = storageService.deleteFolder(user, Id(folderId))) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .header("Location", instance.toASCIIString())
                    .build<Unit>()
            is Failure ->
                when (res.value) {
                    DeleteFolderError.FolderNotFound -> Problem.folderNotFound(folderId, instance)
                    DeleteFolderError.InvalidCredential -> Problem.invalidCredential(instance)
                    DeleteFolderError.ErrorCreatingContext -> Problem.invalidCreateContext(instance)
                    DeleteFolderError.ErrorCreatingGlobalBucket -> Problem.invalidCreationGlobalBucket(instance)
                    DeleteFolderError.ErrorDeletingFolder -> Problem.invalidDeleteFile(instance)
                    DeleteFolderError.PermissionDenied ->
                        Problem.userPermissionsDeniedType(authenticatedUser.user.username.value, instance)
                }
        }
    }

    @DeleteMapping(Uris.Folders.DELETE_FILE_IN_FOLDER)
    fun deleteFileInFolder(
        @PathVariable @Validated folderId: Int,
        @PathVariable @Validated fileId: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Folders.deleteFileInFolder(folderId, fileId)
        val user = authenticatedUser.user
        return when (val res = storageService.deleteFileInFolder(user, Id(folderId), Id(fileId))) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .header("Location", instance.toASCIIString())
                    .build<Unit>()
            is Failure ->
                when (res.value) {
                    DeleteFileError.FileNotFound -> Problem.fileNotFound(fileId, instance)
                    DeleteFileError.InvalidCredential -> Problem.invalidCredential(instance)
                    DeleteFileError.ErrorCreatingContext -> Problem.invalidCreateContext(instance)
                    DeleteFileError.ErrorCreatingGlobalBucket -> Problem.invalidCreationGlobalBucket(instance)
                    DeleteFileError.ErrorDeletingFile -> Problem.invalidDeleteFile(instance)
                    DeleteFileError.ParentFolderNotFound -> Problem.parentFolderNotFound(folderId, instance)
                    DeleteFileError.PermissionDenied ->
                        Problem.userPermissionsDeniedType(authenticatedUser.user.username.value, instance)
                }
        }
    }

    @PostMapping(Uris.Folders.CREATE_INVITE_FOLDER)
    fun inviteFolder(
        @PathVariable @Validated folderId: Int,
        @RequestBody input: FolderInviteInput,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Folders.inviteFolder(folderId)
        val folderShared =
            storageService.inviteFolder(Id(folderId), authenticatedUser.user, Username(input.username))
        return when (folderShared) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(IdOutputModel(folderShared.value.value))
            is Failure ->
                when (folderShared.value) {
                    InviteFolderError.FolderIsPrivate -> Problem.folderIsPrivate(folderId, instance)
                    InviteFolderError.FolderNotFound -> Problem.folderNotFound(folderId, instance)
                    InviteFolderError.UserAlreadyInFolder -> Problem.userAlreadyInFolder(input.username, instance)
                    InviteFolderError.GuestNotFound -> Problem.userNotFoundByUsername(input.username, instance)
                    InviteFolderError.UserIsNotOwner -> Problem.userIsNotFolderOwner(authenticatedUser.user.username.value, instance)
                }
        }
    }

    @PostMapping(Uris.Folders.VALIDATE_FOLDER_INVITE)
    fun validateFolderInvite(
        @PathVariable @Validated folderId: Int,
        @PathVariable @Validated inviteId: Int,
        @RequestBody input: FolderInviteStatusInputModel,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Folders.validateFolderInvite(folderId, inviteId)
        val invite = storageService.validateFolderInvite(authenticatedUser.user, Id(folderId), Id(inviteId), input.inviteStatus)
        return when (invite) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(FolderInfoOutputModel.fromDomain(invite.value))

            is Failure ->
                when (invite.value) {
                    ValidateFolderInviteError.FolderIsPrivate -> Problem.folderIsPrivate(folderId, instance)
                    ValidateFolderInviteError.InvalidInvite -> Problem.invalidInviteFolder(folderId, instance)
                    ValidateFolderInviteError.FolderNotFound -> Problem.folderNotFound(folderId, instance)
                    ValidateFolderInviteError.UserAlreadyInFolder ->
                        Problem.userAlreadyInFolder(authenticatedUser.user.username.value, instance)
                }
        }
    }

    @GetMapping(Uris.Folders.RECEIVED_FOLDER_INVITES)
    fun getReceivedFolderInvites(
        authenticatedUser: AuthenticatedUser,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<*> {
        val setLimit = size ?: DEFAULT_LIMIT
        val setPage = page ?: DEFAULT_PAGE
        val setSort = sort ?: DEFAULT_SORT
        val res = storageService.getReceivedFolderInvites(authenticatedUser.user, setLimit, setPage, setSort)
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                PageResult(
                    content = FolderPrivateInviteListOutputModel(res.content.map { FolderPrivateInviteOutputModel.fromDomain(it) }).invites,
                    pageable = res.pageable,
                    totalElements = res.totalElements,
                    totalPages = res.totalPages,
                    last = res.last,
                    first = res.first,
                    size = res.size,
                    number = res.number,
                ),
            )
    }

    @GetMapping(Uris.Folders.SENT_FOLDER_INVITES)
    fun getSentFolderInvites(
        authenticatedUser: AuthenticatedUser,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<*> {
        val setLimit = size ?: DEFAULT_LIMIT
        val setPage = page ?: DEFAULT_PAGE
        val setSort = sort ?: DEFAULT_SORT
        val res = storageService.getSentFolderInvites(authenticatedUser.user, setLimit, setPage, setSort)
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                PageResult(
                    content = FolderPrivateInviteListOutputModel(res.content.map { FolderPrivateInviteOutputModel.fromDomain(it) }).invites,
                    pageable = res.pageable,
                    totalElements = res.totalElements,
                    totalPages = res.totalPages,
                    last = res.last,
                    first = res.first,
                    size = res.size,
                    number = res.number,
                ),
            )
    }

    @PostMapping(Uris.Folders.LEAVE_SHARED_FOLDER)
    fun leaveFolder(
        @PathVariable @Validated folderId: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Folders.leaveFolder(folderId)
        return when (val res = storageService.leaveFolder(authenticatedUser.user, Id(folderId))) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .header("Location", instance.toASCIIString())
                    .build<Unit>()
            is Failure ->
                when (res.value) {
                    LeaveFolderError.FolderNotFound -> Problem.folderNotFound(folderId, instance)
                    LeaveFolderError.FolderIsPrivate -> Problem.folderIsPrivate(folderId, instance)
                    LeaveFolderError.UserNotInFolder ->
                        Problem.userNotFoundInFolder(
                            authenticatedUser.user.username.value,
                            folderId,
                            instance,
                        )
                    LeaveFolderError.ErrorLeavingFolder -> Problem.errorLeavingFolder(folderId, instance)
                }
        }
    }

    companion object {
        private const val DEFAULT_LIMIT = 10
        private const val DEFAULT_PAGE = 0
        private const val DEFAULT_SORT = "created_asc"
    }
}
