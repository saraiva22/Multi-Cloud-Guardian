package pt.isel.leic.multicloudguardian.http.pipeline.interceptors

import org.slf4j.LoggerFactory
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import pt.isel.leic.multicloudguardian.domain.user.AuthenticatedUser
import pt.isel.leic.multicloudguardian.http.media.Problem
import pt.isel.leic.multicloudguardian.http.pipeline.processors.RequestTokenProcessor
import pt.isel.leic.multicloudguardian.http.pipeline.resolvers.AuthenticatedUserArgumentResolver


@Component
class AuthenticationInterceptor(
    private val authorizationHeaderProcessor: RequestTokenProcessor,
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (handler is HandlerMethod && handler.methodParameters.any {
                it.parameterType == AuthenticatedUser::class.java
            }) {
            logger.info("Intercepting request to enforce authentication")
            // process token in authentication schema
            val bearerToken = request.getHeader(NAME_AUTHORIZATION_HEADER)
            val authUserToken = authorizationHeaderProcessor.processAuthorizationHeaderValue(bearerToken)
            logger.info("Bearer Token: $bearerToken")
            // process token in cookie
            val authCookie = request.cookies?.find { it.name == NAME_COOKIE }?.value
            val authUserCookie = authorizationHeaderProcessor.processCookieValue(authCookie)
            logger.info("Cookie: $authCookie")
            return if (authUserToken == null && authUserCookie == null) {
                // client is not authenticated
                response.contentType = Problem.MEDIA_TYPE
                val objectMapper = ObjectMapper()
                val problem = Problem.unauthorizedRequest
                val json = objectMapper.writeValueAsString(problem)
                response.writer.write(json)
                response.status = 401
                response.contentType = Problem.MEDIA_TYPE
                response.addHeader(NAME_WWW_AUTHENTICATE_HEADER, RequestTokenProcessor.SCHEME)
                logger.info("User not authenticated")
                false
            } else {
                (authUserToken ?: authUserCookie)?.let { AuthenticatedUserArgumentResolver.addUserTo(it, request) }
                logger.info("User authenticated")
                true
            }

        }

        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthenticationInterceptor::class.java)
        const val NAME_AUTHORIZATION_HEADER = "Authorization"
        private const val NAME_WWW_AUTHENTICATE_HEADER = "WWW-Authenticate"
        const val NAME_COOKIE = "token"
    }
}