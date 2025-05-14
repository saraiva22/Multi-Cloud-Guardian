package pt.isel.leic.multicloudguardian.http.model.user

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import pt.isel.leic.multicloudguardian.domain.preferences.LocationType
import pt.isel.leic.multicloudguardian.domain.preferences.PerformanceType
import pt.isel.leic.multicloudguardian.domain.user.components.Password
import pt.isel.leic.multicloudguardian.domain.user.components.Username

data class UserCreateInputModel(
    @field:NotBlank(message = "Username must not be blank")
    @field:Size(
        min = Username.MIN_LENGTH,
        max = Username.MAX_LENGTH,
        message = "Username must have between ${Username.MIN_LENGTH} and ${Username.MAX_LENGTH} characters",
    )
    val username: String,
    @field:NotBlank(message = "Email must not be blank")
    @field:jakarta.validation.constraints.Email(message = "Email must be valid")
    val email: String,
    @field:NotBlank(message = "Password must not be blank")
    @field:Size(
        min = Password.MIN_LENGTH,
        max = Password.MAX_LENGTH,
        message = "Password must have between 5 and 40 characters",
    )
    val password: String,
    @field:NotBlank(message = "Salt must not be blank")
    val salt: String,
    @field:NotNull(message = "Iterations must not be null")
    val iterations: Int,
    val performanceType: PerformanceType,
    val locationType: LocationType,
)
