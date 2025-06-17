package pt.isel.leic.multicloudguardian.http.model.storage

import pt.isel.leic.multicloudguardian.domain.folder.InviteStatus

data class FolderInviteStatusInputModel(
    val inviteStatus: InviteStatus,
)
