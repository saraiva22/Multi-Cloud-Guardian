package pt.isel.leic.multicloudguardian.domain.provider

import org.springframework.stereotype.Component
import pt.isel.leic.multicloudguardian.domain.provider.components.StorageConfig

@Component
class ProviderDomain(
    private val googleConfig: GoogleCloudStorageConfig,
    //  private val amazonConfig: AmazonS3StorageConfig,
    //  private val azureConfig: AzureStorageConfig,
    // private val backBlazeConfig: BackBlazeStorageConfig,
) {
    private val providerConfigs: Map<ProviderType, StorageConfig> =
        mapOf(
            //    ProviderType.AMAZON to amazonConfig,
            ProviderType.GOOGLE to googleConfig,
            //   ProviderType.AZURE to azureConfig,
            //  ProviderType.BACK_BLAZE to backBlazeConfig,
        )

    private fun getConfig(providerType: ProviderType): StorageConfig? = providerConfigs[providerType]

    fun getCredential(providerType: ProviderType): String? = getConfig(providerType)?.credential

    fun getBucketName(providerType: ProviderType): String? = getConfig(providerType)?.bucketName

    fun getIdentity(providerType: ProviderType): String? = getConfig(providerType)?.identity
}
