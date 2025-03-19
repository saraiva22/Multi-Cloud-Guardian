package pt.isel.leic.multicloudguardian.repository

interface Transaction {
    val usersRepository: UsersRepository

    fun rollback()
}
