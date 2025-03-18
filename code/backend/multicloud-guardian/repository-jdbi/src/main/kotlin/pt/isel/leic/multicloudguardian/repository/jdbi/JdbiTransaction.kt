package pt.isel.leic.multicloudguardian.repository.jdbi

import pt.isel.leic.multicloudguardian.repository.Transaction
import org.jdbi.v3.core.Handle
import pt.isel.leic.multicloudguardian.repository.UsersRepository

class JdbiTransaction(private val handle: Handle) : Transaction {
    override val usersRepository: UsersRepository = JdbiUsersRepository(handle)

    override fun rollback() {
        handle.rollback()
    }
}