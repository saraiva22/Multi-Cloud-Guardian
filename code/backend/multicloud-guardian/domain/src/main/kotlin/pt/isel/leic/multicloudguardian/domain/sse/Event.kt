package pt.isel.leic.multicloudguardian.domain.sse

import kotlinx.datetime.Instant
import pt.isel.leic.multicloudguardian.domain.folder.FolderInfo
import pt.isel.leic.multicloudguardian.domain.folder.InviteStatus
import pt.isel.leic.multicloudguardian.domain.user.UserInfo

sealed interface Event {
    data class File(
        val id: Long,
        val fileId: Int,
        val user: UserInfo,
        val folderInfo: FolderInfo?,
        val fileName: String,
        val path: String,
        val size: Long,
        val contentType: String,
        val createdAt: String,
        val encryption: Boolean,
    ) : Event

    data class Invite(
        val id: Long,
        val inviteId: Int,
        val status: InviteStatus,
        val user: UserInfo,
        val folderId: Int,
        val folderName: String,
    ) : Event

    data class KeepAlive(
        val timestamp: Instant,
    ) : Event
}
