package pt.isel.leic.multicloudguardian.service.utils

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.leic.multicloudguardian.domain.preferences.LocationType
import pt.isel.leic.multicloudguardian.domain.preferences.PerformanceType
import pt.isel.leic.multicloudguardian.domain.preferences.PreferencesDomain
import pt.isel.leic.multicloudguardian.domain.provider.ProviderDomainConfig
import pt.isel.leic.multicloudguardian.domain.token.Sha256TokenEncoder
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.domain.user.UsersDomain
import pt.isel.leic.multicloudguardian.domain.user.UsersDomainConfig
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.repository.jdbi.JdbiTransactionManager
import pt.isel.leic.multicloudguardian.service.storage.StorageService
import pt.isel.leic.multicloudguardian.service.storage.jclouds.StorageFileJclouds
import pt.isel.leic.multicloudguardian.service.user.UsersService
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

open class ServiceTests : ApplicationTests() {
    companion object {
        fun createUsersService(
            testClock: TestClock,
            tokenTtl: Duration = 30.days,
            tokenRollingTtl: Duration = 30.minutes,
            maxTokensPerUser: Int = 3,
        ) = UsersService(
            JdbiTransactionManager(jdbi),
            UsersDomain(
                BCryptPasswordEncoder(),
                Sha256TokenEncoder(),
                UsersDomainConfig(
                    tokenSizeInBytes = 256 / 8,
                    tokenTtl = tokenTtl,
                    tokenRollingTtl,
                    maxTokensPerUser = maxTokensPerUser,
                ),
            ),
            PreferencesDomain(),
            testClock,
        )

        fun storageService(
            jcloudsStorage: StorageFileJclouds,
            providerDomain: ProviderDomainConfig,
            clock: TestClock,
        ): StorageService =
            StorageService(
                JdbiTransactionManager(jdbi),
                jcloudsStorage,
                providerDomain,
                clock,
            )

        fun createUserInService(
            userName: String,
            password: String,
            email: String,
            salt: String,
            iteration: Int,
            performance: PerformanceType,
            location: LocationType,
        ): User {
            val createUserResult = userServices.createUser(userName, email, password, salt, iteration, performance, location)
            val userId =
                when (createUserResult) {
                    is Failure -> {
                        fail("Unexpected $createUserResult")
                    }
                    is Success -> createUserResult
                }

            val getUserResult = userServices.getUserById(userId.value.value)
            return when (getUserResult) {
                is Failure -> {
                    fail("Unexpected $getUserResult")
                }
                is Success -> getUserResult.value
            }
        }

        private val testClock = TestClock()
        private val userServices = createUsersService(testClock)

        private fun createUser(): User {
            val username = newTestUserName()
            val password = newTestPassword()
            val email = newTestEmail(username)
            val salt = newTestSalt()
            val iteration = newTestIteration()
            val performance = PerformanceType.MEDIUM
            val location = LocationType.EUROPE

            return createUserInService(username, password, email, salt, iteration, performance, location)
        }
    }
}
