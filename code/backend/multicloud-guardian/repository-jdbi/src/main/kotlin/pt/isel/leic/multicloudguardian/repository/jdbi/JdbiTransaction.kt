package pt.isel.leic.multicloudguardian.repository.jdbi

import org.jdbi.v3.core.Handle
import pt.isel.leic.multicloudguardian.repository.StorageRepository
import pt.isel.leic.multicloudguardian.repository.Transaction
import pt.isel.leic.multicloudguardian.repository.UsersRepository

class JdbiTransaction(
    private val handle: Handle,
) : Transaction {
    override val usersRepository: UsersRepository = JdbiUsersRepository(handle)
    override val storageRepository: StorageRepository = JdbiStorageRepository(handle)

    override fun rollback() {
        handle.rollback()
    }
}
