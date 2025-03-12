package pt.isel.leic.multicloudguardian.domain.file

import pt.isel.leic.multicloudguardian.domain.components.Id
import pt.isel.leic.multicloudguardian.domain.provider.Provider

data class File(
    val fileId: Id,
    val name: String,
    val storageProvider: Provider,
    val encryption: Boolean,
    val size: Int,
    val checkSum: Long,
    val url: String
)
