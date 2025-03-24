package pt.isel.leic.multicloudguardian.repository

interface Transaction {
    val usersRepository: UsersRepository
    val fileRepository: FileRepository

    fun rollback()
}
