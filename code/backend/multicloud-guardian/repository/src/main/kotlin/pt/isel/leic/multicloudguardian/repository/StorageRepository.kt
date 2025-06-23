package pt.isel.leic.multicloudguardian.repository

import kotlinx.datetime.Instant
import pt.isel.leic.multicloudguardian.domain.file.File
import pt.isel.leic.multicloudguardian.domain.file.FileCreate
import pt.isel.leic.multicloudguardian.domain.folder.Folder
import pt.isel.leic.multicloudguardian.domain.folder.FolderPrivateInvite
import pt.isel.leic.multicloudguardian.domain.folder.FolderType
import pt.isel.leic.multicloudguardian.domain.folder.InviteStatus
import pt.isel.leic.multicloudguardian.domain.user.UserInfo
import pt.isel.leic.multicloudguardian.domain.utils.Id

interface StorageRepository {
    fun storeFile(
        file: FileCreate,
        path: String,
        url: String,
        userId: Id,
        folderId: Id?,
        fileFakeName: String,
        encryption: Boolean,
        createdAt: Instant,
        updatedAt: Instant?,
    ): Id

    fun getFileNamesInFolder(
        userId: Id,
        folderId: Id?,
    ): List<String>

    fun isFileFakeNameInFolder(
        userId: Id,
        fileFakeName: String,
        folderId: Id?,
    ): Boolean

    fun isFileNameInFolder(
        userId: Id,
        folderId: Id?,
        fileName: String,
    ): Boolean

    fun getFileById(fileId: Id): File?

    fun getFileInFolder(
        folderId: Id,
        fileId: Id,
    ): File?

    fun getFolderByName(
        parentFolderId: Id?,
        folderName: String,
    ): Folder?

    fun isFolderNameExists(
        parentFolderId: Id?,
        folderName: String,
    ): Boolean

    fun membersInFolder(folderId: Id): List<UserInfo>

    fun getFolderById(
        folderId: Id,
        members: Boolean,
    ): Pair<Folder, List<UserInfo>>?

    fun getFiles(
        userId: Id,
        limit: Int,
        offset: Int,
        sort: String,
        shared: Boolean = false,
        search: String? = null,
    ): List<File>

    fun countFiles(
        userId: Id,
        shared: Boolean = false,
        search: String? = null,
    ): Long

    fun countFolder(
        userId: Id,
        shared: Boolean = false,
        search: String? = null,
    ): Long

    fun getFolders(
        userId: Id,
        limit: Int,
        offset: Int,
        sort: String,
        shared: Boolean = false,
        search: String? = null,
    ): List<Folder>

    fun getFoldersInFolder(
        userId: Id,
        folderId: Id,
        limit: Int,
        offset: Int,
        sort: String,
    ): Pair<List<Folder>, Long>

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
        updatedAt: Instant?,
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
        folderType: FolderType,
        createdAt: Instant,
    ): Id

    fun updateFilePath(
        userId: Id,
        file: File,
        newPath: String,
        updateAt: Instant,
        folderId: Id?,
    )

    fun isMemberOfFolder(
        userId: Id,
        folderId: Id,
    ): Boolean

    fun createInviteFolder(
        inviterId: Id,
        guestId: Id,
        folderId: Id,
    ): Id

    fun isInviteCodeValid(
        userId: Id,
        folderId: Id,
        inviteId: Id,
    ): Boolean

    fun isOwnerOfFolder(
        userId: Id,
        folderId: Id,
    ): Boolean

    fun folderInviteUpdated(
        guestId: Id,
        inviteId: Id,
        inviteStatus: InviteStatus,
    )

    fun getReceivedFolderInvites(
        userId: Id,
        limit: Int,
        offset: Int,
        sort: String,
    ): List<FolderPrivateInvite>

    fun getSentFolderInvites(
        userId: Id,
        limit: Int,
        offset: Int,
        sort: String,
    ): List<FolderPrivateInvite>

    fun countReceivedFolderInvites(userId: Id): Long

    fun countSentFolderInvites(userId: Id): Long

    fun leaveFolder(
        userId: Id,
        folderId: Id,
    ): Boolean
}
