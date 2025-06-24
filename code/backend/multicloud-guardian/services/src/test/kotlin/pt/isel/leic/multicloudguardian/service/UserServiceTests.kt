package pt.isel.leic.multicloudguardian.service

import pt.isel.leic.multicloudguardian.domain.preferences.CostType
import pt.isel.leic.multicloudguardian.domain.preferences.LocationType
import pt.isel.leic.multicloudguardian.domain.utils.Either
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.service.sse.SSEService
import pt.isel.leic.multicloudguardian.service.user.UserCreationError
import pt.isel.leic.multicloudguardian.service.utils.ServiceTests
import pt.isel.leic.multicloudguardian.service.utils.TestClock
import pt.isel.leic.multicloudguardian.service.utils.clearData
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class UserServiceTests : ServiceTests() {
    @Test
    fun `can create user, token, and retrieve by token`() {
        // given: a user service
        val testClock = TestClock()
        val sseService = SSEService()
        val userService = createUsersService(sseService, testClock)
        val userAgent = "Mobile"

        // when: creating a user
        val username = newTestUserName()
        val password = newTestPassword()
        val email = newTestEmail(username)
        val salt = newTestSalt()
        val iteration = newTestIteration()
        val performance = CostType.MEDIUM
        val location = LocationType.EUROPE

        val createUserResult = userService.createUser(username, email, password, salt, iteration, performance, location)

        // then: the creation is successful
        when (createUserResult) {
            is Success -> assertTrue(createUserResult.value.value > 0)
            is Failure -> fail("Unexpected $createUserResult")
        }

        // when: creating a token
        val createTokenResult = userService.createToken(username, password, userAgent)

        // then: the creation is successful
        val token =
            when (createTokenResult) {
                is Success -> createTokenResult.value.tokenValue
                is Failure -> fail(createTokenResult.toString())
            }

        // and: the token bytes have the expected length
        val tokenBytes = Base64.getUrlDecoder().decode(token)
        assertEquals(256 / 8, tokenBytes.size)

        // when: retrieving the user by token
        val user = userService.getUserByToken(token)

        // then: a user is found
        assertNotNull(user)

        // and: has the expected name
        assertEquals(username, user.username.value)

        // clean up
        clearData(jdbi, "dbo.Tokens", "user_id", user.id.value)
        clearData(jdbi, "dbo.Users", "id", user.id.value)
    }

    @Test
    fun `create user with invalid password`() {
        // given: a user service
        val testClock = TestClock()
        val sseService = SSEService()
        val userService = createUsersService(sseService, testClock)
        val userAgent = "Mobile"

        // when: creating a user with an invalid password
        val username = newTestUserName()
        val passwordValidationInfo = "invalidpassword"
        val email = newTestEmail(username)
        val salt = newTestSalt()
        val iteration = newTestIteration()
        val performance = CostType.MEDIUM
        val location = LocationType.EUROPE

        // then: the creation fails
        val createUserResult =
            userService.createUser(username, email, passwordValidationInfo, salt, iteration, performance, location)

        when (createUserResult) {
            is Success -> fail("Expected failure but got success: $createUserResult")
            is Failure -> assertTrue(createUserResult.value is UserCreationError.InsecurePassword)
        }
    }

    @Test
    fun `create user token`() {
        // given: a user service
        val testClock = TestClock()
        val sseService = SSEService()
        val userService = createUsersService(sseService, testClock)
        val userAgent = "Mobile"

        // when: creating a user
        val username = newTestUserName()
        val passwordValidationInfo = newTestPassword()
        val email = newTestEmail(username)
        val salt = newTestSalt()
        val iteration = newTestIteration()
        val performance = CostType.MEDIUM
        val location = LocationType.EUROPE

        val createUserResult =
            createUserInService(username, passwordValidationInfo, email, salt, iteration, performance, location)

        // when: creating a token
        val createTokenResult =
            userService.createToken(createUserResult.username.value, passwordValidationInfo, userAgent)

        // then: the creation is successful
        val token =
            when (createTokenResult) {
                is Success -> createTokenResult.value.tokenValue
                is Failure -> fail(createTokenResult.toString())
            }

        // when: retrieving the user by token
        val user = userService.getUserByToken(token)

        // then: the user is found
        assertNotNull(user)
        assertEquals(createUserResult.username, user.username)
        assertEquals(createUserResult.email, user.email)
        assertEquals(createUserResult.passwordValidation, user.passwordValidation)

        // finally: clear the data
        clearData(jdbi, "dbo.Tokens", "user_id", user.id.value)
        clearData(jdbi, "dbo.Users", "id", user.id.value)
    }

    @Test
    fun `can use token during rolling period but not after absolute TTL`() {
        // given: a user service
        val testClock = TestClock()
        val tokenTtl = 90.minutes
        val tokenRollingTtl = 30.minutes
        val sseService = SSEService()
        val userService = createUsersService(sseService, testClock, tokenTtl, tokenRollingTtl)
        val userAgent = "Mobile"

        // when: creating a user
        val username = newTestUserName()
        val password = newTestPassword()
        val email = newTestEmail(username)
        val salt = newTestSalt()
        val iteration = newTestIteration()
        val performance = CostType.MEDIUM
        val location = LocationType.EUROPE

        val createUserResult = userService.createUser(username, email, password, salt, iteration, performance, location)

        // then: the creation is successful
        when (createUserResult) {
            is Success -> assertTrue(createUserResult.value.value > 0)
            is Failure -> fail("Unexpected $createUserResult")
        }

        // when: creating a token
        val createTokenResult = userService.createToken(username, password, userAgent)

        // then: the creation is successful
        val token =
            when (createTokenResult) {
                is Success -> createTokenResult.value.tokenValue
                is Failure -> fail(createTokenResult.toString())
            }

        // when: retrieving the user after (rolling TTL - 1s) intervals
        val startInstant = testClock.now()
        while (true) {
            testClock.advance(tokenRollingTtl.minus(1.seconds))
            userService.getUserByToken(token) ?: break
        }

        // then: user is not found only after the absolute TTL has elapsed
        assertTrue((testClock.now() - startInstant) > tokenTtl)

        // clean up
        clearData(jdbi, "dbo.Tokens", "user_id", createUserResult.value.value)
        clearData(jdbi, "dbo.Users", "id", createUserResult.value.value)
    }

    @Test
    fun `revoke user token`() {
        // given: a user service
        val tests = TestClock()
        val sseService = SSEService()
        val userService = createUsersService(sseService, tests)
        val userAgent = "Mobile"

        // then: creating a user
        val username = newTestUserName()
        val password = newTestPassword()
        val email = newTestEmail(username)
        val salt = newTestSalt()
        val iteration = newTestIteration()
        val performance = CostType.MEDIUM
        val location = LocationType.EUROPE
        val createUserResult = createUserInService(username, password, email, salt, iteration, performance, location)

        // when: creating a token
        val createTokenResult = userService.createToken(username, password, userAgent)

        // then: the creation is successful
        val token =
            when (createTokenResult) {
                is Either.Left -> fail(createTokenResult.toString())
                is Either.Right -> createTokenResult.value.tokenValue
            }

        // when: revoking the token
        userService.revokeToken(createUserResult.id, token)

        // then: the token is no longer valid
        val user = userService.getUserByToken(token)
        assertNull(user, "The token should be revoked and not valid anymore")

        // clean up
        clearData(jdbi, "dbo.Tokens", "user_id", createUserResult.id.value)
        clearData(jdbi, "dbo.Users", "id", createUserResult.id.value)
    }

    @Test
    fun `can limit the number of tokens`() {
        // given: a user service
        val testClock = TestClock()
        val maxTokensPerUser = 5
        val sseService = SSEService()
        val userService = createUsersService(sseService, testClock, maxTokensPerUser = maxTokensPerUser)
        val userAgent = "Mobile"

        // when: creating a user
        val username = newTestUserName()
        val password = newTestPassword()
        val email = newTestEmail(username)
        val salt = newTestSalt()
        val iteration = newTestIteration()
        val performance = CostType.MEDIUM
        val location = LocationType.EUROPE
        val createUserResult = userService.createUser(username, email, password, salt, iteration, performance, location)

        // then: the creation is successful
        when (createUserResult) {
            is Success -> assertTrue(createUserResult.value.value > 0)
            is Failure -> fail("Unexpected $createUserResult")
        }

        // when: creating MAX tokens
        val tokens =
            (0 until maxTokensPerUser)
                .map {
                    val createTokenResult = userService.createToken(username, password, userAgent)
                    testClock.advance(1.minutes)

                    // then: the creation is successful
                    val token =
                        when (createTokenResult) {
                            is Success -> createTokenResult.value
                            is Failure -> fail(createTokenResult.toString())
                        }
                    token
                }.toTypedArray()
                .reversedArray()

        // and: using the tokens at different times
        (tokens.indices).forEach {
            assertNotNull(userService.getUserByToken(tokens[it].tokenValue), "token $it must be valid")
            testClock.advance(1.seconds)
        }

        // and: creating a new token
        val createTokenResult = userService.createToken(username, password, userAgent)
        testClock.advance(1.seconds)
        val newToken =
            when (createTokenResult) {
                is Success -> createTokenResult.value
                is Failure -> fail(createTokenResult.toString())
            }

        // then: newToken is valid
        assertNotNull(userService.getUserByToken(newToken.tokenValue))

        // and: the first token (the least recently used) is not valid
        assertNull(userService.getUserByToken(tokens[0].tokenValue))

        // and: the remaining tokens are still valid
        (1 until tokens.size).forEach {
            assertNotNull(userService.getUserByToken(tokens[it].tokenValue))
        }

        // clean up
        clearData(jdbi, "dbo.Tokens", "user_id", createUserResult.value.value)
        clearData(jdbi, "dbo.Users", "id", createUserResult.value.value)
    }

    @Test
    fun `can limit the number of tokens even if multiple tokens are used at the same time`() {
        // given: a user service
        val testClock = TestClock()
        val maxTokensPerUser = 5
        val sseService = SSEService()
        val userService = createUsersService(sseService, testClock, maxTokensPerUser = maxTokensPerUser)
        val userAgent = "Mobile"

        // when: creating a user
        val username = newTestUserName()
        val password = newTestPassword()
        val email = newTestEmail(username)
        val salt = newTestSalt()
        val iteration = newTestIteration()
        val performance = CostType.MEDIUM
        val location = LocationType.EUROPE
        val createUserResult = userService.createUser(username, email, password, salt, iteration, performance, location)

        // then: the creation is successful
        when (createUserResult) {
            is Success -> assertTrue(createUserResult.value.value > 0)
            is Failure -> fail("Unexpected $createUserResult")
        }

        // when: creating MAX tokens
        val tokens =
            (0 until maxTokensPerUser)
                .map {
                    val createTokenResult = userService.createToken(username, password, userAgent)
                    testClock.advance(1.minutes)

                    // then: the creation is successful
                    val token =
                        when (createTokenResult) {
                            is Success -> createTokenResult.value
                            is Failure -> fail(createTokenResult.toString())
                        }
                    token
                }.toTypedArray()
                .reversedArray()

        // and: using the tokens at the same time
        testClock.advance(1.minutes)
        (tokens.indices).forEach {
            assertNotNull(userService.getUserByToken(tokens[it].tokenValue), "token $it must be valid")
        }

        // and: creating a new token
        val createTokenResult = userService.createToken(username, password, userAgent)
        testClock.advance(1.minutes)
        val newToken =
            when (createTokenResult) {
                is Success -> createTokenResult.value
                is Failure -> fail(createTokenResult.toString())
            }

        // then: newToken is valid
        assertNotNull(userService.getUserByToken(newToken.tokenValue))

        // and: exactly one of the previous tokens is now not valid
        assertEquals(
            maxTokensPerUser - 1,
            tokens.count {
                userService.getUserByToken(it.tokenValue) != null
            },
        )

        // clean up
        clearData(jdbi, "dbo.Tokens", "user_id", createUserResult.value.value)
        clearData(jdbi, "dbo.Users", "id", createUserResult.value.value)
    }

    @Test
    fun `can logout`() {
        // given: a user service
        val testClock = TestClock()
        val maxTokensPerUser = 5
        val sseService = SSEService()
        val userService = createUsersService(sseService, testClock, maxTokensPerUser = maxTokensPerUser)
        val userAgent = "Mobile"

        // when: creating a user
        val username = newTestUserName()
        val password = newTestPassword()
        val email = newTestEmail(username)
        val salt = newTestSalt()
        val iteration = newTestIteration()
        val performance = CostType.MEDIUM
        val location = LocationType.EUROPE
        val createUserResult = userService.createUser(username, email, password, salt, iteration, performance, location)

        // then: the creation is successful
        when (createUserResult) {
            is Success -> assertTrue(createUserResult.value.value > 0)
            is Failure -> fail("Unexpected $createUserResult")
        }

        // when: creating a token
        val tokenCreationResult = userService.createToken(username, password, userAgent)

        // then: token creation is successful
        val token =
            when (tokenCreationResult) {
                is Success -> tokenCreationResult.value
                is Failure -> fail("Token creation should be successful: '${tokenCreationResult.value}'")
            }

        // when: using the token
        var maybeUser = userService.getUserByToken(token.tokenValue)

        // then: token usage is successful
        assertNotNull(maybeUser)

        // when: revoking and using the token
        userService.revokeToken(maybeUser.id, token.tokenValue)

        maybeUser = userService.getUserByToken(token.tokenValue)

        // then: token usage is successful
        assertNull(maybeUser)

        // clean up
        clearData(jdbi, "dbo.Tokens", "user_id", createUserResult.value.value)
        clearData(jdbi, "dbo.Users", "id", createUserResult.value.value)
    }
}
