package pt.isel.leic.multicloudguardian.repository.jdbi

import kotlinx.datetime.Instant
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.slf4j.LoggerFactory
import pt.isel.leic.multicloudguardian.domain.file.File
import pt.isel.leic.multicloudguardian.domain.file.FileCreate
import pt.isel.leic.multicloudguardian.domain.folder.Folder
import pt.isel.leic.multicloudguardian.domain.folder.FolderPrivateInvite
import pt.isel.leic.multicloudguardian.domain.folder.FolderType
import pt.isel.leic.multicloudguardian.domain.folder.InviteStatus
import pt.isel.leic.multicloudguardian.domain.utils.Id
import pt.isel.leic.multicloudguardian.repository.StorageRepository

class JdbiStorageRepository(
    private val handle: Handle,
) : StorageRepository {
    override fun storeFile(
        file: FileCreate,
        path: String,
        url: String,
        userId: Id,
        folderId: Id?,
        encryption: Boolean,
        createdAt: Instant,
        updatedAt: Instant?,
    ): Id {
        val fileId =
            handle
                .createUpdate(
                    """
                    insert into dbo.Files (user_id, folder_id, file_name,path, size, content_type, created_at, encryption_key, encryption) 
                    values (:user_id,:folderId, :name,  :path, :size, :content_type, :created_at, :encryption_key, :encryption)
                    """.trimIndent(),
                ).bind("user_id", userId.value)
                .bind("folderId", folderId?.value)
                .bind("name", file.blobName)
                .bind("path", path)
                .bind("size", file.size)
                .bind("content_type", file.contentType)
                .bind("created_at", createdAt.epochSeconds)
                .bind("encryption_key", file.encryptedKey)
                .bind("encryption", encryption)
                .executeAndReturnGeneratedKeys()
                .mapTo<Int>()
                .one()

        logger.info("{} file stored in the database", file.blobName)

        if (updatedAt != null && folderId != null) {
            handle
                .createUpdate(
                    """
                    update dbo.Folders
                    set updated_at = :updated_at
                    where folder_id = :folder_id
                    """.trimIndent(),
                ).bind("updated_at", updatedAt.epochSeconds)
                .bind("folder_id", folderId.value)
                .execute()
        }

        logger.info("Folder updated at: {}", updatedAt)

        return Id(fileId)
    }

    override fun getFileNamesInFolder(
        userId: Id,
        folderId: Id?,
    ): List<String> =
        handle
            .createQuery(
                """
                select file_name from dbo.Files 
                where user_id = :user_id and ((:folderId IS NULL AND folder_id IS NULL) 
                OR folder_id = :folderId)
                """.trimIndent(),
            ).bind("user_id", userId.value)
            .bind("folderId", folderId?.value)
            .mapTo<String>()
            .list()

    override fun isFileNameInFolder(
        userId: Id,
        folderId: Id?,
        fileName: String,
    ): Boolean =
        handle
            .createQuery(
                """
                select count(*) from dbo.Files 
                where user_id = :userId 
                  and file_name = :fileName 
                  and folder_id IS NOT DISTINCT FROM :folderId
                """.trimIndent(),
            ).bind("userId", userId.value)
            .bind("fileName", fileName)
            .bind("folderId", folderId?.value)
            .mapTo<Int>()
            .single() == 1

    override fun getFileById(
        userId: Id,
        fileId: Id,
    ): File? =
        handle
            .createQuery(
                """
                select file.*, users.username, users.email, folder.folder_id as folder_id, folder.folder_name as folder_name
                from dbo.Files file
                inner join dbo.Users on file.user_id = users.id
                left join dbo.Folders folder on file.folder_id = folder.folder_id
                where file.file_id = :fileId and file.user_id = :userId
                """.trimIndent(),
            ).bind("userId", userId.value)
            .bind("fileId", fileId.value)
            .mapTo<File>()
            .singleOrNull()

    override fun getFileInFolder(
        userId: Id,
        folderId: Id,
        fileId: Id,
    ): File? =
        handle
            .createQuery(
                """
                select file.*, users.username, users.email, folder.folder_id as folder_id, folder.folder_name as folder_name
                from dbo.Files file
                inner join dbo.Users on file.user_id = users.id
                left join dbo.Folders folder on file.folder_id = folder.folder_id
                where file.file_id = :fileId and file.user_id = :userId
                and file.folder_id = :folderId
                """.trimIndent(),
            ).bind("userId", userId.value)
            .bind("folderId", folderId.value)
            .bind("fileId", fileId.value)
            .mapTo<File>()
            .singleOrNull()

    override fun getFolderByName(
        userId: Id,
        parentFolderId: Id?,
        folderName: String,
    ): Folder? =
        handle
            .createQuery(
                """
                select folder.*, users.username, users.email,
                       parent.folder_id as parent_id, parent.folder_name as parent_folder_name
                from dbo.Folders folder
                inner join dbo.Users on folder.user_id = users.id
                left join dbo.Folders parent on folder.parent_folder_id = parent.folder_id
                where folder.folder_name = :folderName and folder.user_id = :userId
                  and ((:parentFolderId IS NULL AND folder.parent_folder_id IS NULL)
                  OR folder.parent_folder_id = :parentFolderId)
                """.trimIndent(),
            ).bind("userId", userId.value)
            .bind("parentFolderId", parentFolderId?.value)
            .bind("folderName", folderName)
            .mapTo<Folder>()
            .singleOrNull()

    override fun isFolderNameExists(
        userId: Id,
        parentFolderId: Id?,
        folderName: String,
    ): Boolean =
        handle
            .createQuery(
                """
                select count(*) from dbo.Folders 
                where user_id = :userId
                and folder_name = :folderName
                and parent_folder_id IS NOT DISTINCT FROM :parentFolderId
                """.trimIndent(),
            ).bind("userId", userId.value)
            .bind("folderName", folderName)
            .bind("parentFolderId", parentFolderId?.value)
            .mapTo<Int>()
            .single() == 1

    override fun getFolderById(
        userId: Id,
        folderId: Id,
    ): Folder? =
        handle
            .createQuery(
                """
                select folder.*, users.username, users.email,
                       parent.folder_id as parent_id, parent.folder_name as parent_folder_name
                from dbo.Folders folder
                inner join dbo.Users on folder.user_id = users.id
                left join dbo.Folders parent on folder.parent_folder_id = parent.folder_id
                where folder.folder_id = :folderId and folder.user_id = :userId
                """.trimIndent(),
            ).bind("userId", userId.value)
            .bind("folderId", folderId.value)
            .mapTo<Folder>()
            .singleOrNull()

    override fun getFolderById(folderId: Id): Folder? =
        handle
            .createQuery(
                """
                select folder.*, users.username, users.email,
                       parent.folder_id as parent_id, parent.folder_name as parent_folder_name
                from dbo.Folders folder
                inner join dbo.Users on folder.user_id = users.id
                left join dbo.Folders parent on folder.parent_folder_id = parent.folder_id
                where folder.folder_id = :folderId
                """.trimIndent(),
            ).bind("folderId", folderId.value)
            .mapTo<Folder>()
            .singleOrNull()

    override fun getFiles(
        userId: Id,
        limit: Int,
        offset: Int,
        sort: String,
    ): List<File> {
        val order = orderBy(sort, "file_name")
        return handle
            .createQuery(
                """
                select file.*, users.username, users.email, folder.folder_id as folder_id, folder.folder_name as folder_name
                from dbo.Files file
                inner join dbo.Users on file.user_id = users.id
                left join dbo.Folders folder on file.folder_id = folder.folder_id
                where file.user_id = :userId
                order by $order
                LIMIT :limit OFFSET :offset
                """.trimIndent(),
            ).bind("userId", userId.value)
            .bind("limit", limit)
            .bind("offset", offset)
            .mapTo<File>()
            .toList()
    }

    override fun countFiles(userId: Id): Long =
        handle
            .createQuery(
                """
                select count(*) from dbo.Files where user_id = :userId
                """.trimIndent(),
            ).bind("userId", userId.value)
            .mapTo<Long>()
            .one()

    override fun countFolder(userId: Id): Long =
        handle
            .createQuery(
                """
                select count(*) from dbo.Folders where user_id = :userId
                """.trimIndent(),
            ).bind("userId", userId.value)
            .mapTo<Long>()
            .one()

    override fun getFolders(
        userId: Id,
        limit: Int,
        offset: Int,
        sort: String,
    ): List<Folder> {
        val order = orderBy(sort, "folder_name")
        return handle
            .createQuery(
                """
                select folder.*, users.username, users.email,
                       parent.folder_id as parent_id, parent.folder_name as parent_folder_name
                from dbo.Folders folder
                inner join dbo.Users on folder.user_id = users.id
                left join dbo.Folders parent on folder.parent_folder_id = parent.folder_id
                where folder.user_id = :userId
                order by $order
                LIMIT :limit OFFSET :offset
                """.trimIndent(),
            ).bind("userId", userId.value)
            .bind("limit", limit)
            .bind("offset", offset)
            .mapTo<Folder>()
            .toList()
    }

    override fun getFoldersInFolder(
        userId: Id,
        folderId: Id,
        limit: Int,
        offset: Int,
        sort: String,
    ): Pair<List<Folder>, Long> {
        val order = orderBy(sort, "folder_name")
        val totalElements =
            handle
                .createQuery(
                    """
                    select count(*) from dbo.Folders where user_id = :userId 
                    and parent_folder_id = :folderId
                    """.trimIndent(),
                ).bind("userId", userId.value)
                .bind("folderId", folderId.value)
                .mapTo<Long>()
                .one()
        val folders =
            handle
                .createQuery(
                    """
                    select folder.*, users.username, users.email,
                           parent.folder_id as parent_id, parent.folder_name as parent_folder_name
                    from dbo.Folders folder
                    inner join dbo.Users on folder.user_id = users.id
                    left join dbo.Folders parent on folder.parent_folder_id = parent.folder_id
                    where folder.user_id = :userId and folder.parent_folder_id = :folderId
                    order by $order
                    LIMIT :limit OFFSET :offset
                    """.trimIndent(),
                ).bind("userId", userId.value)
                .bind("folderId", folderId.value)
                .bind("limit", limit)
                .bind("offset", offset)
                .mapTo<Folder>()
                .toList()

        return Pair(folders, totalElements)
    }

    override fun getFilesInFolder(
        userId: Id,
        folderId: Id,
        limit: Int,
        offset: Int,
        sort: String,
    ): List<File> {
        val order = orderBy(sort, "file_name")

        return handle
            .createQuery(
                """
                select files.*, users.username, users.email, folders.folder_id as folder_id, folders.folder_name as folder_name
                from dbo.Files 
                inner join dbo.folders on files.folder_id = folders.folder_id 
                inner join dbo.users on folders.user_id = users.id 
                where folders.folder_id = :folderId and users.id = :userId
                order by $order
                LIMIT :limit OFFSET :offset
                """.trimIndent(),
            ).bind("userId", userId.value)
            .bind("folderId", folderId.value)
            .bind("limit", limit)
            .bind("offset", offset)
            .mapTo<File>()
            .toList()
    }

    override fun getPathById(
        userId: Id,
        fileId: Id,
    ): String? =
        handle
            .createQuery(
                """
                select path from dbo.Files where file_id = :fileId and user_id = :userId
                """.trimIndent(),
            ).bind("userId", userId.value)
            .bind("fileId", fileId.value)
            .mapTo<String>()
            .singleOrNull()

    override fun deleteFile(
        userId: Id,
        file: File,
        updatedAt: Instant?,
    ) {
        handle
            .createUpdate(
                """
                delete from dbo.Files where file_id = :fileId and user_id = :userId
                """.trimIndent(),
            ).bind("fileId", file.fileId.value)
            .bind("userId", userId.value)
            .execute()

        val folderId = file.folderInfo
        if (updatedAt != null && folderId != null) {
            handle
                .createUpdate(
                    """
                    update dbo.Folders
                    set updated_at = :updated_at
                    where folder_id = :folder_id
                    """.trimIndent(),
                ).bind("updated_at", updatedAt.epochSeconds)
                .bind("folder_id", folderId.id.value)
                .execute()
        }
    }

    override fun deleteFolder(
        userId: Id,
        folder: Folder,
    ) {
        handle
            .createUpdate(
                """
                delete from dbo.Folders where folder_id = :folderId and user_id = :userId
                """.trimIndent(),
            ).bind("folderId", folder.folderId.value)
            .bind("userId", userId.value)
            .execute()
    }

    override fun createFolder(
        userId: Id,
        folderName: String,
        parentFolderId: Id?,
        path: String,
        folderType: FolderType,
        createdAt: Instant,
    ): Id {
        val id =
            handle
                .createUpdate(
                    """
                    insert into dbo.Folders (user_id, parent_folder_id, folder_name, size, 
                    number_files, created_at, updated_at, path, type) values (:user_id, :parent_folder_id, :folder_name, 0, 0, :created_at,:created_at, :path, :type)
                    """.trimIndent(),
                ).bind("user_id", userId.value)
                .bind("parent_folder_id", parentFolderId?.value)
                .bind("folder_name", folderName)
                .bind("created_at", createdAt.epochSeconds)
                .bind("path", path)
                .bind("type", folderType.ordinal)
                .executeAndReturnGeneratedKeys()
                .mapTo<Int>()
                .one()
        return Id(id)
    }

    override fun updateFilePath(
        userId: Id,
        file: File,
        newPath: String,
        updateAt: Instant,
        folderId: Id?,
    ) {
        handle
            .createUpdate(
                """
                update dbo.Files
                set path =:newPath, folder_id = :folderId 
                where file_id = :fileId and user_id = :userId
                """.trimIndent(),
            ).bind("newPath", newPath)
            .bind("folderId", folderId?.value)
            .bind("fileId", file.fileId.value)
            .bind("userId", userId.value)
            .execute()

        if (folderId != null) {
            handle
                .createUpdate(
                    """
                    update dbo.Folders
                    set updated_at = :updatedAt
                    where folder_id = :folderId
                    """.trimIndent(),
                ).bind("updatedAt", updateAt.epochSeconds)
                .bind("folderId", folderId.value)
                .execute()
        }
        val fileFolderId = file.folderInfo
        if (fileFolderId != null) {
            handle
                .createUpdate(
                    """
                    update dbo.Folders
                    set updated_at = :updatedAt
                    where folder_id = :folderId
                    """.trimIndent(),
                ).bind("updatedAt", updateAt.epochSeconds)
                .bind("folderId", fileFolderId.id.value)
                .execute()
        }
    }

    override fun isMemberOfFolder(
        userId: Id,
        folderId: Id,
    ): Boolean =
        handle
            .createQuery(
                """
                select count(*) from dbo.Join_Folders where user_id = :userId and folder_id = :folderId
                """.trimIndent(),
            ).bind("userId", userId.value)
            .bind("folderId", folderId.value)
            .mapTo<Int>()
            .single() == 1

    override fun createInviteFolder(
        inviterId: Id,
        guestId: Id,
        folderId: Id,
    ): Id {
        val inviteId =
            handle
                .createUpdate(
                    """
                    insert into dbo.Invited_Folders(inviter_id, guest_id, folder_id, status) 
                    values (:inviterId, :guestId, :folderId, :status)
                    """.trimIndent(),
                ).bind("inviterId", inviterId.value)
                .bind("guestId", guestId.value)
                .bind("folderId", folderId.value)
                .bind("status", InviteStatus.PENDING.ordinal)
                .executeAndReturnGeneratedKeys()
                .mapTo<Int>()
                .one()

        logger.info("Invite created for user {} to folder {}", guestId, folderId)

        return Id(inviteId)
    }

    override fun isInviteCodeValid(
        userId: Id,
        folderId: Id,
        inviteId: Id,
    ): Boolean =
        handle
            .createQuery(
                """
                select count(*) from dbo.Invited_Folders 
                where guest_id = :userId and folder_id = :folderId and invite_id = :inviteId
                and status = :status
                """.trimIndent(),
            ).bind("userId", userId.value)
            .bind("folderId", folderId.value)
            .bind("inviteId", inviteId.value)
            .bind("status", InviteStatus.PENDING.ordinal)
            .mapTo<Int>()
            .single() == 1

    override fun isOwnerOfFolder(
        userId: Id,
        folderId: Id,
    ): Boolean =
        handle
            .createQuery(
                "select 1 from dbo.Folders where folder_id = :folderId and user_id = :userId",
            ).bind("folderId", folderId.value)
            .bind("userId", userId.value)
            .mapTo<Int>()
            .findOne()
            .isPresent

    override fun folderInviteUpdated(
        guestId: Id,
        inviteId: Id,
        inviteStatus: InviteStatus,
    ) {
        handle
            .createUpdate(
                """
                update dbo.Invited_Folders 
                set status = :status 
                where invite_id = :inviteId and guest_id = :guestId
                """.trimIndent(),
            ).bind("inviteId", inviteId.value)
            .bind("guestId", guestId.value)
            .bind("status", inviteStatus.ordinal)
            .execute()
    }

    override fun getReceivedFolderInvites(
        userId: Id,
        limit: Int,
        offset: Int,
        sort: String,
    ): List<FolderPrivateInvite> {
        val order = orderBy(sort, "folder_name")

        return handle
            .createQuery(
                """
                select invite.invite_id, invite.folder_id, folder.folder_name, users.id as user_id, users.username, users.email, invite.status
                from dbo.Invited_Folders invite 
                inner join dbo.Folders folder on invite.folder_id = folder.folder_id 
                inner join dbo.Users users on invite.inviter_id = users.id 
                where invite.guest_id = :userId
                order by $order
                LIMIT :limit OFFSET :offset
                """.trimIndent(),
            ).bind("userId", userId.value)
            .bind("limit", limit)
            .bind("offset", offset)
            .mapTo<FolderPrivateInvite>()
            .toList()
    }

    override fun getSentFolderInvites(
        userId: Id,
        limit: Int,
        offset: Int,
        sort: String,
    ): List<FolderPrivateInvite> {
        val order = orderBy(sort, "folder_name")

        return handle
            .createQuery(
                """
                select invite.invite_id, invite.folder_id, folder.folder_name, users.id as user_id, users.username, users.email, invite.status
                from dbo.Invited_Folders invite 
                inner join dbo.Folders folder on invite.folder_id = folder.folder_id 
                inner join dbo.Users users on invite.guest_id = users.id 
                where invite.inviter_id = :userId
                order by $order
                LIMIT :limit OFFSET :offset
                """.trimIndent(),
            ).bind("userId", userId.value)
            .bind("limit", limit)
            .bind("offset", offset)
            .mapTo<FolderPrivateInvite>()
            .toList()
    }

    override fun countReceivedFolderInvites(userId: Id): Long =
        handle
            .createQuery(
                """
                select count(*) from dbo.Invited_Folders where guest_id = :userId
                """.trimIndent(),
            ).bind("userId", userId.value)
            .mapTo<Long>()
            .one()

    override fun countSentFolderInvites(userId: Id): Long =
        handle
            .createQuery(
                """
                select count(*) from dbo.Invited_Folders where inviter_id = :userId
                """.trimIndent(),
            ).bind("userId", userId.value)
            .mapTo<Long>()
            .one()

    override fun leaveFolder(
        userId: Id,
        folderId: Id,
    ): Boolean {
        val isOwner = isOwnerOfFolder(userId, folderId)

        if (isOwner) {
            logger.info("User {} is trying to leave folder {} but is the owner", userId, folderId)
            return true
        }

        return handle
            .createUpdate(
                """
                delete from dbo.Join_Folders where user_id = :userId and folder_id = :folderId
                """.trimIndent(),
            ).bind("userId", userId.value)
            .bind("folderId", folderId.value)
            .execute() > 0
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JdbiStorageRepository::class.java)

        fun orderBy(
            sort: String,
            attributeName: String,
        ): String =
            when (sort) {
                "name_desc" -> "$attributeName desc"
                "name_asc" -> "$attributeName asc"
                "created_desc" -> "created_at desc"
                "created_asc" -> "created_at asc"
                "updated_desc" -> "updated_at desc"
                "updated_asc" -> "updated_at asc"
                "size_desc" -> "size desc"
                "size_asc" -> "size asc"
                else -> "created_at"
            }
    }
}
