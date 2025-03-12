package pt.isel.leic.multicloudguardian.repository.jdbi

import pt.isel.leic.multicloudguardian.repository.Transaction
import org.jdbi.v3.core.Handle

class JdbiTransaction(private val handle: Handle): Transaction {
}