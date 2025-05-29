package pt.isel.leic.multicloudguardian.http.model.storage

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class GenerateTempUrlInputModel(
    @field:Min(1, message = "expiresIn must be at least 1 minute")
    @field:Max(45, message = "expiresIn must not exceed 45 minutes")
    val expiresIn: Long,
)
