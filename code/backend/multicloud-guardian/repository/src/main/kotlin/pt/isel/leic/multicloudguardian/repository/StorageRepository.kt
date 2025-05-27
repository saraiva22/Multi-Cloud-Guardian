package pt.isel.leic.multicloudguardian.repository

import kotlinx.datetime.Instant
import pt.isel.leic.multicloudguardian.domain.file.File
import pt.isel.leic.multicloudguardian.domain.file.FileCreate
import pt.isel.leic.multicloudguardian.domain.folder.Folder
import pt.isel.leic.multicloudguardian.domain.utils.Id

interface StorageRepository {
    fun storeFile(
        file: FileCreate,
        path: String,
        url: String,
        userId: Id,
        folderId: Id?,
        encryption: Boolean,
        createdAt: Instant,
    ): Id

    fun getFileNamesInFolder(
        userId: Id,
        folderId: Id?,
    ): List<String>

    fun isFileNameInFolder(
        userId: Id,
        folderId: Id?,
        fileName: String,
    ): Boolean

    fun getFileById(
        userId: Id,
        fileId: Id,
    ): File?

    fun getFileInFolder(
        userId: Id,
        folderId: Id,
        fileId: Id,
    ): File?

    fun getFolderByName(
        userId: Id,
        parentFolderId: Id?,
        folderName: String,
    ): Folder?

    fun isFolderNameExists(
        userId: Id,
        parentFolderId: Id?,
        folderName: String,
    ): Boolean

    fun getFolderById(
        userId: Id,
        folderId: Id,
    ): Folder?

    fun getFiles(
        userId: Id,
        limit: Int,
        offset: Int,
        sort: String,
    ): List<File>

    fun countFiles(userId: Id): Long

    fun countFolder(userId: Id): Long

    fun getFolders(
        userId: Id,
        limit: Int,
        offset: Int,
        sort: String,
    ): List<Folder>

    fun getFilesInFolder(
        userId: Id,
        folderId: Id,
        limit: Int,
        offset: Int,
        sort: String,
    ): List<File>

    fun getPathById(
        userId: Id,
        fileId: Id,
    ): String?

    fun deleteFile(
        userId: Id,
        file: File,
    )

    fun deleteFolder(
        userId: Id,
        folder: Folder,
    )

    fun createFolder(
        userId: Id,
        folderName: String,
        parentFolderId: Id?,
        path: String,
        createdAt: Instant,
    ): Id
}
