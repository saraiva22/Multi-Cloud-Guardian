package pt.isel.leic.multicloudguardian.repository

interface Transaction {
    val usersRepository: UsersRepository
    val storageRepository: StorageRepository

    fun rollback()
}
