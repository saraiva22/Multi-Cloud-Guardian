package pt.isel.leic.multicloudguardian.http.model.storage

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import pt.isel.leic.multicloudguardian.domain.folder.Folder
import pt.isel.leic.multicloudguardian.domain.folder.FolderType

data class FolderCreateInputModel(
    @field:NotBlank(message = "Folder must not be blank")
    @field:Size(
        min = Folder.MIN_LENGTH,
        max = Folder.MAX_LENGTH,
        message = "Folder must have between ${Folder.MIN_LENGTH} and ${Folder.MAX_LENGTH} characters",
    )
    val folderName: String,
    val folderType: FolderType,
)
