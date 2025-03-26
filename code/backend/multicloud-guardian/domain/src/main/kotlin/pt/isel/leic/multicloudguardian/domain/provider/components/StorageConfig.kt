package pt.isel.leic.multicloudguardian.domain.provider.components

interface StorageConfig {
    val credential: String
    val bucketName: String
    val identity: String
    val location: String
}
