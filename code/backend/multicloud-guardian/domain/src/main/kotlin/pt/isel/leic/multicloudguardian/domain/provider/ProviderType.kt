package pt.isel.leic.multicloudguardian.domain.provider

/**
 * Enum class representing the different types of cloud storage providers.
 *
 * @property AMAZON Represents the Amazon S3 cloud storage provider.
 * @property AZURE Represents the Azure Blob Storage cloud storage provider.
 * @property GOOGLE Represents the Google Cloud Storage cloud storage provider.
 * @property BACK_BLAZE Represents the Back_blaze B2 cloud storage provider.
 */

enum class ProviderType {
    AMAZON,
    AZURE,
    GOOGLE,
    BACK_BLAZE,
    ;

    companion object {
        private const val AMAZON_NAME = "aws-s3"
        private const val AZURE_NAME = "azureblob"
        private const val GOOGLE_NAME = "google-cloud-storage"
        private const val BACK_BLAZE_NAME = "b2"

        fun fromInt(value: Int) = entries[value]

        fun convertString(providerType: ProviderType): String =
            when (entries[providerType.ordinal]) {
                AMAZON -> AMAZON_NAME
                AZURE -> AZURE_NAME
                GOOGLE -> GOOGLE_NAME
                BACK_BLAZE -> BACK_BLAZE_NAME
            }
    }
}
