package pt.isel.leic.multicloudguardian.http.model.user


import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Password
import pt.isel.leic.multicloudguardian.domain.user.components.Username

data class UserCreateInputModel(
    @field:NotBlank(message = "Username must not be blank")
    @field:Size(
        min = Username.minLength,
        max = Username.maxLength,
        message = "Username must have between ${Username.minLength} and ${Username.maxLength} characters"
    )
    val username: Username,
    @field:NotBlank(message = "Email must not be blank")
    @field:jakarta.validation.constraints.Email(message = "Email must be valid")
    val email: Email,
    @field:NotBlank(message = "Password must not be blank")
    @field:Size(
        min = Password.minLength,
        max = Password.maxLength,
        message = "Password must have between 5 and 40 characters"
    )
    val password: Password

)
