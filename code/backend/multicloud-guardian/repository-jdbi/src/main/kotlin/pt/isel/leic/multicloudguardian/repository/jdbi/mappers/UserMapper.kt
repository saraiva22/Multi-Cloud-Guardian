package pt.isel.leic.multicloudguardian.repository.jdbi.mappers

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.Id
import java.sql.ResultSet
import java.sql.SQLException

class UserMapper : RowMapper<User> {
    @Throws(SQLException::class)
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): User =
        User(
            id = Id(rs.getInt("id")),
            username = Username(rs.getString("username")),
            email = Email(rs.getString("email")),
            passwordValidation = PasswordValidationInfoMapper().map(rs, 4, ctx),
        )
}
