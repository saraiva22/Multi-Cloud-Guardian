package pt.isel.leic.multicloudguardian.service

import pt.isel.leic.multicloudguardian.domain.file.FileCreate
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.service.storage.GetFileByIdError
import pt.isel.leic.multicloudguardian.service.storage.GetFileInFolderError
import pt.isel.leic.multicloudguardian.service.utils.ServiceTests
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class StorageServiceTests : ServiceTests() {
    @Test
    fun `can upload File to Storage`() {
        // Arrange: create a storage service instance
        val storageService = createStorageService()

        // Act: upload a file for two different users (Azure and BackBlaze)
        val blobName = "test-file.txt"
        val fileContent = "This is a test file content."
        val contentType = "text/plain"
        val size = fileContent.length.toLong()
        val encryption = false
        val file =
            FileCreate(
                blobName = blobName,
                fileContent = fileContent.toByteArray(),
                contentType = contentType,
                size = size,
                encryption = false,
                encryptedKey = null,
            )

        val createFileAzure = storageService.uploadFile(file, encryption, testUser)
        val createFileBackBlaze = storageService.uploadFile(file, encryption, testUser2)

        // Assert: file creation should succeed for both users
        when (createFileAzure) {
            is Success -> assertTrue(createFileAzure.value.value > 0)
            is Failure -> fail("Unexpected $createFileAzure")
        }

        when (createFileBackBlaze) {
            is Success -> assertTrue(createFileBackBlaze.value.value > 0)
            is Failure -> fail("Unexpected $createFileBackBlaze")
        }

        // Act: retrieve the uploaded files by ID
        val getFileResultAzure = storageService.getFileById(testUser, createFileAzure.value)
        val getFileResultBackBlaze = storageService.getFileById(testUser2, createFileBackBlaze.value)

        // Assert: file retrieval should return correct file details
        when (getFileResultAzure) {
            is Failure -> fail("Unexpected $getFileResultAzure")
            is Success -> {
                assertEquals(createFileAzure.value.value, getFileResultAzure.value.fileId.value)
                assertEquals(file.blobName, getFileResultAzure.value.fileName)
                assertEquals(file.contentType, getFileResultAzure.value.contentType)
                assertEquals(file.encryption, getFileResultAzure.value.encryption)
                assertEquals(size, getFileResultAzure.value.size)
            }
        }

        when (getFileResultBackBlaze) {
            is Failure -> fail("Unexpected $getFileResultBackBlaze")
            is Success -> {
                assertEquals(createFileBackBlaze.value.value, getFileResultBackBlaze.value.fileId.value)
                assertEquals(file.blobName, getFileResultBackBlaze.value.fileName)
                assertEquals(file.contentType, getFileResultBackBlaze.value.contentType)
                assertEquals(file.encryption, getFileResultBackBlaze.value.encryption)
                assertEquals(size, getFileResultBackBlaze.value.size)
            }
        }

        // Act: delete the uploaded files
        val deleteFileResultAzure = storageService.deleteFile(testUser, createFileAzure.value)
        val deleteFileResultBackBlaze = storageService.deleteFile(testUser2, createFileBackBlaze.value)

        // Assert: file deletion should succeed for both users
        when (deleteFileResultAzure) {
            is Success -> assertTrue(deleteFileResultAzure.value)
            is Failure -> fail("Unexpected $deleteFileResultAzure")
        }

        when (deleteFileResultBackBlaze) {
            is Success -> assertTrue(deleteFileResultBackBlaze.value)
            is Failure -> fail("Unexpected $deleteFileResultBackBlaze")
        }

        // Act: try to retrieve the deleted files
        val getDeletedFileResultAzure = storageService.getFileById(testUser, createFileAzure.value)
        val getDeletedFileResultBackBlaze = storageService.getFileById(testUser2, createFileBackBlaze.value)

        // Assert: retrieval should fail with FileNotFound error
        when (getDeletedFileResultAzure) {
            is Success -> fail("Expected file to be deleted, but got $getDeletedFileResultAzure")
            is Failure -> assertTrue(getDeletedFileResultAzure.value is GetFileByIdError.FileNotFound)
        }

        when (getDeletedFileResultBackBlaze) {
            is Success -> fail("Expected file to be deleted, but got $getDeletedFileResultBackBlaze")
            is Failure -> assertTrue(getDeletedFileResultBackBlaze.value is GetFileByIdError.FileNotFound)
        }
    }

    @Test
    fun `can upload File in Folder`() {
        // Arrange: create a storage service instance
        val storageService = createStorageService()

        // Act: create a new folder for the user
        val folderName = "ISEL"
        val user = testUser
        val userInfo = testUserInfo
        val createFolder = storageService.createFolder(folderName, user)

        // Assert: folder creation should succeed
        when (createFolder) {
            is Success -> assertTrue(createFolder.value.value > 0)
            is Failure -> fail("Unexpected $createFolder")
        }

        // Act: retrieve the created folder
        val getFolder = storageService.getFolderById(user, createFolder.value)

        // Assert: folder retrieval should return correct details
        when (getFolder) {
            is Failure -> fail("Unexpected $getFolder")
            is Success -> {
                assertEquals(folderName, getFolder.value.folderName)
                assertEquals(userInfo, getFolder.value.user)
                assertEquals(createFolder.value, getFolder.value.folderId)
            }
        }

        // Act: upload a file into the created folder
        val blobName = "test-file.txt"
        val fileContent = "This is a test file content."
        val contentType = "text/plain"
        val size = fileContent.length.toLong()
        val encryption = false
        val file =
            FileCreate(
                blobName = blobName,
                fileContent = fileContent.toByteArray(),
                contentType = contentType,
                size = size,
                encryption = false,
                encryptedKey = null,
            )

        val uploadFileInFolder = storageService.uploadFileInFolder(file, encryption, user, createFolder.value)

        // Assert: file upload in folder should succeed
        when (uploadFileInFolder) {
            is Success -> assertTrue(uploadFileInFolder.value.value > 0)
            is Failure -> fail("Unexpected $uploadFileInFolder")
        }

        // Act: retrieve the uploaded file from the folder
        val getFile = storageService.getFileInFolder(user, getFolder.value.folderId, uploadFileInFolder.value)

        // Assert: file retrieval from folder should return correct details
        when (getFile) {
            is Failure -> fail("Unexpected $getFile")
            is Success -> {
                assertEquals(uploadFileInFolder.value, getFile.value.fileId)
                assertEquals(getFolder.value.folderId, getFile.value.folderId)
                assertEquals(size, getFile.value.size)
                assertEquals(getFolder.value.user, getFile.value.user)
            }
        }

        // Act: delete the file from the folder
        val deleteFile = storageService.deleteFileInFolder(user, getFolder.value.folderId, getFile.value.fileId)

        // Assert: file deletion from folder should succeed
        when (deleteFile) {
            is Success -> assertTrue(deleteFile.value)
            is Failure -> fail("Unexpected $deleteFile")
        }

        // Act: try to retrieve the deleted file from the folder
        val getDeletedFileResult = storageService.getFileInFolder(testUser, getFolder.value.folderId, uploadFileInFolder.value)

        // Assert: retrieval should fail with FileNotFound error
        when (getDeletedFileResult) {
            is Success -> fail("Expected file to be deleted, but got $getDeletedFileResult")
            is Failure -> assertTrue(getDeletedFileResult.value is GetFileInFolderError.FileNotFound)
        }

        // Act: check folder size after file deletion
        val getFileAgain = storageService.getFolderById(user, createFolder.value)

        // Assert: folder size should be zero
        when (getFileAgain) {
            is Success -> assertEquals(0, getFileAgain.value.size)
            is Failure -> fail("Unexpected $getFileAgain")
        }

        // Act: delete the folder
        val deleteFolder = storageService.deleteFolder(user, getFolder.value.folderId)

        // Assert: folder deletion should succeed
        when (deleteFolder) {
            is Success -> assertTrue(deleteFolder.value)
            is Failure -> fail("Unexpected $deleteFile")
        }

        // Act: try to retrieve a file from the deleted folder
        val getDeletedFolderResult = storageService.getFileInFolder(testUser, getFolder.value.folderId, uploadFileInFolder.value)

        // Assert: retrieval should fail with FolderNotFound error
        when (getDeletedFolderResult) {
            is Success -> fail("Expected file to be deleted, but got $getDeletedFolderResult")
            is Failure -> assertEquals(GetFileInFolderError.FolderNotFound, getDeletedFolderResult.value)
        }
    }
}
