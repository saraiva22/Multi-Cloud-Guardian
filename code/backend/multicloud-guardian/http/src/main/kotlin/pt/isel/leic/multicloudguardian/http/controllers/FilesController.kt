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
import pt.isel.leic.multicloudguardian.domain.utils.PageResult
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.http.Uris
import pt.isel.leic.multicloudguardian.http.media.Problem
import pt.isel.leic.multicloudguardian.http.model.storage.DownloadFileOutputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FileCreateInputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FileInfoOutputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FilesListOutputModel
import pt.isel.leic.multicloudguardian.http.model.storage.GenerateTempUrlInputModel
import pt.isel.leic.multicloudguardian.http.model.storage.MoveFileInputModel
import pt.isel.leic.multicloudguardian.http.model.utils.IdOutputModel
import pt.isel.leic.multicloudguardian.service.storage.CreateTempUrlFileError
import pt.isel.leic.multicloudguardian.service.storage.DeleteFileError
import pt.isel.leic.multicloudguardian.service.storage.DownloadFileError
import pt.isel.leic.multicloudguardian.service.storage.GetFileByIdError
import pt.isel.leic.multicloudguardian.service.storage.MoveFileError
import pt.isel.leic.multicloudguardian.service.storage.StorageService
import pt.isel.leic.multicloudguardian.service.storage.UploadFileError

@RestController
class FilesController(
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
    fun getFiles(
        authenticatedUser: AuthenticatedUser,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) sort: String?,
    ): ResponseEntity<*> {
        val setLimit = size ?: DEFAULT_LIMIT
        val setPage = page ?: DEFAULT_PAGE
        val setSort = sort ?: DEFAULT_SORT
        val res = storageService.getFiles(authenticatedUser.user, setLimit, setPage, setSort)

        return ResponseEntity.status(HttpStatus.OK).body(
            PageResult(
                content =
                    FilesListOutputModel(
                        res.content.map { FileInfoOutputModel.fromDomain(it) },
                    ).files,
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
                    .body(FileInfoOutputModel.fromDomain(res.value))

            is Failure ->
                when (res.value) {
                    GetFileByIdError.FileNotFound -> Problem.fileNotFound(fileId, instance)
                }
        }
    }

    @PostMapping(Uris.Files.CREATE_URL)
    fun generateTemporaryFileUrl(
        @Validated @PathVariable fileId: Int,
        @RequestBody input: GenerateTempUrlInputModel,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Files.createUrl(fileId)
        val res = storageService.generateTemporaryFileUrl(authenticatedUser.user, Id(fileId), input.expiresIn)
        return when (res) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(FileInfoOutputModel.fromDomain(res.value.first, res.value.second))

            is Failure ->
                when (res.value) {
                    CreateTempUrlFileError.FileNotFound -> Problem.fileNotFound(fileId, instance)
                    CreateTempUrlFileError.EncryptedFile -> Problem.fileIsEncrypted(fileId, instance)
                    CreateTempUrlFileError.ErrorCreatingContext -> Problem.invalidCreateContext(instance)
                    CreateTempUrlFileError.ErrorCreatingGlobalBucket -> Problem.invalidCreationGlobalBucket(instance)
                    CreateTempUrlFileError.InvalidCredential -> Problem.invalidCredential(instance)
                }
        }
    }

    @PostMapping(Uris.Files.MOVE_FILE)
    fun moveFile(
        @Validated @PathVariable fileId: Int,
        @RequestBody input: MoveFileInputModel,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Files.moveFile(fileId)
        val folderIdOrNull = input.folderId?.let { Id(it) }
        val res = storageService.moveFile(authenticatedUser.user, Id(fileId), folderIdOrNull)
        return when (res) {
            is Success ->
                ResponseEntity.status(HttpStatus.OK).build<Unit>()
            is Failure ->
                when (res.value) {
                    MoveFileError.FileNotFound -> Problem.fileNotFound(fileId, instance)
                    MoveFileError.ErrorCreatingContext -> Problem.invalidCreateContext(instance)
                    MoveFileError.ErrorCreatingGlobalBucket -> Problem.invalidCreationGlobalBucket(instance)
                    MoveFileError.InvalidCredential -> Problem.invalidCredential(instance)
                    MoveFileError.FileNameAlreadyExists -> Problem.invalidFileName("Name associated $fileId", instance)
                    MoveFileError.FolderNotFound -> Problem.folderNotFound(input.folderId ?: 0, instance)
                    MoveFileError.MoveBlobError -> Problem.invalidCreateBlob(instance)
                    MoveFileError.MoveBlobNotFound -> Problem.invalidCreateBlob(instance)
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
                    DownloadFileError.InvalidKey -> Problem.invalidKey(instance)
                    DownloadFileError.ParentFolderNotFound -> Problem.parentFolderNotFound(0, instance)
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
                    DeleteFileError.ParentFolderNotFound -> Problem.parentFolderNotFound(0, instance)
                }
        }
    }

    companion object {
        private const val DEFAULT_LIMIT = 10
        private const val DEFAULT_PAGE = 0
        private const val DEFAULT_SORT = "created_asc"
    }
}
