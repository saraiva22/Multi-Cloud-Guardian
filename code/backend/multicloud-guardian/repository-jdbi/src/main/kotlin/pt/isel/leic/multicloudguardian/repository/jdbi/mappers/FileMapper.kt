package pt.isel.leic.multicloudguardian.repository.jdbi.mappers

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.leic.multicloudguardian.domain.file.File
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
    ): File =
        File(
            fileId = Id(rs.getInt("file_id")),
            user =
                UserInfo(
                    id = Id(rs.getInt("user_id")),
                    username = Username(rs.getString("username")),
                    email = Email(rs.getString("email")),
                ),
            folderId = rs.getObject("folder_id")?.let { Id(it as Int) },
            fileName = rs.getString("file_name"),
            path = rs.getString("path"),
            size = rs.getLong("size"),
            encryption = rs.getBoolean("encryption"),
            encryptionKey = rs.getString("encryption_key"),
        )
}
