package pt.isel.leic.multicloudguardian.domain.provider

import pt.isel.leic.multicloudguardian.domain.provider.components.StorageConfig

data class GoogleCloudStorageConfig(
    override val bucketName: String,
    override val identity: String,
    override val credential: String,
    override val location: String,
) : StorageConfig
