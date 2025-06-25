package pt.isel.leic.multicloudguardian.http.model.storage

import pt.isel.leic.multicloudguardian.domain.folder.FolderType

data class FolderInfoDetailsModel(
    val folderId: Int,
    val folderName: String,
    val folderType: FolderType,
)
