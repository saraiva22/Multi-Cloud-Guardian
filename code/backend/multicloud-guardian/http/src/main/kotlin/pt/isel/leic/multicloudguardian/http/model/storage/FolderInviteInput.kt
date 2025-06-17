package pt.isel.leic.multicloudguardian.http.model.storage

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import pt.isel.leic.multicloudguardian.domain.user.components.Username

data class FolderInviteInput(
    @field:NotBlank(message = "Username must not be blank")
    @field:Size(
        min = Username.MIN_LENGTH,
        max = Username.MAX_LENGTH,
        message = "Username must have between ${Username.MIN_LENGTH} and ${Username.MAX_LENGTH} characters",
    )
    val username: String,
)
