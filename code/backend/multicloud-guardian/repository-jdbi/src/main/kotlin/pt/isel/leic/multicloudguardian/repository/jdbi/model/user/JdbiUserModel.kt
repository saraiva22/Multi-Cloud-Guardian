package pt.isel.leic.multicloudguardian.repository.jdbi.model.user

import org.jdbi.v3.core.mapper.reflect.ColumnName
import pt.isel.leic.multicloudguardian.domain.components.Id
import pt.isel.leic.multicloudguardian.domain.user.PasswordValidationInfo
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.get
import pt.isel.leic.multicloudguardian.repository.jdbi.model.JdbiModel

class JdbiUserModel(
    val id: Int,
    val username: String,
    val email: String,
    @ColumnName("password_validation")
    val passwordValidationInfo: String
) : JdbiModel<User> {
    override fun toDomainModel(): User {
        return User(
            id = Id(id).get(),
            username = Username(username).get(),
            email = Email(email).get(),
            passwordValidation = PasswordValidationInfo(passwordValidationInfo)

        )
    }
}