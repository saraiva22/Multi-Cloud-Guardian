package pt.isel.leic.multicloudguardian.repository

import kotlinx.datetime.Instant
import pt.isel.leic.multicloudguardian.domain.file.File
import pt.isel.leic.multicloudguardian.domain.file.FileCreate
import pt.isel.leic.multicloudguardian.domain.utils.Id

interface FileRepository {
    fun storeFile(
        file: FileCreate,
        path: String,
        checkSum: Long,
        url: String,
        userId: Id,
        encryption: Boolean,
        createdAt: Instant,
    ): Id

    fun getFileNames(userId: Id): List<String>

    fun getFileById(
        userId: Id,
        fileId: Id,
    ): File?

    fun getPathById(
        userId: Id,
        fileId: Id,
    ): String?

    fun deleteFile(file: File)
}
