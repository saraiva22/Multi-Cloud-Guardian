package pt.isel.leic.multicloudguardian.domain.sse

import kotlinx.datetime.Instant
import pt.isel.leic.multicloudguardian.domain.folder.InviteStatus

sealed interface Event {
    data class File(
        val id: Long,
        val fileId: Int,
        val user: UserInfoOutput,
        val folderInfo: FolderInfoOutput?,
        val fileName: String,
        val path: String,
        val size: Long,
        val contentType: String,
        val createdAt: Long,
        val encryption: Boolean,
    ) : Event

    data class DeleteFile(
        val id: Long,
        val fileId: Int,
        val user: UserInfoOutput,
        val folderInfo: FolderInfoOutput?,
        val fileName: String,
        val createdAt: Long,
    ) : Event

    data class LeaveFolder(
        val id: Long,
        val user: UserInfoOutput,
        val folderInfo: FolderInfoOutput?,
    ) : Event

    data class Invite(
        val id: Long,
        val inviteId: Int,
        val status: InviteStatus,
        val user: UserInfoOutput,
        val folderId: Int,
        val folderName: String,
    ) : Event

    data class RespondInvite(
        val id: Long,
        val inviteId: Int,
        val status: InviteStatus,
        val user: UserInfoOutput,
        val folderId: Int,
        val folderName: String,
    ) : Event

    class NewMember(
        val id: Long,
        val ownerId: Int,
        val newMember: UserInfoOutput,
        val folderId: Int,
        val folderName: String,
    ) : Event

    data class KeepAlive(
        val timestamp: Instant,
    ) : Event
}
