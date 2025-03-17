package pt.isel.leic.multicloudguardian.http.pipeline.processors


import org.springframework.stereotype.Component
import pt.isel.leic.multicloudguardian.domain.user.AuthenticatedUser
import pt.isel.leic.multicloudguardian.service.user.UsersService


@Component
class RequestTokenProcessor(
    val usersService: UsersService,
) {
    fun processAuthorizationHeaderValue(authorizationValue: String?): AuthenticatedUser? {
        if (authorizationValue == null) {
            return null
        }

        // Verify if Bearer <token> is present
        val parts = authorizationValue.trim().split(" ")
        if (parts.size != 2) {
            return null
        }
        if (parts[0].lowercase() != SCHEME) return null

        return usersService.getUserByToken(parts[1]?.let { AuthenticatedUser(it, parts[1]) })
    }

    fun processCookieValue(cookieValue: String?): AuthenticatedUser? {
        if (cookieValue == null) {
            return null
        }
        return usersService.getUserByToken(cookieValue)?.let {
            AuthenticatedUser(it, cookieValue)
        }
    }

    companion object {
        const val SCHEME = "bearer"
    }
}