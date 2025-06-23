package pt.isel.leic.multicloudguardian.repository.jdbi

import kotlinx.datetime.Instant
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.postgresql.ds.PGSimpleDataSource
import pt.isel.leic.multicloudguardian.ApplicationTests.Companion.newTestEmail
import pt.isel.leic.multicloudguardian.ApplicationTests.Companion.newTestIteration
import pt.isel.leic.multicloudguardian.ApplicationTests.Companion.newTestSalt
import pt.isel.leic.multicloudguardian.Environment
import pt.isel.leic.multicloudguardian.TestClock
import pt.isel.leic.multicloudguardian.domain.preferences.CostType
import pt.isel.leic.multicloudguardian.domain.preferences.LocationType
import pt.isel.leic.multicloudguardian.domain.provider.ProviderType
import pt.isel.leic.multicloudguardian.domain.token.Token
import pt.isel.leic.multicloudguardian.domain.token.TokenValidationInfo
import pt.isel.leic.multicloudguardian.domain.user.PasswordValidationInfo
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class JdbiUserRepositoryTests {
    @Test
    fun `can create and retrieve user`() {
        runWithHandle { handle ->
            // given: a UserRepository
            val repo = JdbiUsersRepository(handle)

            // when: storing a user
            val username = Username(newTestUserName())
            val email = Email(newTestEmail(username.value))
            val salt = newTestSalt()
            val iteration = newTestIteration()
            val passwordValidationInfo = PasswordValidationInfo(newTokenValidationData())
            repo.storeUser(username, email, salt, iteration, passwordValidationInfo)

            // and: retrieving a user
            val user: User? = repo.getUserByUsername(username)

            // then:
            assertNotNull(user)
            assertEquals(username, user.username)
            assertEquals(passwordValidationInfo, user.passwordValidation)
            assertTrue(user.id.value >= 0)

            // when: asking if the user exists
            val isUserIsStored = repo.isUserStoredByUsername(username)

            // then: response is true
            assertTrue(isUserIsStored)

            // when: asking if a different user exists
            val anotherUserIsStored = repo.isUserStoredByUsername(Username("another-${username.value}"))

            // then: response is false
            assertFalse(anotherUserIsStored)

            // finally: clear data
            clearData(jdbi, "dbo.Tokens", "user_id", user.id.value)
            clearData(jdbi, "dbo.Users", "id", user.id.value)
        }
    }

    @Test
    fun `can create user with provider and credentials and validate them`() {
        runWithHandle { handle ->
            // given: a user repository and user data
            val repo = JdbiUsersRepository(handle)
            val username = Username(newTestUserName())
            val email = Email(newTestEmail(username.value))
            val salt = newTestSalt()
            val iteration = newTestIteration()
            val passwordValidationInfo = PasswordValidationInfo(newTokenValidationData())
            val costType = CostType.HIGH
            val locationType = LocationType.EUROPE
            val providerType = ProviderType.AZURE

            // when: storing the user and preferences
            repo.storeUser(username, email, salt, iteration, passwordValidationInfo)
            val user: User? = repo.getUserByUsername(username)
            assertNotNull(user)

            repo.storagePreferences(
                user.id,
                costType,
                locationType,
                providerType,
            )

            // then: provider and credentials are correctly stored and retrieved
            val provider = repo.getProvider(user.id)
            val credentials = repo.getUserCredentialsById(user.id)

            // Validate provider type and credentials
            assertEquals(providerType, provider)
            assertNotNull(credentials)
            assertEquals(salt, credentials.salt)
            assertEquals(username, credentials.user.username)
            assertEquals(email, credentials.user.email)
            assertTrue(providerType == provider)

            // finally: clear data
            clearData(jdbi, "dbo.Tokens", "user_id", user.id.value)
            clearData(jdbi, "dbo.Users", "id", user.id.value)
        }
    }

    @Test
    fun `create and retrieve token with associated user`() =
        runWithHandle { handle ->
            // given: a UsersRepository
            val repo = JdbiUsersRepository(handle)

            // and: a test clock
            val clock = TestClock()

            // when: a user is created
            val username = Username(newTestUserName())
            val email = Email(newTestEmail(username.value))
            val salt = newTestSalt()
            val iteration = newTestIteration()
            val passwordValidationInfo = PasswordValidationInfo(newTokenValidationData())

            // when: storing the user and preferences
            repo.storeUser(username, email, salt, iteration, passwordValidationInfo)

            // then: the user is created
            val user = repo.getUserByUsername(username)
            assertNotNull(user)

            // and: test TokenValidationInfo
            val testTokenValidationInfo = TokenValidationInfo(newTokenValidationData())

            // when: creating a token
            val tokenCreationInstant = clock.now()
            val token =
                Token(
                    testTokenValidationInfo,
                    user.id,
                    createdAt = tokenCreationInstant,
                    lastUsedAt = tokenCreationInstant,
                    userAgent = "Mobile",
                )
            repo.createToken(token, 1)

            // then: createToken does not throw errors
            // no exception

            // when: retrieving the token and associated user
            val userAndToken = repo.getTokenByTokenValidationInfo(testTokenValidationInfo)

            // then:
            val (associatedUser, retrievedToken) = userAndToken ?: fail("token and associated user must exist")

            // and: ...
            assertEquals(username, associatedUser.username)
            assertEquals(testTokenValidationInfo.validationInfo, retrievedToken.tokenValidationInfo.validationInfo)
            assertEquals(tokenCreationInstant, retrievedToken.createdAt)

            // finally: clear data
            clearData(jdbi, "dbo.Tokens", "user_id", user.id.value)
            clearData(jdbi, "dbo.Users", "id", user.id.value)
        }

    @Test
    fun `update token`() =
        runWithHandle { handle ->
            // given: a UsersRepository
            val repo = JdbiUsersRepository(handle)

            // and: a test clock
            val clock = TestClock()

            // when: a user is created
            val username = Username(newTestUserName())
            val email = Email(newTestEmail(username.value))
            val salt = newTestSalt()
            val iteration = newTestIteration()
            val passwordValidationInfo = PasswordValidationInfo(newTokenValidationData())

            // when: storing the user and preferences
            repo.storeUser(username, email, salt, iteration, passwordValidationInfo)

            // then: the user is created
            val user = repo.getUserByUsername(username)
            assertNotNull(user)

            // and: test TokenValidationInfo
            val testTokenValidationInfo = TokenValidationInfo(newTokenValidationData())

            // when: creating a token
            val tokenCreationInstant = clock.now()
            val token =
                Token(
                    testTokenValidationInfo,
                    user.id,
                    createdAt = tokenCreationInstant,
                    lastUsedAt = tokenCreationInstant,
                    userAgent = "Mobile",
                )
            repo.createToken(token, 1)

            // then: createToken does not throw errors
            // no exception

            // when: updating the token
            val newInstant = Instant.fromEpochSeconds(tokenCreationInstant.epochSeconds + 1)
            val newToken = Token(token.tokenValidationInfo, token.userId, token.createdAt, newInstant, "Mobile")
            repo.updateTokenLastUsed(newToken, newInstant)

            // then: updateTokenLastUsed does not throw errors
            // no exception

            // when: retrieving the token and associated user
            val userAndToken = repo.getTokenByTokenValidationInfo(testTokenValidationInfo)

            // then:
            val (associatedUser, retrievedToken) = userAndToken ?: fail("token and associated user must exist")

            // and: ...
            assertEquals(username, associatedUser.username)
            assertEquals(testTokenValidationInfo.validationInfo, retrievedToken.tokenValidationInfo.validationInfo)
            assertEquals(tokenCreationInstant, retrievedToken.createdAt)
            assertEquals(newInstant, retrievedToken.lastUsedAt)

            // finally: clear data
            clearData(jdbi, "dbo.Tokens", "user_id", user.id.value)
            clearData(jdbi, "dbo.Users", "id", user.id.value)
        }

    @Test
    fun `delete token`() =
        runWithHandle { handle ->
            // given: a UsersRepository
            val repo = JdbiUsersRepository(handle)

            // and: a test clock
            val clock = TestClock()

            // when: a user is created
            val username = Username(newTestUserName())
            val email = Email(newTestEmail(username.value))
            val salt = newTestSalt()
            val iteration = newTestIteration()
            val passwordValidationInfo = PasswordValidationInfo(newTokenValidationData())

            // when: storing the user and preferences
            repo.storeUser(username, email, salt, iteration, passwordValidationInfo)

            // then: the user is created
            val user = repo.getUserByUsername(username)
            assertNotNull(user)

            // and: test TokenValidationInfo
            val testTokenValidationInfo = TokenValidationInfo(newTokenValidationData())

            // when: creating a token
            val tokenCreationInstant = clock.now()
            val token =
                Token(
                    testTokenValidationInfo,
                    user.id,
                    createdAt = tokenCreationInstant,
                    lastUsedAt = tokenCreationInstant,
                    "Mobile",
                )
            repo.createToken(token, 1)

            // then: createToken does not throw errors
            // no exception

            // when: deleting the token
            repo.removeTokenByValidationInfo(testTokenValidationInfo)

            // then: deleteToken does not throw errors
            // no exception

            // when: retrieving the token and associated user
            val userAndToken = repo.getTokenByTokenValidationInfo(testTokenValidationInfo)

            // then:
            assertEquals(null, userAndToken)

            // finally: clear data
            clearData(jdbi, "dbo.Users", "id", user.id.value)
        }

    companion object {
        private fun runWithHandle(block: (Handle) -> Unit) = jdbi.useTransaction<Exception>(block)

        private fun newTestUserName() = "user-${abs(Random.nextLong())}"

        private fun newTokenValidationData() = "token-${abs(Random.nextLong())}"

        private fun clearData(
            jdbi: Jdbi,
            tableName: String,
            columnName: String,
            userId: Int,
        ) {
            jdbi.useHandle<Exception> { handle ->
                handle.execute("delete from $tableName where $columnName = $userId")
            }
        }

        private val jdbi =
            Jdbi
                .create(
                    PGSimpleDataSource().apply {
                        setURL(Environment.getDbUrl())
                    },
                ).configureWithAppRequirements()
    }
}
