package pt.isel.leic.multicloudguardian.repository

interface Transaction {

    fun rollback()
}