package pt.isel.leic.multicloudguardian.domain.provider

enum class ProviderType(val provider: String) {
    AMAZON("aws-s3"),
    AZURE("azureblob"),
    GOOGLE("google-cloud-storage"),
    BACKBLAZE("b2");

    companion object {
        fun fromString(value: String): ProviderType? = entries.find { it.provider == value }
        fun isProviderType(value: String): Boolean = entries.any { it.provider == value }
    }
}

