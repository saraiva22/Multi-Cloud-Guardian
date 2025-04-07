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
import pt.isel.leic.multicloudguardian.http.model.storage.DownloadFileInputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FileCreateInputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FileInfoOutputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FilesListOutputModel
import pt.isel.leic.multicloudguardian.http.model.storage.UploadFileOutputModel
import pt.isel.leic.multicloudguardian.service.storage.DeleteFileError
import pt.isel.leic.multicloudguardian.service.storage.DownloadFileError
import pt.isel.leic.multicloudguardian.service.storage.FileCreationError
import pt.isel.leic.multicloudguardian.service.storage.GetFileByIdError
import pt.isel.leic.multicloudguardian.service.storage.StorageService

@RestController
class StorageController(
    private val storageService: StorageService,
) {
    @PostMapping(Uris.Files.UPLOAD)
    fun uploadFile(
        @RequestParam("file") fileMultiPart: MultipartFile,
        @RequestParam("encryption") encryption: Boolean,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val fileCreateInputModel = FileCreateInputModel(fileMultiPart, encryption)
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
                        Uris.Files.byId(file.value.first.value).toASCIIString(),
                    ).body(UploadFileOutputModel(file.value.first.value, file.value.second))

            is Failure ->
                when (file.value) {
                    FileCreationError.FileStorageError ->
                        Problem.invalidCreationStorage(
                            instance,
                        )

                    FileCreationError.ErrorCreatingGlobalBucket ->
                        Problem.invalidCreationGlobalBucket(
                            instance,
                        )

                    FileCreationError.ErrorCreatingContext ->
                        Problem.invalidCreateContext(instance)

                    FileCreationError.FileNameAlreadyExists ->
                        Problem.invalidFileName(fileDomain.blobName, instance)

                    FileCreationError.FileStorageError ->
                        Problem.invalidCreationStorage(instance)

                    FileCreationError.InvalidCredential ->
                        Problem.invalidCredential(instance)

                    FileCreationError.ErrorEncryptingFile ->
                        Problem.invalidEncryptFile(instance)
                }
        }
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

    @PostMapping(Uris.Files.DOWNLOAD_FILE)
    fun downloadFile(
        @RequestBody input: DownloadFileInputModel,
        @Validated @PathVariable fileId: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Files.downloadFile(fileId)
        val user = authenticatedUser.user
        return when (
            val res =
                storageService.downloadFile(Id(fileId), input.encryption, input.encryptedKey, input.pathSaveFile, user)
        ) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .header("Location", Uris.Files.downloadFile(fileId).toASCIIString())
                    .build<Unit>()

            is Failure ->
                when (res.value) {
                    DownloadFileError.ErrorDownloadingFile -> Problem.invalidDownloadFile(instance)
                    DownloadFileError.FileNotFound -> Problem.fileNotFound(fileId, instance)
                    DownloadFileError.ErrorCreatingContext -> Problem.invalidCreateContext(instance)
                    DownloadFileError.ErrorCreatingGlobalBucket -> Problem.invalidCreationGlobalBucket(instance)
                    DownloadFileError.InvalidCredential -> Problem.invalidCredential(instance)
                    DownloadFileError.ErrorDecryptingFile -> Problem.invalidDecryptFile(instance)
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

    @GetMapping(Uris.Files.GET_FILES)
    fun getFiles(authenticatedUser: AuthenticatedUser): ResponseEntity<*> {
        val res = storageService.getFiles(authenticatedUser.user)
        return ResponseEntity.status(HttpStatus.OK).body(
            FilesListOutputModel(
                res.map {
                    (FileInfoOutputModel.fromDomain(it))
                },
            ),
        )
    }
}
