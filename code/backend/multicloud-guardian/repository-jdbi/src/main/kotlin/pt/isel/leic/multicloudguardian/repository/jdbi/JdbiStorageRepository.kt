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
        checkSum: Long,
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
                    insert into dbo.Files (user_id, folder_id, file_name,checksum, path, size, encryption) values (:user_id,:folderId, :name, :checksum, :path, :size, :encryption)
                    """.trimIndent(),
                ).bind("user_id", userId.value)
                .bind("folderId", folderId?.value)
                .bind("name", file.blobName)
                .bind("checksum", checkSum)
                .bind("path", path)
                .bind("size", file.size)
                .bind("encryption", encryption)
                .executeAndReturnGeneratedKeys()
                .mapTo<Int>()
                .one()

        logger.info("{} file stored in the database", file.blobName)

        handle
            .createUpdate(
                """
                insert into dbo.Metadata (file_id, content_type, tags, created_at, indexed_at) values (:file_id, :content_type, :tags, :created_at, :indexed_at)
                """.trimIndent(),
            ).bind("file_id", fileId)
            .bind("content_type", file.contentType)
            .bindArray("tags", file.blobName, file.contentType, file.size.toString())
            .bind("created_at", createdAt.epochSeconds)
            .bind("indexed_at", createdAt.epochSeconds)
            .execute()

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
                select file.*  from dbo.Files file inner join dbo.Users on file.user_id = id where file.file_id = :fileId and file.user_id = :userId
                """.trimIndent(),
            ).bind("userId", userId.value)
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
                select folder.* from dbo.Folders folder inner join dbo.Users on folder.user_id = id where folder.folder_name = :folderName and folder.user_id = :userId
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
                select folder.* from dbo.Folders folder inner join dbo.Users on folder.user_id = id where folder.folder_id = :folderId and folder.user_id = :userId
                """.trimIndent(),
            ).bind("userId", userId.value)
            .bind("folderId", folderId.value)
            .mapTo<Folder>()
            .singleOrNull()

    override fun getFiles(userId: Id): List<File> =
        handle
            .createQuery(
                """
                select file.*  from dbo.Files file inner join dbo.Users on file.user_id = id where file.user_id = :userId
                """.trimIndent(),
            ).bind("userId", userId.value)
            .mapTo<File>()
            .toList()

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

    override fun deleteFile(file: File) {
        handle
            .createUpdate(
                """
                delete from dbo.Files where file_id = :fileId
                """.trimIndent(),
            ).bind("fileId", file.fileId.value)
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
    }
}
