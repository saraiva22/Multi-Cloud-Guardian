package pt.isel.leic.multicloudguardian.repository.jdbi.mappers

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.leic.multicloudguardian.domain.file.File
import pt.isel.leic.multicloudguardian.domain.folder.FolderInfo
import pt.isel.leic.multicloudguardian.domain.user.UserInfo
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.Id
import java.sql.ResultSet
import java.sql.SQLException

class FileMapper : RowMapper<File> {
    @Throws(SQLException::class)
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): File {
        val folderId = rs.getObject("folder_id") as? Int
        return File(
            fileId = Id(rs.getInt("file_id")),
            user =
                UserInfo(
                    id = Id(rs.getInt("user_id")),
                    username = Username(rs.getString("username")),
                    email = Email(rs.getString("email")),
                ),
            folderInfo =
                if (folderId == null) {
                    null
                } else {
                    FolderInfo(
                        id = Id(rs.getInt("folder_id")),
                        folderName = rs.getString("folder_name"),
                    )
                },
            fileName = rs.getString("file_name"),
            path = rs.getString("path"),
            size = rs.getLong("size"),
            contentType = rs.getString("content_type"),
            createdAt = InstantMapper().map(rs, "created_at", ctx),
            encryption = rs.getBoolean("encryption"),
            encryptionKey = rs.getString("encryption_key"),
        )
    }
}
