package pt.isel.leic.multicloudguardian.repository

interface TransactionManager {
    fun <R> run(block: (Transaction) -> R): R
}