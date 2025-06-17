package pt.isel.leic.multicloudguardian.domain.folder

import pt.isel.leic.multicloudguardian.domain.user.UserInfo
import pt.isel.leic.multicloudguardian.domain.utils.Id

data class FolderPrivateInvite(
    val inviteId: Id,
    val folderId: Id,
    val folderName: String,
    val user: UserInfo,
    val status: InviteStatus,
)
