package pt.isel.leic.multicloudguardian.domain.folder

import kotlinx.datetime.Instant
import pt.isel.leic.multicloudguardian.domain.user.UserInfo
import pt.isel.leic.multicloudguardian.domain.utils.Id

/** Represents a folder in the cloud storage.
 * @property folderId The unique identifier for the folder.
 * @property userId T
 * @property parentFolderId The unique identifier for the parent folder.
 * @property folderName The name of the folder.
 * @property size The size of the folder in bytes.
 * @property numberFiles The number of files in the folder.
 * @property path The path of the folder.
 * @property type The type of the folder, which can be either a regular folder or a shared folder.
 * @property createdAt The [Instant]  when the folder was created.
 * @property updatedAt The [Instant]  of the last folder update.
 * */

private const val MAX_FOLDER_NAME_LENGTH = 25
private const val MIN_FOLDER_NAME_LENGTH = 2

data class Folder(
    val folderId: Id,
    val user: UserInfo,
    val parentFolderId: Id?,
    val folderName: String,
    val size: Long,
    val numberFiles: Int,
    val path: String,
    val type: FolderType,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        const val MIN_LENGTH = MIN_FOLDER_NAME_LENGTH

        const val MAX_LENGTH = MAX_FOLDER_NAME_LENGTH
    }
}
