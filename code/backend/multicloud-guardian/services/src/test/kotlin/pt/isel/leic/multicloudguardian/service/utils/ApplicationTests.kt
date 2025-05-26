package pt.isel.leic.multicloudguardian.service.utils

import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import pt.isel.leic.multicloudguardian.repository.jdbi.configureWithAppRequirements
import kotlin.math.abs
import kotlin.random.Random

open class ApplicationTests {
    companion object {
        fun newTestUserName() = "user-${abs(Random.nextLong())}"

        fun newTestEmail(username: String) = "$username@gmail.com"

        fun newTestPassword() = "Test_${abs(Random.nextInt())}"

        fun newTestSalt() = "salt-${abs(Random.nextLong())}"

        fun newTestIteration() = abs(Random.nextInt(15000, 20000))

        val jdbi =
            Jdbi
                .create(
                    PGSimpleDataSource().apply {
                        setURL(DB_URL)
                    },
                ).configureWithAppRequirements()

        private const val DB_URL = "jdbc:postgresql://localhost:5432/db?user=dbuser&password=changeit"
    }
}
