package pt.isel.leic.multicloudguardian.user

import kotlinx.datetime.Clock
import org.springframework.stereotype.Service
import pt.isel.leic.multicloudguardian.domain.components.Id
import pt.isel.leic.multicloudguardian.domain.user.UsersDomain
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Password
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.failure
import pt.isel.leic.multicloudguardian.domain.utils.success
import pt.isel.leic.multicloudguardian.repository.TransactionManager

@Service
class UsersService(
    private val transactionManager: TransactionManager,
    private val usersDomain: UsersDomain,
    private val clock: Clock
) {
    fun createUser(username: Username, email: Email, password: Password): UserCreationResult {
        if (!usersDomain.isSafePassword(password)) {
            return failure(UserCreationError.InsecurePassword)
        }

        TODO()
    }
}