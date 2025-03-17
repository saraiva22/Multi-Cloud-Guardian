package pt.isel.leic.multicloudguardian.http.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.http.Uris
import pt.isel.leic.multicloudguardian.http.model.user.IdOutputModel
import pt.isel.leic.multicloudguardian.http.model.user.UserCreateInputModel
import pt.isel.leic.multicloudguardian.service.user.UserCreationError
import pt.isel.leic.multicloudguardian.service.user.UsersService

@RestController
class UsersController(
    private val userService: UsersService
) {

    companion object {
        const val HEADER_SET_COOKIE_NAME = "Set-Cookie"
        const val COOKIE_NAME_LOGIN = "login"
        const val COOKIE_NAME_TOKEN = "token"
    }

    @PostMapping(Uris.Users.CREATE)
    fun createUser(
        @Validated @RequestBody input: UserCreateInputModel
    ): ResponseEntity<*> {
        val instance = Uris.Users.register()
        val user = userService.createUser(input.username, input.email, input.password)
        return when (user) {
            is Success -> ResponseEntity.status(HttpStatus.CREATED)
                .header(
                    "Location",
                    Uris.Users.byId(user.value.value).toASCIIString()
                ).body(IdOutputModel(user.value.value))

            is Failure -> when (user.value) {
                UserCreationError.InsecurePassword -> Problem.insecurePassword(instance)
                UserCreationError.UserNameAlreadyExists -> Problem.usernameAlreadyExists(input.username, instance)
                UserCreationError.EmailAlreadyExists -> Problem.emailAlreadyExists(input.email, instance)
            }
        }
    }


}