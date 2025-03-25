package pt.isel.leic.multicloudguardian.http.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import pt.isel.leic.multicloudguardian.domain.user.AuthenticatedUser
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.http.Uris
import pt.isel.leic.multicloudguardian.http.media.Problem
import pt.isel.leic.multicloudguardian.http.model.storage.FileCreateInputModel
import pt.isel.leic.multicloudguardian.http.model.user.IdOutputModel
import pt.isel.leic.multicloudguardian.service.storage.FileCreationError
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
                        Uris.Files.byId(file.value.value).toASCIIString(),
                    ).body(IdOutputModel(file.value.value))

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
                }
        }
    }
}
