package pt.isel.leic.multicloudguardian.http.controllers

import org.springframework.web.bind.annotation.RestController
import pt.isel.leic.multicloudguardian.user.UsersService

@RestController
class UsersController(private val userService: UsersService) {



    companion object {
        const val HEADER_SET_COOKIE_NAME = "Set-Cookie"
        const val COOKIE_NAME_LOGIN = "login"
        const val COOKIE_NAME_TOKEN = "token"
    }
}