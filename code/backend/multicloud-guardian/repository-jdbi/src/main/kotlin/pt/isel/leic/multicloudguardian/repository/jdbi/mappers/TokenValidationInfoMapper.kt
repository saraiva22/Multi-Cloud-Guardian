package pt.isel.leic.multicloudguardian.repository.jdbi.mappers

import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.leic.multicloudguardian.domain.token.TokenValidationInfo
import java.sql.ResultSet
import java.sql.SQLException

class TokenValidationInfoMapper : ColumnMapper<pt.isel.leic.multicloudguardian.domain.token.TokenValidationInfo> {
    @Throws(SQLException::class)
    override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext?): pt.isel.leic.multicloudguardian.domain.token.TokenValidationInfo =
        pt.isel.leic.multicloudguardian.domain.token.TokenValidationInfo(r.getString(columnNumber))
}