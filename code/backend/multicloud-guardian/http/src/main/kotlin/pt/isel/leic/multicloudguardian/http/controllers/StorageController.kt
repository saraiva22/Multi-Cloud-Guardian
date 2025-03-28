package pt.isel.leic.multicloudguardian.http.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import pt.isel.leic.multicloudguardian.domain.user.AuthenticatedUser
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Id
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.http.Uris
import pt.isel.leic.multicloudguardian.http.media.Problem
import pt.isel.leic.multicloudguardian.http.model.storage.FileCreateInputModel
import pt.isel.leic.multicloudguardian.http.model.storage.FileOutputModel
import pt.isel.leic.multicloudguardian.http.model.storage.UploadFileOutputModel
import pt.isel.leic.multicloudguardian.service.storage.FileCreationError
import pt.isel.leic.multicloudguardian.service.storage.GetFileByIdError
import pt.isel.leic.multicloudguardian.service.storage.StorageService

@RestController
class StorageController(
    private val storageService: StorageService,
) {
    @PostMapping(Uris.Files.CREATE)
    fun createFile(
        @RequestParam("file") fileMultiPart: MultipartFile,
        @RequestParam("encryption") encryption: Boolean,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val fileCreateInputModel = FileCreateInputModel(fileMultiPart, encryption)
        val instance = Uris.Files.register()
        val fileDomain = fileCreateInputModel.toDomain()
        val file =
            storageService.createFile(
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
                        Problem.invalidFileName(fileDomain.fileName, instance)

                    FileCreationError.FileStorageError ->
                        Problem.invalidCreationStorage(instance)

                    FileCreationError.InvalidCredential ->
                        Problem.invalidCredential(instance)
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
                        FileOutputModel.fromDomain(res.value),
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
}
