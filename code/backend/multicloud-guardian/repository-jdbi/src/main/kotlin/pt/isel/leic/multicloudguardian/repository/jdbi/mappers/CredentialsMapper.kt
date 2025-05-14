package pt.isel.leic.multicloudguardian.repository.jdbi.mappers

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.leic.multicloudguardian.domain.credentials.Credentials
import pt.isel.leic.multicloudguardian.domain.user.UserInfo
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.Id
import java.sql.ResultSet
import java.sql.SQLException

class CredentialsMapper : RowMapper<Credentials> {
    @Throws(SQLException::class)
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): Credentials =
        Credentials(
            credentialsId = Id(rs.getInt("credentials_id")),
            user =
                UserInfo(
                    id = Id(rs.getInt("user_id")),
                    username = Username(rs.getString("username")),
                    email = Email(rs.getString("email")),
                ),
            salt = rs.getString("salt_id"),
            iterations = rs.getInt("iterations"),
        )
}
