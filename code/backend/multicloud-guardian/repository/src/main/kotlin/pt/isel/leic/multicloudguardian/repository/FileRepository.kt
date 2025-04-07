package pt.isel.leic.multicloudguardian.repository

import kotlinx.datetime.Instant
import pt.isel.leic.multicloudguardian.domain.file.File
import pt.isel.leic.multicloudguardian.domain.file.FileCreate
import pt.isel.leic.multicloudguardian.domain.folder.Folder
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

    fun getFolderByName(
        userId: Id,
        parentFolderId: Id?,
        folderName: String,
    ): Folder?

    fun getFolderById(
        userId: Id,
        folderId: Id,
    ): Folder?

    fun getFiles(userId: Id): List<File>

    fun getPathById(
        userId: Id,
        fileId: Id,
    ): String?

    fun deleteFile(file: File)

    fun createFolder(
        userId: Id,
        folderName: String,
        parentFolderId: Id?,
        path: String,
        createdAt: Instant,
    ): Id
}
