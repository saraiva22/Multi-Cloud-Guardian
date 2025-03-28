package pt.isel.leic.multicloudguardian.domain.provider

import org.springframework.stereotype.Component
import pt.isel.leic.multicloudguardian.domain.provider.components.StorageConfig

@Component
class ProviderDomainConfig(
    private val googleConfig: GoogleCloudStorageConfig,
    private val amazonConfig: AmazonS3StorageConfig,
    private val azureConfig: AzureStorageConfig,
    private val backBlazeConfig: BackBlazeStorageConfig,
) {
    private fun getConfig(providerType: ProviderType): StorageConfig =
        when (providerType) {
            ProviderType.AMAZON -> amazonConfig
            ProviderType.GOOGLE -> googleConfig
            ProviderType.AZURE -> azureConfig
            ProviderType.BACK_BLAZE -> backBlazeConfig
        }

    fun getCredential(providerType: ProviderType): String = getConfig(providerType).credential

    fun getBucketName(providerType: ProviderType): String = getConfig(providerType).bucketName

    fun getIdentity(providerType: ProviderType): String = getConfig(providerType).identity

    fun getLocation(providerType: ProviderType): String = getConfig(providerType).location
}
