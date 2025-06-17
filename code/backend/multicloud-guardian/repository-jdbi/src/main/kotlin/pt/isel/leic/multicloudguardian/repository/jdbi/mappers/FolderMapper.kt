package pt.isel.leic.multicloudguardian.repository.jdbi.mappers

import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.leic.multicloudguardian.domain.folder.Folder
import pt.isel.leic.multicloudguardian.domain.folder.Type
import pt.isel.leic.multicloudguardian.domain.user.UserInfo
import pt.isel.leic.multicloudguardian.domain.utils.Id
import java.sql.ResultSet
import java.sql.SQLException

class FolderMapper(
    private val ownerMapper: RowMapper<UserInfo>,
    private val typeMapper: ColumnMapper<Type>,
) : RowMapper<Folder> {
    @Throws(SQLException::class)
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): Folder =
        Folder(
            folderId = Id(rs.getInt("folder_id")),
            user = ownerMapper.map(rs, ctx),
            parentFolderId = rs.getObject("parent_folder_id")?.let { Id(it as Int) },
            folderName = rs.getString("folder_name"),
            size = rs.getLong("size"),
            numberFiles = rs.getInt("number_files"),
            path = rs.getString("path"),
            type = typeMapper.map(rs, 6, ctx),
            createdAt = InstantMapper().map(rs, 7, ctx),
            updatedAt = InstantMapper().map(rs, 8, ctx),
        )
}
