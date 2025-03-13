package pt.isel.leic.multicloudguardian.domain.file

import pt.isel.leic.multicloudguardian.domain.components.Id
import pt.isel.leic.multicloudguardian.domain.provider.ProviderType

data class File(
    val fileId: Id,
    val name: String,
    val storageProvider: ProviderType,
    val encryption: Boolean,
    val size: Int,
    val checkSum: Long,
    val path : String,
    val url: String
)
