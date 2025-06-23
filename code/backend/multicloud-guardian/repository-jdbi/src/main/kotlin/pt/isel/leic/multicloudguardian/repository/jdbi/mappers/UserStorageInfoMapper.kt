package pt.isel.leic.multicloudguardian.repository.jdbi.mappers

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.leic.multicloudguardian.domain.preferences.CostType
import pt.isel.leic.multicloudguardian.domain.preferences.LocationType
import pt.isel.leic.multicloudguardian.domain.user.UserStorageInfo
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.Id
import java.sql.ResultSet
import java.sql.SQLException

class UserStorageInfoMapper : RowMapper<UserStorageInfo> {
    @Throws(SQLException::class)
    override fun map(
        rs: ResultSet,
        ctx: StatementContext?,
    ): UserStorageInfo =
        UserStorageInfo(
            id = Id(rs.getInt("id")),
            username = Username(rs.getString("username")),
            email = Email(rs.getString("email")),
            locationType = LocationType.entries[rs.getInt("location")],
            costType = CostType.entries[rs.getInt("performance")],
        )
}
