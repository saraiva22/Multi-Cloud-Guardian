package pt.isel.leic.multicloudguardian.domain.provider

/**
 * Enum class representing the different types of cloud storage providers.
 *
 * @property AMAZON Represents the Amazon S3 cloud storage provider.
 * @property AZURE Represents the Azure Blob Storage cloud storage provider.
 * @property GOOGLE Represents the Google Cloud Storage cloud storage provider.
 * @property BACKBLAZE Represents the Backblaze B2 cloud storage provider.
 */

enum class ProviderType {
    AMAZON,
    AZURE,
    GOOGLE,
    BACKBLAZE,
    ;

    companion object {
        private const val AMAZON_NAME = "aws-s3"
        private const val AZURE_NAME = "azureblob"
        private const val GOOGLE_NAME = "google-cloud-storage"
        private const val BACKBLAZE_NAME = "b2"

        fun fromInt(value: Int) = entries[value]

        fun convertString(value: Int): String =
            when (entries[value]) {
                AMAZON -> AMAZON_NAME
                AZURE -> AZURE_NAME
                GOOGLE -> GOOGLE_NAME
                BACKBLAZE -> BACKBLAZE_NAME
            }
    }
}
