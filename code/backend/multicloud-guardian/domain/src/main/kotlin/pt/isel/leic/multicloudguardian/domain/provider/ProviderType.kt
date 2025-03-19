package pt.isel.leic.multicloudguardian.domain.provider

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
