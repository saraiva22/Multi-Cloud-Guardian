package pt.isel.leic.multicloudguardian.domain.file

import pt.isel.leic.multicloudguardian.domain.utils.Id

/** Represents a file in the cloud storage.
 * @property fileId The unique identifier for the file.
 * @property userId The unique identifier for the user.
 * @property folderId The unique identifier for the folder. But it can be null if the file is in the root directory.
 * @property name The name of the file.
 * @property checksum The checksum of the file.
 * @property path The path of the file.
 * @property size The size of the file in bytes.
 * @property encryption The encryption status of the file.
 * @property url The URL of the file.
 */

data class File(
    val fileId: Id,
    val userId: Id,
    val folderId: Id?,
    val name: String,
    val checksum: Long,
    val path: String,
    val size: Int,
    val encryption: Boolean,
    val url: String,
)
