package pt.isel.leic.multicloudguardian.repository.jdbi.mappers

import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import java.sql.ResultSet
import java.sql.SQLException
import kotlin.jvm.Throws

class UsernameMapper : ColumnMapper<Username> {
    @Throws(SQLException::class)
    override fun map(
        r: ResultSet,
        columnNumber: Int,
        ctx: StatementContext?,
    ): Username = Username(r.getString(columnNumber))
}
