package pt.isel.leic.multicloudguardian.repository.jdbi.mappers

import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.leic.multicloudguardian.domain.utils.Id
import java.sql.ResultSet
import java.sql.SQLException

class IdMapper : ColumnMapper<Id> {
    @Throws(SQLException::class)
    override fun map(
        r: ResultSet,
        columnNumber: Int,
        ctx: StatementContext?,
    ): Id = Id(r.getInt(columnNumber))
}
