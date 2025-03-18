package pt.isel.leic.multicloudguardian.repository.jdbi

import jakarta.inject.Named
import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory
import pt.isel.leic.multicloudguardian.repository.Transaction
import pt.isel.leic.multicloudguardian.repository.TransactionManager

@Named
class JdbiTransactionManager(private val jdbi: Jdbi) : TransactionManager {
    private val logger = LoggerFactory.getLogger(JdbiTransactionManager::class.java)

    override fun <R> run(block: (Transaction) -> R): R {
        return try {
            jdbi.inTransaction<R, Exception> { handle ->
                val transaction = JdbiTransaction(handle)
                block(transaction)
            }
        } catch (e: Exception) {
            logger.error("Transaction failed ", e)
            throw e
        }
    }
}