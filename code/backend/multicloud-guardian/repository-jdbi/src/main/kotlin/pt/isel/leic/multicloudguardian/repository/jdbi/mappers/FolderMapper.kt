package pt.isel.leic.multicloudguardian.repository.jdbi.mappers

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.leic.multicloudguardian.domain.folder.Folder
import pt.isel.leic.multicloudguardian.domain.user.UserInfo
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.Id
import java.sql.ResultSet
import java.sql.SQLException

class FolderMapper : RowMapper<Folder> {
    @Throws(SQLException::class)
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): Folder =
        Folder(
            folderId = Id(rs.getInt("folder_id")),
            user =
                UserInfo(
                    id = Id(rs.getInt("user_id")),
                    username = Username(rs.getString("username")),
                    email = Email(rs.getString("email")),
                ),
            parentFolderId = rs.getObject("parent_folder_id")?.let { Id(it as Int) },
            folderName = rs.getString("folder_name"),
            size = rs.getLong("size"),
            numberFiles = rs.getInt("number_files"),
            path = rs.getString("path"),
            createdAt = InstantMapper().map(rs, 7, ctx),
            updatedAt = InstantMapper().map(rs, 8, ctx),
        )
}
