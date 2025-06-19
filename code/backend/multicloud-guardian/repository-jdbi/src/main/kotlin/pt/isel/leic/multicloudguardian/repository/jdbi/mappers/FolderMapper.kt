package pt.isel.leic.multicloudguardian.repository.jdbi.mappers

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.leic.multicloudguardian.domain.folder.Folder
import pt.isel.leic.multicloudguardian.domain.folder.FolderInfo
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
    ): Folder {
        val parentFolderId = rs.getObject("parent_id") as? Int

        return Folder(
            folderId = Id(rs.getInt("folder_id")),
            user =
                UserInfo(
                    id = Id(rs.getInt("user_id")),
                    username = Username(rs.getString("username")),
                    email = Email(rs.getString("email")),
                ),
            parentFolderInfo =
                if (parentFolderId == null) {
                    null
                } else {
                    FolderInfo(
                        id = Id(parentFolderId),
                        folderName = rs.getString("parent_folder_name"),
                    )
                },
            folderName = rs.getString("folder_name"),
            size = rs.getLong("size"),
            numberFiles = rs.getInt("number_files"),
            path = rs.getString("path"),
            type = TypeMapper().map(rs, 10, ctx),
            createdAt = InstantMapper().map(rs, 7, ctx),
            updatedAt = InstantMapper().map(rs, 8, ctx),
        )
    }
}
