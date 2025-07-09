package pt.isel.leic.multicloudguardian.repository.jdbi

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.junit.jupiter.api.Assertions
import org.postgresql.ds.PGSimpleDataSource
import pt.isel.leic.multicloudguardian.ApplicationTests.Companion.fileCreation
import pt.isel.leic.multicloudguardian.ApplicationTests.Companion.newTestEmail
import pt.isel.leic.multicloudguardian.ApplicationTests.Companion.newTestIteration
import pt.isel.leic.multicloudguardian.ApplicationTests.Companion.newTestSalt
import pt.isel.leic.multicloudguardian.Environment
import pt.isel.leic.multicloudguardian.TestClock
import pt.isel.leic.multicloudguardian.domain.folder.FolderType
import pt.isel.leic.multicloudguardian.domain.folder.InviteStatus
import pt.isel.leic.multicloudguardian.domain.user.PasswordValidationInfo
import pt.isel.leic.multicloudguardian.domain.user.User
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.Id
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class JdbiStorageRepositoryTests {
    @Test
    fun `should store file in folder and retrieve it with all properties correctly`() {
        runWithHandle { handle ->
            // given: a storage repository and file data
            val repo = JdbiStorageRepository(handle)
            val userId = Id(1)
            val folderId = null
            val fileCreate = fileCreation()
            val path = "/folder/testfile.txt"
            val url = "http://example.com/testfile.txt"
            val clock = TestClock()
            val createdAt = Instant.fromEpochSeconds(clock.now().epochSeconds)

            // when: storing the file
            val fileId =
                repo.storeFile(
                    file = fileCreate,
                    path = path,
                    url = url,
                    userId = userId,
                    folderId = folderId,
                    fileFakeName = fileCreate.blobName,
                    encryption = false,
                    createdAt = createdAt,
                    updatedAt = null,
                )

            // then: the file can be retrieved and all properties match
            val file = repo.getFileById(fileId)
            assertNotNull(file)
            assertEquals(fileCreate.blobName, file.fileName)
            assertEquals(fileCreate.size, file.size)
            assertEquals(fileCreate.contentType, file.contentType)
            assertEquals(userId, file.user.id)
            assertEquals(folderId, file.folderInfo?.id)
            assertEquals(path, file.path)
            assertEquals(false, file.encryption)
            assertEquals(createdAt.epochSeconds, file.createdAt.epochSeconds)

            // Cleanup
            clearData(jdbi, "dbo.Files", "file_id", fileId.value)
        }
    }

    @Test
    fun `should create folder and retrieve it with all properties correctly`() {
        runWithHandle { handle ->
            // given: a storage repository and folder data
            val repo = JdbiStorageRepository(handle)
            val userId = Id(1)
            val folderName = "TestFolder"
            val parentFolderId: Id? = null
            val path = "/TestFolder"
            val createdAt = Clock.System.now()
            val folderType = FolderType.PRIVATE

            // when: creating the folder
            val folderId = repo.createFolder(userId, folderName, parentFolderId, path, folderType, createdAt)
            assertNotNull(folderId)

            // then: the folder can be retrieved and all properties match
            val folderMembers = repo.getFolderById(folderId, false)
            assertNotNull(folderMembers)
            assertEquals(folderName, folderMembers.folder.folderName)
            assertEquals(userId, folderMembers.folder.user.id)
            assertEquals(parentFolderId, folderMembers.folder.parentFolderInfo?.id)
            assertEquals(path, folderMembers.folder.path)
            assertEquals(createdAt.epochSeconds, folderMembers.folder.createdAt.epochSeconds)

            // Cleanup
            clearData(jdbi, "dbo.Folders", "folder_id", folderId.value)
        }
    }

    @Test
    fun `should return empty list when getting file names in newly created empty folder`() {
        runWithHandle { handle ->
            val repo = JdbiStorageRepository(handle)
            val userId = Id(1)
            val folderName = "EmptyFolder"
            val parentFolderId: Id? = null
            val path = "/EmptyFolder"
            val createdAt = Clock.System.now()
            val folderType = FolderType.PRIVATE

            // Create the folder
            val folderId = repo.createFolder(userId, folderName, parentFolderId, path, folderType, createdAt)

            // When: getting file names in the empty folder
            val fileNames = repo.getFileNamesInFolder(userId, folderId)
            assertEquals(emptyList(), fileNames)

            // Cleanup
            clearData(jdbi, "dbo.Folders", "folder_id", folderId.value)
        }
    }

    @Test
    fun `should confirm file name exists in folder after storing file`() {
        runWithHandle { handle ->
            val repo = JdbiStorageRepository(handle)
            val userId = Id(1)
            val folderId = null
            val fileCreate = fileCreation()
            val path = "/exists.txt"
            val url = "http://example.com/exists.txt"
            val createdAt = Clock.System.now()
            val fileId = repo.storeFile(fileCreate, path, url, userId, folderId, fileCreate.blobName, false, createdAt, null)
            assertEquals(true, repo.isFileNameInFolder(userId, folderId, fileCreate.blobName))
            clearData(jdbi, "dbo.Files", "file_id", fileId.value)
        }
    }

    @Test
    fun `should return null when getting file by non-existent id`() {
        runWithHandle { handle ->
            val repo = JdbiStorageRepository(handle)
            val userId = Id(1)
            val file = repo.getFileById(Id(99999))
            assertEquals(null, file)
        }
    }

    @Test
    fun `should return null when getting file in folder with wrong folder id`() {
        runWithHandle { handle ->
            val repo = JdbiStorageRepository(handle)
            val userId = Id(1)
            val folderId = null
            val fileCreate = fileCreation()
            val path = "/wrongfolder.txt"
            val url = "http://example.com/wrongfolder.txt"
            val createdAt = Clock.System.now()
            val fileId = repo.storeFile(fileCreate, path, url, userId, folderId, fileCreate.blobName, false, createdAt, null)
            val file = repo.getFileInFolder(Id(99999), fileId)
            assertEquals(null, file)
            clearData(jdbi, "dbo.Files", "file_id", fileId.value)
        }
    }

    @Test
    fun `should return null when getting folder by non-existent name`() {
        runWithHandle { handle ->
            val repo = JdbiStorageRepository(handle)
            val userId = Id(1)
            val folder = repo.getFolderByName(null, "nope")
            assertEquals(null, folder)
        }
    }

    @Test
    fun `should confirm folder name exists after creation`() {
        runWithHandle { handle ->
            val repo = JdbiStorageRepository(handle)
            val userId = Id(1)
            val folderName = "ExistsFolder"
            val path = "/ExistsFolder"
            val createdAt = Clock.System.now()
            val folderType = FolderType.PRIVATE
            val folderId = repo.createFolder(userId, folderName, null, path, folderType, createdAt)
            assertEquals(true, repo.isFolderNameExists(userId, null, folderName))
            clearData(jdbi, "dbo.Folders", "folder_id", folderId.value)
        }
    }

    @Test
    fun `should return null when getting folder by non-existent id`() {
        runWithHandle { handle ->
            val repo = JdbiStorageRepository(handle)
            val userId = Id(1)
            val folder = repo.getFolderById(Id(99999), false)
            assertEquals(null, folder)
        }
    }

    @Test
    fun `should return empty list when getting files with no files stored`() {
        runWithHandle { handle ->
            val repo = JdbiStorageRepository(handle)
            val userId = Id(1)
            val files = repo.getFiles(userId, 10, 0, "created_asc")
            assertEquals(emptyList(), files)
        }
    }

    @Test
    fun `should return zero when counting files and folders for user with none`() {
        runWithHandle { handle ->
            val repo = JdbiStorageRepository(handle)
            val userId = Id(1)
            assertEquals(0, repo.countFiles(userId))
            assertEquals(0, repo.countFolder(userId))
        }
    }

    @Test
    fun `should return empty list when getting folders for user with none`() {
        runWithHandle { handle ->
            val repo = JdbiStorageRepository(handle)
            val userId = Id(1)
            val folders = repo.getFolders(userId, 10, 0, "created_asc")
            assertEquals(emptyList(), folders)
        }
    }

    @Test
    fun `should return empty list and zero when getting folders in folder with none`() {
        runWithHandle { handle ->
            val repo = JdbiStorageRepository(handle)
            val userId = Id(1)
            val folderId = Id(99999)
            val (folders, total) = repo.getFoldersInFolder(userId, folderId, 10, 0, "created_asc")
            assertEquals(emptyList(), folders)
            assertEquals(0, total)
        }
    }

    @Test
    fun `should return empty list when getting files in folder with none`() {
        runWithHandle { handle ->
            val repo = JdbiStorageRepository(handle)
            val userId = Id(1)
            val folderId = Id(99999)
            val files = repo.getFilesInFolder(folderId, 10, 0, "created_asc")
            assertEquals(emptyList(), files)
        }
    }

    @Test
    fun `should return null when getting path by non-existent file id`() {
        runWithHandle { handle ->
            val repo = JdbiStorageRepository(handle)
            val userId = Id(1)
            val path = repo.getPathById(userId, Id(99999))
            assertEquals(null, path)
        }
    }

    @Test
    fun `should delete file and folder successfully`() {
        runWithHandle { handle ->
            val repo = JdbiStorageRepository(handle)
            val userId = Id(1)
            val folderName = "DeleteFolder"
            val path = "/DeleteFolder"
            val createdAt = Clock.System.now()
            val folderType = FolderType.PRIVATE
            val folderId = repo.createFolder(userId, folderName, null, path, folderType, createdAt)
            val fileCreate = fileCreation()
            val filePath = "$path/file.txt"
            val url = "http://example.com/file.txt"
            val fileId = repo.storeFile(fileCreate, filePath, url, userId, folderId, fileCreate.blobName, false, createdAt, null)
            val file = repo.getFileById(fileId)
            val folderMembers = repo.getFolderById(folderId, false)
            assertNotNull(file)
            assertNotNull(folderMembers)
            repo.deleteFile(userId, file, null)
            repo.deleteFolder(userId, folderMembers.folder)
            assertEquals(null, repo.getFileById(fileId))
            assertEquals(null, repo.getFolderById(folderId, false))
        }
    }

    @Test
    fun `should return files ordered by name ascending`() {
        runWithHandle { handle ->
            val repo = JdbiStorageRepository(handle)
            val userId = Id(1)
            val createdAt = TestClock()

            val fileA = fileCreation()
            val fileB = fileCreation()

            val idA = repo.storeFile(fileA, "/A.txt", "http://a.com", userId, null, fileA.blobName, false, createdAt.now(), null)
            createdAt.advance(1.minutes)
            val idB = repo.storeFile(fileB, "/B.txt", "http://b.com", userId, null, fileB.blobName, false, createdAt.now(), null)

            val files = repo.getFiles(userId, 10, 0, "")
            createdAt.advance(1.minutes)
            assertEquals(listOf(fileA.blobName, fileB.blobName), files.map { it.fileName })

            clearData(jdbi, "dbo.Files", "file_id", idA.value)
            clearData(jdbi, "dbo.Files", "file_id", idB.value)
        }
    }

    @Test
    fun `should delete files inside folder when folder is deleted`() {
        runWithHandle { handle ->
            val repo = JdbiStorageRepository(handle)
            val userId = Id(1)
            val createdAt = Clock.System.now()
            val folderType = FolderType.PRIVATE

            val folderId = repo.createFolder(userId, "ToDelete", null, "/ToDelete", folderType, createdAt)
            val file = fileCreation()
            val fileId =
                repo.storeFile(
                    file,
                    "/ToDelete/file.txt",
                    "http://f.com",
                    userId,
                    folderId,
                    file.blobName,
                    false,
                    createdAt,
                    null,
                )
            val folderMembers = repo.getFolderById(folderId, false)
            assertNotNull(folderMembers)
            repo.deleteFolder(userId, folderMembers.folder)
            val fileAfter = repo.getFileById(fileId)
            val folderAfter = repo.getFolderById(folderId, false)

            assertEquals(null, fileAfter)
            assertEquals(null, folderAfter)
        }
    }

    @Test
    fun `should add user to Join_Folders after accepting invite`() {
        runWithHandle { handle ->
            val repo = JdbiStorageRepository(handle)
            val ownerId = Id(1)
            val folderType = FolderType.SHARED
            val createdAt = Clock.System.now()

            // given: a UserRepository
            val repoUser = JdbiUsersRepository(handle)

            // when: storing a user
            val username = Username(newTestUserName())
            val email = Email(newTestEmail(username.value))
            val salt = newTestSalt()
            val iteration = newTestIteration()
            val passwordValidationInfo = PasswordValidationInfo(newTokenValidationData())
            repoUser.storeUser(username, email, salt, iteration, passwordValidationInfo)

            // and: retrieving a user
            val user: User? = repoUser.getUserByUsername(username)

            // then:
            assertNotNull(user)
            Assertions.assertEquals(username, user.username)
            Assertions.assertEquals(passwordValidationInfo, user.passwordValidation)
            assertTrue(user.id.value >= 0)
            // Owner creates a folder
            val folderId = repo.createFolder(ownerId, "SharedFolder", null, "/SharedFolder", folderType, createdAt)

            // Owner invites guest to the folder
            val inviteId = repo.createInviteFolder(ownerId, user.id, folderId)

            // Guest accepts the invite
            repo.folderInviteUpdated(user.id, inviteId, InviteStatus.ACCEPT)

            // Check that guest is now in Join_Folders for this folder
            val isMember = repo.isMemberOfFolder(user.id, folderId)
            assertEquals(true, isMember)

            // Cleanup
            clearData(jdbi, "dbo.Join_Folders", "folder_id", folderId.value)
            clearData(jdbi, "dbo.Folders", "folder_id", folderId.value)
            clearData(jdbi, "dbo.Users", "id", user.id.value)
        }
    }

    companion object {
        private fun runWithHandle(block: (Handle) -> Unit) = jdbi.useTransaction<Exception>(block)

        private fun newTestUserName() = "user-${abs(Random.nextLong())}"

        private fun newTokenValidationData() = "token-${abs(Random.nextLong())}"

        private fun clearData(
            jdbi: Jdbi,
            tableName: String,
            columnName: String,
            userId: Int,
        ) {
            jdbi.useHandle<Exception> { handle ->
                handle.execute("delete from $tableName where $columnName = $userId")
            }
        }

        private val jdbi =
            Jdbi
                .create(
                    PGSimpleDataSource().apply {
                        setURL(Environment.getDbUrl())
                    },
                ).configureWithAppRequirements()
    }
}
