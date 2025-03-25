package pt.isel.leic.multicloudguardian.repository.jdbi

import kotlinx.datetime.Instant
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.slf4j.LoggerFactory
import pt.isel.leic.multicloudguardian.domain.file.FileCreate
import pt.isel.leic.multicloudguardian.domain.utils.Id
import pt.isel.leic.multicloudguardian.repository.FileRepository

class JdbiFileRepository(
    private val handle: Handle,
) : FileRepository {
    override fun storeFile(
        file: FileCreate,
        path: String,
        checkSum: Long,
        url: String,
        userId: Id,
        encryption: Boolean,
        createdAt: Instant,
    ): Id {
        val fileId =
            handle
                .createUpdate(
                    """
                    insert into dbo.Files (user_id, name, checksum, path, size, encryption, url) values (:user_id, :name, :checksum, :path, :size, :encryption, :url)
                    """.trimIndent(),
                ).bind("user_id", userId.value)
                .bind("name", file.fileName)
                .bind("checksum", checkSum)
                .bind("path", path)
                .bind("size", file.size)
                .bind("encryption", encryption)
                .bind("url", url)
                .executeAndReturnGeneratedKeys()
                .mapTo<Int>()
                .one()

        logger.info("{} file stored in the database", fileId)

        handle
            .createUpdate(
                """
                insert into dbo.Metadata (file_id, content_type, tags, created_at, indexed_at) values (:file_id, :content_type, :tags, :created_at, :indexed_at)
                """.trimIndent(),
            ).bind("file_id", fileId)
            .bind("content_type", file.contentType)
            .bindArray("tags", file.fileName, file.contentType, file.size.toString())
            .bind("created_at", createdAt.epochSeconds)
            .bind("indexed_at", createdAt.epochSeconds)
            .execute()

        return Id(fileId)
    }

    override fun getFileNames(userId: Id): List<String> =
        handle
            .createQuery(
                """
                select name from dbo.Files where user_id = :user_id
                """.trimIndent(),
            ).bind("user_id", userId.value)
            .mapTo<String>()
            .list()

    companion object {
        private val logger = LoggerFactory.getLogger(JdbiFileRepository::class.java)
    }
}
