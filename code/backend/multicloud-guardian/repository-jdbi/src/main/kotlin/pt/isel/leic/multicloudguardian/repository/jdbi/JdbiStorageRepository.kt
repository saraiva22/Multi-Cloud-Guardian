package pt.isel.leic.multicloudguardian.repository.jdbi

import kotlinx.datetime.Instant
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.slf4j.LoggerFactory
import pt.isel.leic.multicloudguardian.domain.file.File
import pt.isel.leic.multicloudguardian.domain.file.FileCreate
import pt.isel.leic.multicloudguardian.domain.folder.Folder
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

        return Id(fileId)
    }

    override fun getFileNamesInFolder(
        userId: Id,
        folderId: Id?,
    ): List<String> =
        handle
            .createQuery(
                """
                select file_name from dbo.Files where user_id = :user_id and ((:folderId IS NULL AND folder_id IS NULL) 
                OR folder_id = :folderId)
                """.trimIndent(),
            ).bind("user_id", userId.value)
            .bind("folderId", folderId)
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
                select file.*, users.username ,users.email 
                from dbo.Files file inner join dbo.Users on file.user_id = id where file.file_id = :fileId and file.user_id = :userId
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
                select file.*, users.username ,users.email 
                from dbo.Files file inner join dbo.Users on file.user_id = id where file.file_id = :fileId and file.user_id = :userId
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
                select folder.*, users.username ,users.email 
                from dbo.Folders folder inner join dbo.Users on folder.user_id = id where folder.folder_name = :folderName and folder.user_id = :userId
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
                select folder.*, users.username ,users.email 
                from dbo.Folders folder inner join dbo.Users on folder.user_id = id where folder.folder_id = :folderId and folder.user_id = :userId
                """.trimIndent(),
            ).bind("userId", userId.value)
            .bind("folderId", folderId.value)
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
                select file.*, users.username ,users.email 
                from dbo.Files file inner join dbo.Users on file.user_id = id where file.user_id = :userId
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
                select folder.*, users.username ,users.email 
                from dbo.Folders folder inner join dbo.Users on folder.user_id = id where folder.user_id = :userId 
                order by $order
                LIMIT :limit OFFSET :offset
                """.trimIndent(),
            ).bind("userId", userId.value)
            .bind("limit", limit)
            .bind("offset", offset)
            .mapTo<Folder>()
            .toList()
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
                select files.*, users.username ,users.email  from dbo.Files inner join dbo.folders on files.folder_id = folders.folder_id 
                inner join dbo.users on folders.user_id = users.id where folders.folder_id = :folderId and users.id = :userId
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
    ) {
        handle
            .createUpdate(
                """
                delete from dbo.Files where file_id = :fileId and user_id = :userId
                """.trimIndent(),
            ).bind("fileId", file.fileId.value)
            .bind("userId", userId.value)
            .execute()
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
        createdAt: Instant,
    ): Id {
        val id =
            handle
                .createUpdate(
                    """
                    insert into dbo.Folders (user_id, parent_folder_id, folder_name, size, 
                    number_files, created_at, updated_at, path) values (:user_id, :parent_folder_id, :folder_name, 0, 0, :created_at,:created_at, :path)
                    """.trimIndent(),
                ).bind("user_id", userId.value)
                .bind("parent_folder_id", parentFolderId?.value)
                .bind("folder_name", folderName)
                .bind("created_at", createdAt.epochSeconds)
                .bind("path", path)
                .executeAndReturnGeneratedKeys()
                .mapTo<Int>()
                .one()
        return Id(id)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JdbiStorageRepository::class.java)

        fun orderBy(
            sort: String,
            attributeName: String,
        ): String =
            when (sort) {
                "name" -> attributeName
                "created_at" -> "created_at"
                "size" -> "size"
                "last_created" -> "created_at desc"
                else -> "created_at"
            }
    }
}
