package pt.isel.leic.multicloudguardian

import kotlinx.datetime.Clock
import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.leic.multicloudguardian.domain.provider.AmazonS3StorageConfig
import pt.isel.leic.multicloudguardian.domain.provider.AzureStorageConfig
import pt.isel.leic.multicloudguardian.domain.provider.BackBlazeStorageConfig
import pt.isel.leic.multicloudguardian.domain.provider.GoogleCloudStorageConfig
import pt.isel.leic.multicloudguardian.domain.token.Sha256TokenEncoder
import pt.isel.leic.multicloudguardian.domain.user.UsersDomainConfig
import pt.isel.leic.multicloudguardian.repository.jdbi.configureWithAppRequirements
import kotlin.time.Duration.Companion.hours

@SpringBootApplication
class MultiCloudGuardianApplication {
    @Bean
    fun jdbi() =
        Jdbi
            .create(
                PGSimpleDataSource().apply {
                    setURL(Environment.getDbUrl())
                },
            ).configureWithAppRequirements()

    @Bean
    fun googleCloudStorageConfig() =
        GoogleCloudStorageConfig(
            bucketName = Environment.getBucketName(),
            identity = Environment.getGoogleIdentity(),
            credential = Environment.getGoogleCredentials(),
        )

    @Bean
    fun azureStorageConfig() =
        AzureStorageConfig(
            bucketName = Environment.getBucketName(),
            identity = Environment.getAzureIdentity(),
            credential = Environment.getAzureCredentials(),
        )

    @Bean
    fun backBlazeStorageConfig() =
        BackBlazeStorageConfig(
            bucketName = Environment.getBucketName(),
            identity = Environment.getBackBlazeIdentity(),
            credential = Environment.getBackBlazeCredentials(),
        )

    @Bean
    fun amazonS3Config() =
        AmazonS3StorageConfig(
            bucketName = Environment.getBucketName(),
            identity = Environment.getAmazonIdentity(),
            credential = Environment.getAmazonCredentials(),
        )

    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

    @Bean
    fun tokenEncoder() = Sha256TokenEncoder()

    @Bean
    fun clock() = Clock.System

    @Bean
    fun usersDomainConfig() =
        UsersDomainConfig(
            tokenSizeInBytes = 256 / 8,
            tokenTtl = 24.hours,
            tokenRollingTtl = 1.hours,
            maxTokensPerUser = 3,
        )
}

fun main(args: Array<String>) {
    runApplication<MultiCloudGuardianApplication>(*args)
}
