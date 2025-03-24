package pt.isel.leic.multicloudguardian.repository.jdbi

import org.jdbi.v3.core.Handle
import pt.isel.leic.multicloudguardian.repository.FileRepository
import pt.isel.leic.multicloudguardian.repository.Transaction
import pt.isel.leic.multicloudguardian.repository.UsersRepository

class JdbiTransaction(
    private val handle: Handle,
) : Transaction {
    override val usersRepository: UsersRepository = JdbiUsersRepository(handle)
    override val fileRepository: FileRepository = JdbiFileRepository(handle)

    override fun rollback() {
        handle.rollback()
    }
}
