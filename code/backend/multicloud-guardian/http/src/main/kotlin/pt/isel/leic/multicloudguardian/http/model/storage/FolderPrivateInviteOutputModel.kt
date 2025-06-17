package pt.isel.leic.multicloudguardian.http.model.storage

import pt.isel.leic.multicloudguardian.domain.folder.FolderPrivateInvite
import pt.isel.leic.multicloudguardian.domain.folder.InviteStatus
import pt.isel.leic.multicloudguardian.http.model.user.UserInfoOutputModel

data class FolderPrivateInviteOutputModel(
    val inviteId: Int,
    val folderId: Int,
    val folderName: String,
    val user: UserInfoOutputModel,
    val status: InviteStatus,
) {
    companion object {
        fun fromDomain(invite: FolderPrivateInvite): FolderPrivateInviteOutputModel =
            FolderPrivateInviteOutputModel(
                inviteId = invite.inviteId.value,
                folderId = invite.folderId.value,
                folderName = invite.folderName,
                user =
                    UserInfoOutputModel(
                        invite.user.id.value,
                        invite.user.username.value,
                        invite.user.email.value,
                    ),
                status = invite.status,
            )
    }
}
