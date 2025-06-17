package pt.isel.leic.multicloudguardian.repository.jdbi.mappers

import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.leic.multicloudguardian.domain.folder.Type
import java.sql.ResultSet
import java.sql.SQLException

class TypeMapper : ColumnMapper<Type> {
    @Throws(SQLException::class)
    override fun map(
        rs: ResultSet,
        columnNumber: Int,
        ctx: StatementContext,
    ): Type? = Type.fromInt(rs.getInt(columnNumber))
}
