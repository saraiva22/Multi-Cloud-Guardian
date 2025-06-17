package pt.isel.leic.multicloudguardian.repository.jdbi.mappers

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.leic.multicloudguardian.domain.folder.FolderPrivateInvite
import pt.isel.leic.multicloudguardian.domain.user.UserInfo
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.Id
import java.sql.ResultSet
import java.sql.SQLException

class FolderPrivateInviteMapper : RowMapper<FolderPrivateInvite> {
    @Throws(SQLException::class)
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): FolderPrivateInvite =
        FolderPrivateInvite(
            inviteId = Id(rs.getInt("invite_id")),
            folderId = Id(rs.getInt("folder_id")),
            folderName = rs.getString("folder_name"),
            user =
                UserInfo(
                    id = Id(rs.getInt("user_id")),
                    username = Username(rs.getString("username")),
                    email = Email(rs.getString("email")),
                ),
            status = StatusMapper().map(rs, 7, ctx),
        )
}
