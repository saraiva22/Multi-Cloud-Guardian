package pt.isel.leic.multicloudguardian

import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import pt.isel.leic.multicloudguardian.domain.file.FileCreate
import pt.isel.leic.multicloudguardian.repository.jdbi.configureWithAppRequirements
import java.util.Base64
import kotlin.math.abs
import kotlin.random.Random

open class ApplicationTests {
    companion object {
        fun newTestUserName() = "user-${abs(Random.nextLong())}"

        fun newTestEmail(username: String) = "$username@gmail.com"

        fun newTestPassword() = "Test_${abs(Random.nextInt())}"

        fun newTestSalt() = "salt-${abs(Random.nextLong())}"

        fun newTestIteration() = abs(Random.nextInt(15000, 20000))

        fun fileCreation(encryption: Boolean = false): FileCreate {
            val randomNumber = Random.nextInt(1, 1_000_000)
            val blobName = "test-file-$randomNumber.txt"
            val fileContent = "This is a test file content for $blobName ${abs(Random.nextLong())}"
            val contentType = "text/plain"
            val size = fileContent.toByteArray().size.toLong()

            val encryptedKey =
                if (encryption) {
                    val keyWithIV = ByteArray(32)
                    Random.nextBytes(keyWithIV)
                    Base64.getEncoder().encodeToString(keyWithIV)
                } else {
                    ""
                }

            return FileCreate(
                blobName = blobName,
                fileContent = fileContent.toByteArray(),
                contentType = contentType,
                size = size,
                encryption = encryption,
                encryptedKey = encryptedKey,
            )
        }

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
