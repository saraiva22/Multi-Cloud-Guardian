package pt.isel.leic.multicloudguardian.domain.folder

import pt.isel.leic.multicloudguardian.domain.utils.Id

data class FolderInfo(
    val id: Id,
    val folderName: String,
    val folderType: FolderType,
)
