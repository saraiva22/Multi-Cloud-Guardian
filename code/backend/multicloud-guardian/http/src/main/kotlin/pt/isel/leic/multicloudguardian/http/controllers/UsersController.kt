package pt.isel.leic.multicloudguardian.http.controllers

import kotlinx.datetime.Clock
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pt.isel.leic.multicloudguardian.domain.user.AuthenticatedUser
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.http.Uris
import pt.isel.leic.multicloudguardian.http.media.Problem
import pt.isel.leic.multicloudguardian.http.model.user.UserCreateInputModel
import pt.isel.leic.multicloudguardian.http.model.user.UserCreateTokenInputModel
import pt.isel.leic.multicloudguardian.http.model.user.UserCredentialsOutputModel
import pt.isel.leic.multicloudguardian.http.model.user.UserHomeOutputModel
import pt.isel.leic.multicloudguardian.http.model.user.UserListOutputModel
import pt.isel.leic.multicloudguardian.http.model.user.UserStorageInfoOutputModel
import pt.isel.leic.multicloudguardian.http.model.user.UserTokenCreateOutputModel
import pt.isel.leic.multicloudguardian.http.model.utils.IdOutputModel
import pt.isel.leic.multicloudguardian.service.user.TokenCreationError
import pt.isel.leic.multicloudguardian.service.user.UserCreationError
import pt.isel.leic.multicloudguardian.service.user.UsersService

@RestController
class UsersController(
    private val userService: UsersService,
) {
    companion object {
        const val HEADER_SET_COOKIE_NAME = "Set-Cookie"
        const val COOKIE_NAME_LOGIN = "login"
        const val COOKIE_NAME_TOKEN = "token"
    }

    @PostMapping(Uris.Users.CREATE)
    fun createUser(
        @Validated @RequestBody input: UserCreateInputModel,
    ): ResponseEntity<*> {
        val instance = Uris.Users.register()
        val user =
            userService.createUser(
                input.username,
                input.email,
                input.password,
                input.salt,
                input.iterations,
                input.performanceType,
                input.locationType,
            )
        return when (user) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header(
                        "Location",
                        Uris.Users.byId(user.value.value).toASCIIString(),
                    ).body(IdOutputModel(user.value.value))

            is Failure ->
                when (user.value) {
                    UserCreationError.InsecurePassword -> Problem.insecurePassword(instance)
                    UserCreationError.UserNameAlreadyExists ->
                        Problem.usernameAlreadyExists(
                            Username(input.username),
                            instance,
                        )

                    UserCreationError.EmailAlreadyExists -> Problem.emailAlreadyExists(Email(input.email), instance)
                }
        }
    }

    /**
     * Token creation
     * @param input UserCreateTokenInputModel
     * @param response HttpServletResponse
     * @return ResponseEntity<*>
     *
     * HttpOnly: The HttpOnly attribute is used to help prevent attacks such as cross-site scripting, since it does not allow the cookie to be accessed via JavaScript.
     * SameSite: The SameSite attribute is used to prevent the browser from sending this cookie along with cross-site requests. The main goal is mitigate the risk of cross-origin information leakage.
     * Path: The Path attribute indicates a URL path that must exist in the requested URL in order to send the Cookie header.
     * Max-age: The Max-age attribute is used to set the time in seconds for a cookie to expire.
     */

    @PostMapping(Uris.Users.TOKEN)
    fun createToken(
        @Validated @RequestBody input: UserCreateTokenInputModel,
        @RequestHeader("User-Agent") userAgent: String,
    ): ResponseEntity<*> {
        val instance = Uris.Users.login()
        val token =
            userService.createToken(input.username, input.password, userAgent)
        return when (token) {
            is Success -> {
                // Cookie max age is the difference between the token expiration and the current time
                val cookieMaxAge = token.value.tokenExpiration.epochSeconds - Clock.System.now().epochSeconds
                ResponseEntity
                    .status(200)
                    .header(
                        HEADER_SET_COOKIE_NAME,
                        "$COOKIE_NAME_TOKEN=${token.value.tokenValue};Max-age=$cookieMaxAge; HttpOnly; SameSite = Strict; Path=/",
                    ).header(
                        HEADER_SET_COOKIE_NAME,
                        "$COOKIE_NAME_LOGIN=${input.username};Max-age=$cookieMaxAge; SameSite = Strict; Path=/",
                    ).body(UserTokenCreateOutputModel(token.value.tokenValue))
            }

            is Failure ->
                when (token.value) {
                    TokenCreationError.UsernameNotFound -> Problem.usernameNotFound(input.username, instance)
                    TokenCreationError.InvalidToken -> Problem.invalidToken(instance)
                    TokenCreationError.PasswordDoesNotMatch -> Problem.passwordDoesNotMatch(instance)
                    TokenCreationError.InsecurePassword -> Problem.insecurePassword(instance)
                }
        }
    }

    @GetMapping(Uris.Users.SEARCH_USERS)
    fun searchUsers(
        @RequestParam username: String,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val user = userService.searchUsers(username)
        return ResponseEntity
            .status(200)
            .body(UserListOutputModel(user.map { UserHomeOutputModel(it.id.value, it.username.value) }))
    }

    @PostMapping(Uris.Users.LOGOUT)
    fun logout(authenticatedUser: AuthenticatedUser): ResponseEntity<*> {
        val instance = Uris.Users.logout()
        return when (userService.revokeToken(authenticatedUser.token)) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .header(
                        HEADER_SET_COOKIE_NAME,
                        "$COOKIE_NAME_TOKEN=${authenticatedUser.token};Max-age=0; HttpOnly; SameSite = Strict; Path=/",
                    ).header(
                        HEADER_SET_COOKIE_NAME,
                        "$COOKIE_NAME_LOGIN=${authenticatedUser.user.username};Max-age=0; SameSite = Strict; Path=/",
                    ).body(UserTokenCreateOutputModel("Token ${authenticatedUser.token} removed successful"))

            is Failure -> Problem.tokenNotRevoked(instance, authenticatedUser.token)
        }
    }

    @GetMapping(Uris.Users.GET_BY_ID)
    fun getById(
        @PathVariable id: Int,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Users.byId(id)
        val user = userService.getUserById(id)
        return when (user) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(
                        UserStorageInfoOutputModel(
                            user.value.id.value,
                            user.value.username.value,
                            user.value.email.value,
                            user.value.locationType.name,
                            user.value.performanceType.name,
                        ),
                    )

            is Failure -> Problem.userNotFoundById(id, instance)
        }
    }

    @GetMapping(Uris.Users.GET_BY_USERNAME)
    fun getByUsername(
        @RequestParam username: String,
        authenticatedUser: AuthenticatedUser,
    ): ResponseEntity<*> {
        val instance = Uris.Users.byUsername(username)
        val user = userService.getUserByUsername(Username(username))
        return when (user) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(
                        UserStorageInfoOutputModel(
                            user.value.id.value,
                            user.value.username.value,
                            user.value.email.value,
                            user.value.locationType.name,
                            user.value.performanceType.name,
                        ),
                    )

            is Failure -> Problem.userNotFoundByUsername(username, instance)
        }
    }

    @GetMapping(Uris.Users.CREDENTIALS)
    fun getCredentials(authenticatedUser: AuthenticatedUser): ResponseEntity<*> {
        val instance = Uris.Users.credentials()
        val result = userService.getUserCredentialsById(authenticatedUser.user.id.value)
        return when (result) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .body(
                        UserCredentialsOutputModel.fromDomain(result.value),
                    )

            is Failure -> Problem.userNotFoundById(authenticatedUser.user.id.value, instance)
        }
    }

    @GetMapping(Uris.Users.HOME)
    fun getUserHome(authenticatedUser: AuthenticatedUser): UserHomeOutputModel =
        UserHomeOutputModel(authenticatedUser.user.id.value, authenticatedUser.user.username.value)
}
