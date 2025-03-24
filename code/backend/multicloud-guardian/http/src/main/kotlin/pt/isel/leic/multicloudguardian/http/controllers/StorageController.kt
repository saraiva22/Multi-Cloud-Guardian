package pt.isel.leic.multicloudguardian.http.controllers

import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pt.isel.leic.multicloudguardian.domain.user.AuthenticatedUser
import pt.isel.leic.multicloudguardian.http.Uris
import pt.isel.leic.multicloudguardian.http.model.storage.FileCreateInputModel
import pt.isel.leic.multicloudguardian.service.storage.StorageService

@RestController
class StorageController(
    private val storageService: StorageService,
) {
    @PostMapping(Uris.Files.CREATE)
    fun createFile(
        @Validated @RequestBody input: FileCreateInputModel,
        authenticatedUser: AuthenticatedUser,
    ) {
        val instance = Uris.Files.register()
        val fileDomain = input.toDomain()
        val file =
            storageService.createfile(
                fileDomain,
                input.encryption,
                authenticatedUser.user,
            )
        return when (file) {
        }
    }
}
