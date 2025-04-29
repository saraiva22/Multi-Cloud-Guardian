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
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Id
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.http.Uris
import pt.isel.leic.multicloudguardian.http.media.Problem
import pt.isel.leic.multicloudguardian.http.model.storage.DownloadFileOutputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FileCreateInputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FileInfoOutputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FilesListOutputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FolderCreateInputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FolderInfoOutputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FoldersListOutputModel
import pt.isel.leic.multicloudguardian.http.model.utils.IdOutputModel
import pt.isel.leic.multicloudguardian.service.storage.CreationFolderError
import pt.isel.leic.multicloudguardian.service.storage.DeleteFileError
import pt.isel.leic.multicloudguardian.service.storage.DownloadFileError
import pt.isel.leic.multicloudguardian.service.storage.GetFileByIdError
import pt.isel.leic.multicloudguardian.service.storage.GetFileInFolderError
import pt.isel.leic.multicloudguardian.service.storage.GetFilesInFolderError
import pt.isel.leic.multicloudguardian.service.storage.GetFolderByIdError
import pt.isel.leic.multicloudguardian.service.storage.StorageService
import pt.isel.leic.multicloudguardian.service.storage.UploadFileError

@RestController
class StorageController(
    private val storageService: StorageService,
) {
    @PostMapping(Uris.Files.UPLOAD)
    fun uploadFile(
        @RequestParam("file") fileMultiPart: MultipartFile,
        @RequestParam("encryption") encryption: Boolean,
        @RequestParam("encryptedKey") encryptedKey: String,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val fileCreateInputModel = FileCreateInputModel(fileMultiPart, encryption, encryptedKey)
        val instance = Uris.Files.uploadFile()
        val fileDomain = fileCreateInputModel.toDomain()
        val file =
            storageService.uploadFile(
                fileDomain,
                encryption,
                authenticatedUser.user,
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

                    UploadFileError.ParentFolderNotFound -> Problem.parentFolderNotFound(0, instance)
                }
        }
    }

    @GetMapping(Uris.Files.GET_FILES)
    fun getFiles(authenticatedUser: AuthenticatedUser): ResponseEntity<*> {
        val res = storageService.getFiles(authenticatedUser.user)
        return ResponseEntity.status(HttpStatus.OK).body(
            FilesListOutputModel(
                res.map { (FileInfoOutputModel.fromDomain(it)) },
            ),
        )
    }

    @GetMapping(Uris.Files.GET_BY_ID)
    fun getFileById(
        @Validated @PathVariable fileId: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Files.byId(fileId)
        val user = authenticatedUser.user
        return when (val res = storageService.getFileById(user, Id(fileId))) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(
                        FileInfoOutputModel.fromDomain(res.value.first, res.value.second),
                    )

            is Failure ->
                when (res.value) {
                    GetFileByIdError.FileNotFound -> Problem.fileNotFound(fileId, instance)
                    GetFileByIdError.ErrorCreatingContext -> Problem.invalidCreateContext(instance)
                    GetFileByIdError.ErrorCreatingGlobalBucket -> Problem.invalidCreationGlobalBucket(instance)
                    GetFileByIdError.InvalidCredential -> Problem.invalidCredential(instance)
                }
        }
    }

    @GetMapping(Uris.Files.DOWNLOAD_FILE)
    fun downloadFile(
        @Validated @PathVariable fileId: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Files.downloadFile(fileId)
        val user = authenticatedUser.user
        return when (
            val res =
                storageService.downloadFile(
                    user,
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
                    DownloadFileError.ErrorDecryptingFile -> Problem.invalidDecryptFile(instance)
                    DownloadFileError.InvalidKey -> Problem.invalidKey(instance)
                    DownloadFileError.ParentFolderNotFound -> Problem.parentFolderNotFound(0, instance)
                    DownloadFileError.MetadataNotFound -> Problem.metadataNotFound(fileId, instance)
                }
        }
    }

    @DeleteMapping(Uris.Files.DELETE)
    fun deleteFile(
        @Validated @PathVariable fileId: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Files.deleteFile(fileId)
        val user = authenticatedUser.user
        return when (val res = storageService.deleteFile(user, Id(fileId))) {
            is Success -> ResponseEntity.status(HttpStatus.OK).build<Unit>()
            is Failure ->
                when (res.value) {
                    DeleteFileError.FileNotFound -> Problem.fileNotFound(fileId, instance)
                    DeleteFileError.ErrorDeletingFile -> Problem.invalidDeleteFile(instance)
                    DeleteFileError.InvalidCredential -> Problem.invalidCredential(instance)
                    DeleteFileError.ErrorCreatingContext -> Problem.invalidCreateContext(instance)
                    DeleteFileError.ErrorCreatingGlobalBucket -> Problem.invalidCreationGlobalBucket(instance)
                }
        }
    }

    @PostMapping(Uris.Folders.CREATE)
    fun createFolder(
        @Validated @RequestBody input: FolderCreateInputModel,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Folders.register()
        return when (val res = storageService.createFolder(input.folderName, authenticatedUser.user)) {
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
                }
        }
    }

    @GetMapping(Uris.Folders.GET_FOLDERS)
    fun getFolder(authenticatedUser: AuthenticatedUser): ResponseEntity<*> {
        val res = storageService.getFolder(authenticatedUser.user)
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(FoldersListOutputModel(res.map { FolderInfoOutputModel.fromDomain(it) }))
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

    @GetMapping(Uris.Folders.GET_FILES_IN_FOLDER)
    fun getFilesInFolder(
        @PathVariable @Validated folderId: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Folders.filesInFolder(folderId)
        val res = storageService.getFilesInFolder(authenticatedUser.user, Id(folderId))
        return when (res) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(FilesListOutputModel(res.value.map { FileInfoOutputModel.fromDomain(it) }))

            is Failure ->
                when (res.value) {
                    GetFilesInFolderError.FolderNotFound -> Problem.folderNotFound(folderId, instance)
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

    @GetMapping(Uris.Folders.DOWNLOAD_FOLDER)
    fun downloadFolder(): ResponseEntity<*> {
        TODO()
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
                    DownloadFileError.ErrorDecryptingFile -> Problem.invalidDecryptFile(instance)
                    DownloadFileError.ParentFolderNotFound -> Problem.parentFolderNotFound(folderId, instance)
                    DownloadFileError.InvalidKey -> Problem.invalidKey(instance)
                    DownloadFileError.MetadataNotFound -> Problem.metadataNotFound(fileId, instance)
                }
        }
    }

    @DeleteMapping(Uris.Folders.DELETE_FOLDER)
    fun deleteFolder(): ResponseEntity<*> {
        TODO()
    }

    @DeleteMapping(Uris.Folders.DELETE_FILE_IN_FOLDER)
    fun deleteFileInFolder(): ResponseEntity<*> {
        TODO()
    }
}
