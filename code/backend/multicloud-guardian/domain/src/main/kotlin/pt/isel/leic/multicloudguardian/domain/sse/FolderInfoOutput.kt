package pt.isel.leic.multicloudguardian.domain.sse

import pt.isel.leic.multicloudguardian.domain.folder.FolderInfo
import pt.isel.leic.multicloudguardian.domain.folder.FolderType

data class FolderInfoOutput(
    val folderId: Int?,
    val folderName: String?,
    val folderType: FolderType?,
) {
    companion object {
        fun fromDomain(folderInfo: FolderInfo?): FolderInfoOutput? =
            folderInfo?.let {
                FolderInfoOutput(
                    it.id.value,
                    it.folderName,
                    it.folderType,
                )
            }
    }
}
