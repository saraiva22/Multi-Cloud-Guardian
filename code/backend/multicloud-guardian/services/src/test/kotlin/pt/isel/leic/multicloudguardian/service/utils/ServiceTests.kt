package pt.isel.leic.multicloudguardian.service.utils

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.leic.multicloudguardian.Environment
import pt.isel.leic.multicloudguardian.domain.file.File
import pt.isel.leic.multicloudguardian.domain.file.FileCreate
import pt.isel.leic.multicloudguardian.domain.preferences.CostType
import pt.isel.leic.multicloudguardian.domain.preferences.LocationType
import pt.isel.leic.multicloudguardian.domain.preferences.PreferencesDomain
import pt.isel.leic.multicloudguardian.domain.provider.AmazonS3StorageConfig
import pt.isel.leic.multicloudguardian.domain.provider.AzureStorageConfig
import pt.isel.leic.multicloudguardian.domain.provider.BackBlazeStorageConfig
import pt.isel.leic.multicloudguardian.domain.provider.GoogleCloudStorageConfig
import pt.isel.leic.multicloudguardian.domain.provider.ProviderDomainConfig
import pt.isel.leic.multicloudguardian.domain.provider.ProviderType
import pt.isel.leic.multicloudguardian.domain.token.Sha256TokenEncoder
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.domain.user.UserInfo
import pt.isel.leic.multicloudguardian.domain.user.UsersDomain
import pt.isel.leic.multicloudguardian.domain.user.UsersDomainConfig
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.repository.jdbi.JdbiTransactionManager
import pt.isel.leic.multicloudguardian.service.sse.SSEService
import pt.isel.leic.multicloudguardian.service.storage.StorageService
import pt.isel.leic.multicloudguardian.service.storage.apis.AmazonApi
import pt.isel.leic.multicloudguardian.service.storage.apis.AzureApi
import pt.isel.leic.multicloudguardian.service.storage.apis.BackBlazeApi
import pt.isel.leic.multicloudguardian.service.storage.apis.GoogleApi
import pt.isel.leic.multicloudguardian.service.storage.jclouds.StorageFileJclouds
import pt.isel.leic.multicloudguardian.service.user.UsersService
import java.util.Base64
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

open class ServiceTests : ApplicationTests() {
    companion object {
        fun createUsersService(
            sseService: SSEService,
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
            sseService,
            testClock,
        )

        fun createUserInService(
            userName: String,
            password: String,
            email: String,
            salt: String,
            iteration: Int,
            performance: CostType,
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

        fun fileCreation(encryption: Boolean = false): FileCreate {
            val randomNumber = Random.nextInt(1, 1_000_000)
            val blobName = "test-file-$randomNumber.txt"
            val fileContent = "This is a test file content for $blobName ${abs(Random.nextLong())}"
            val contentType = "text/plain"
            val size = fileContent.toByteArray().size.toLong()

            val encryptedKey =
                if (encryption) {
                    val keyWithIV = ByteArray(32)
                    Random.nextBytes(keyWithIV)
                    Base64.getEncoder().encodeToString(keyWithIV)
                } else {
                    ""
                }

            return FileCreate(
                blobName = blobName,
                fileContent = fileContent.toByteArray(),
                contentType = contentType,
                size = size,
                encryption = encryption,
                encryptedKey = encryptedKey,
            )
        }

        fun createFile(
            user: User,
            fileCreate: FileCreate,
        ): File {
            val file = fileCreate

            val createFie = createStorageService().uploadFile(file, file.encryption, user)

            val fileId =
                when (createFie) {
                    is Failure -> {
                        fail("Unexpected $createFie")
                    }
                    is Success -> createFie
                }

            val getFileResult =
                createStorageService()
                    .getFileById(user, fileId.value)

            return when (getFileResult) {
                is Failure -> {
                    fail("Unexpected $getFileResult")
                }
                is Success -> getFileResult.value
            }
        }

        val testClock = TestClock()

        fun getCredentials(providerType: ProviderType) = providerDomain.getCredential(providerType)

        fun getIdentity(providerType: ProviderType) = providerDomain.getIdentity(providerType)

        fun getLocation(providerType: ProviderType) = providerDomain.getLocation(providerType)

        fun getBucketName(providerType: ProviderType) = providerDomain.getBucketName(providerType)

        private val sseService = SSEService()

        private val userServices = createUsersService(sseService, testClock)

        private val providerDomain =
            ProviderDomainConfig(googleCloudStorageConfig(), amazonS3Config(), azureStorageConfig(), backBlazeStorageConfig())

        private fun googleCloudStorageConfig() =
            GoogleCloudStorageConfig(
                bucketName = Environment.getBucketName(),
                identity = Environment.getGoogleIdentity(),
                credential = Environment.getGoogleCredentials(),
                location = Environment.getGoogleLocation(),
            )

        private fun azureStorageConfig() =
            AzureStorageConfig(
                bucketName = Environment.getBucketName(),
                identity = Environment.getAzureIdentity(),
                credential = Environment.getAzureCredentials(),
                location = Environment.getAzureLocation(),
            )

        private fun backBlazeStorageConfig() =
            BackBlazeStorageConfig(
                bucketName = Environment.getBucketName(),
                identity = Environment.getBackBlazeIdentity(),
                credential = Environment.getBackBlazeCredentials(),
                location = Environment.getBackBlazeLocation(),
            )

        private fun amazonS3Config() =
            AmazonS3StorageConfig(
                bucketName = Environment.getBucketName(),
                identity = Environment.getAmazonIdentity(),
                credential = Environment.getAmazonCredentials(),
                location = Environment.getAmazonLocation(),
            )

        private val azureApi = AzureApi()
        private val googleApi = GoogleApi()
        private val amazonApi = AmazonApi()
        private val backBlazeApi = BackBlazeApi()

        val jcloudsStorage = StorageFileJclouds(azureApi, googleApi, amazonApi, backBlazeApi)

        lateinit var testUser: User
        lateinit var testUser2: User
        lateinit var testUser3: User

        lateinit var testUserInfo: UserInfo
        lateinit var testUserInfo2: UserInfo
        lateinit var testUserInfo3: UserInfo

        const val DEFAULT_LIMIT = 10
        const val DEFAULT_PAGE = 0
        const val DEFAULT_SORT = "created_asc"

        fun createStorageService(clock: TestClock = testClock): StorageService =
            StorageService(
                JdbiTransactionManager(jdbi),
                jcloudsStorage,
                providerDomain,
                sseService,
                clock,
            )

        @JvmStatic
        @BeforeAll
        fun setupDB() {
            testUser = createUser(CostType.HIGH, LocationType.EUROPE) // User associated Azure Provider
            testUser2 = createUser(CostType.LOW, LocationType.NORTH_AMERICA) // User associated BackBlaze Provider
            testUser3 = createUser(CostType.HIGH, LocationType.EUROPE) // User associated Azure Provider
            testUserInfo = UserInfo(testUser.id, testUser.username, testUser.email)
            testUserInfo2 = UserInfo(testUser2.id, testUser2.username, testUser2.email)
            testUserInfo3 = UserInfo(testUser3.id, testUser3.username, testUser3.email)
        }

        private fun createUser(
            performance: CostType,
            location: LocationType,
        ): User {
            val username = newTestUserName()
            val password = newTestPassword()
            val email = newTestEmail(username)
            val salt = newTestSalt()
            val iteration = newTestIteration()

            return createUserInService(username, password, email, salt, iteration, performance, location)
        }

        @JvmStatic
        @AfterAll
        fun clearDB() {
            clearData(jdbi, "dbo.Users", "id", testUser.id.value)
            clearData(jdbi, "dbo.Users", "id", testUser2.id.value)
            clearData(jdbi, "dbo.Users", "id", testUser3.id.value)
        }
    }
}
