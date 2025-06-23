package pt.isel.leic.multicloudguardian.domain.file

import kotlinx.datetime.Instant
import pt.isel.leic.multicloudguardian.domain.folder.FolderInfo
import pt.isel.leic.multicloudguardian.domain.user.UserInfo
import pt.isel.leic.multicloudguardian.domain.utils.Id

/** Represents a file in the cloud storage.
 * @property fileId The unique identifier for the file.
 * @property user The unique identifier for the user.
 * @property folderInfo The unique identifier for the folder. But it can be null if the file is in the root directory.
 * @property fileName The name of the file.
 * @property fileFakeName The fake name of the file, which is used to obfuscate the original file name.
 * @property path The path of the file.
 * @property size The size of the file in bytes.
 * @property contentType The MIME type of the file (e.g., image/png, application/pdf).
 * @property createdAt The [Instant] when the file was created.
 * @property encryption The encryption status of the file.
 */

data class File(
    val fileId: Id,
    val user: UserInfo,
    val folderInfo: FolderInfo?,
    val fileName: String,
    val fileFakeName: String,
    val path: String,
    val size: Long,
    val contentType: String,
    val createdAt: Instant,
    val encryption: Boolean,
    val encryptionKey: String?,
)
