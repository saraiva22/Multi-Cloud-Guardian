package pt.isel.leic.multicloudguardian.domain.folder

import kotlinx.datetime.Instant
import pt.isel.leic.multicloudguardian.domain.components.Id

/** Represents a folder in the cloud storage.
 * @property folderId The unique identifier for the folder.
 * @property userId The unique identifier for the user.
 * @property parentFolderId The unique identifier for the parent folder.
 * @property name The name of the folder.
 * @property size The size of the folder in bytes.
 * @property numberFiles The number of files in the folder.
 * @property createdAt The [Instant]  when the folder was created.
 * @property updatedAt The [Instant]  of the last folder update.
 * */

data class Folder(
    val folderId: Id,
    val userId: Id,
    val parentFolderId: Id?,
    val name: String,
    val size: Int,
    val numberFiles: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)
