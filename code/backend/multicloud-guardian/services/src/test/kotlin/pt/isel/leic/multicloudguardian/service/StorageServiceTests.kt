package pt.isel.leic.multicloudguardian.service

import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.service.storage.GetFileByIdError
import pt.isel.leic.multicloudguardian.service.storage.GetFileInFolderError
import pt.isel.leic.multicloudguardian.service.storage.GetFoldersInFolderError
import pt.isel.leic.multicloudguardian.service.storage.UploadFileError
import pt.isel.leic.multicloudguardian.service.utils.ServiceTests
import pt.isel.leic.multicloudguardian.service.utils.TestClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.minutes

class StorageServiceTests : ServiceTests() {
    @Test
    fun `can upload File to Storage`() {
        // Arrange: create a storage service instance
        val storageService = createStorageService()

        // Act: upload a file for two different users (Azure and BackBlaze)
        val file = fileCreation()

        val createFileAzure = storageService.uploadFile(file, file.encryption, testUser)
        val createFileBackBlaze = storageService.uploadFile(file, file.encryption, testUser2)

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
                assertEquals(file.size, getFileResultAzure.value.size)
            }
        }

        when (getFileResultBackBlaze) {
            is Failure -> fail("Unexpected $getFileResultBackBlaze")
            is Success -> {
                assertEquals(createFileBackBlaze.value.value, getFileResultBackBlaze.value.fileId.value)
                assertEquals(file.blobName, getFileResultBackBlaze.value.fileName)
                assertEquals(file.contentType, getFileResultBackBlaze.value.contentType)
                assertEquals(file.encryption, getFileResultBackBlaze.value.encryption)
                assertEquals(file.size, getFileResultBackBlaze.value.size)
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
        val file = fileCreation()

        val uploadFileInFolder = storageService.uploadFileInFolder(file, file.encryption, user, createFolder.value)

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
                assertEquals(file.size, getFile.value.size)
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

    @Test
    fun `get file and getFiles`() {
        // Arrange: initialize the storage service
        val storageService = createStorageService()

        // Act: create and upload a single file
        val fileCreation = fileCreation()
        val file = createFile(testUser, fileCreation)

        val getFileResult = storageService.getFileById(testUser, file.fileId)

        // Assert: verify the uploaded file can be retrieved and matches expected properties
        when (getFileResult) {
            is Failure -> fail("Unexpected $getFileResult")
            is Success -> {
                assertEquals(file, getFileResult.value)
                assertEquals(file.fileId, getFileResult.value.fileId)
                assertEquals(file.fileName, getFileResult.value.fileName)
                assertEquals(file.encryption, getFileResult.value.encryption)
                assertEquals(file.contentType, getFileResult.value.contentType)
                assertEquals(file.folderId, getFileResult.value.folderId)
                assertEquals(fileCreation.blobName, getFileResult.value.fileName)
                assertEquals(fileCreation.size, getFileResult.value.size)
                assertEquals(fileCreation.contentType, getFileResult.value.contentType)
                assertEquals(fileCreation.encryption, getFileResult.value.encryption)
            }
        }

        // Act: create and upload two more files
        val fileContent1 = fileCreation()
        val fileContent2 = fileCreation()
        val file1 = createFile(testUser, fileContent1)
        val file2 = createFile(testUser, fileContent2)
        val setLimit = DEFAULT_LIMIT
        val setPage = DEFAULT_PAGE
        val setSort = DEFAULT_SORT

        // Act: retrieve all files for the user
        val getFiles = storageService.getFiles(testUser, setLimit, setPage, setSort)

        // Assert: verify all three files are present and in expected order
        assertTrue(getFiles.content.isNotEmpty())
        assertEquals(3, getFiles.content.size)
        assertTrue(getFiles.content.any { it.fileId == file1.fileId })
        assertTrue(getFiles.content.any { it.fileId == file2.fileId })
        assertTrue(getFiles.content.any { it.fileName == fileContent1.blobName })
        assertTrue(getFiles.content.any { it.fileName == fileContent2.blobName })
        assertEquals(file, getFiles.content[0])
        assertEquals(file1, getFiles.content[1])
        assertEquals(file2, getFiles.content[2])
        assertEquals(3, getFiles.totalElements)

        // Act: attempt to upload a file with a duplicate name
        val existFile = storageService.uploadFile(fileContent1, fileContent1.encryption, testUser)

        // Assert: verify error is returned for duplicate file name
        when (existFile) {
            is Success -> fail("Unexpected $existFile")
            is Failure -> assertEquals(UploadFileError.FileNameAlreadyExists, existFile.value)
        }

        // Act: retrieve files with a limit of 2
        val newLimit = 2
        val getFiles1 = storageService.getFiles(testUser, newLimit, setPage, setSort)

        // Assert: verify only two files are returned and in correct order
        assertTrue(getFiles.content.isNotEmpty())
        assertEquals(2, getFiles1.content.size)
        assertEquals(file, getFiles1.content[0])
        assertEquals(file1, getFiles1.content[1])
        assertEquals(3, getFiles1.totalElements)

        // Act: delete all files
        storageService.deleteFile(testUser, file.fileId)
        storageService.deleteFile(testUser, file1.fileId)
        storageService.deleteFile(testUser, file2.fileId)

        // Act: retrieve files after deletion
        val getFilesResult = storageService.getFiles(testUser, setLimit, setPage, setSort)

        // Assert: verify no files remain
        assertTrue(getFilesResult.content.isEmpty())
    }

    @Test
    fun `folder and subfolder operations update metadata and list correctly`() {
        // Arrange: initialize test clock and storage service
        val clock = TestClock()
        val storageService = createStorageService(clock)
        val parentFolderName = "ParentFolder"
        val subFolder1Name = "SubFolder1"
        val subFolder2Name = "SubFolder2"
        val user = testUser
        val setLimit = DEFAULT_LIMIT
        val setPage = DEFAULT_PAGE
        val setSort = DEFAULT_SORT

        // Act: create a parent folder for the user
        val createParentFolder = storageService.createFolder(parentFolderName, user)

        // Assert: parent folder is created with a valid ID
        when (createParentFolder) {
            is Success -> assertTrue(createParentFolder.value.value > 0)
            is Failure -> fail("Unexpected $createParentFolder")
        }

        val parentFolderId = createParentFolder.value

        // Act: upload a file to the parent folder after advancing the clock
        val file = fileCreation()
        clock.advance(1.minutes)
        val uploadFile = storageService.uploadFileInFolder(file, file.encryption, user, parentFolderId)

        // Assert: file upload to parent folder is successful
        when (uploadFile) {
            is Success -> assertTrue(uploadFile.value.value > 0)
            is Failure -> fail("Unexpected $uploadFile")
        }

        val fileId = uploadFile.value

        // Act: retrieve parent folder and uploaded file
        val parentFolder = storageService.getFolderById(user, parentFolderId)
        val fileResult = storageService.getFileInFolder(user, parentFolderId, fileId)

        // Assert: uploaded file is present in the parent folder with correct metadata
        when (fileResult) {
            is Failure -> fail("Unexpected $fileResult")
            is Success -> {
                assertEquals(parentFolderId, fileResult.value.folderId)
                assertEquals(file.blobName, fileResult.value.fileName)
            }
        }

        // Assert: parent folder metadata is updated (number of files and updatedAt)
        when (parentFolder) {
            is Failure -> fail("Unexpected $parentFolder")
            is Success -> {
                assertEquals(1, parentFolder.value.numberFiles)
                assertTrue(parentFolder.value.updatedAt.epochSeconds > parentFolder.value.createdAt.epochSeconds)
            }
        }

        // Act: create two subfolders inside the parent folder
        val createSubFolder1 = storageService.createFolderInFolder(subFolder1Name, user, parentFolderId)

        when (createSubFolder1) {
            is Success -> assertTrue(createSubFolder1.value.value > 0)
            is Failure -> fail("Unexpected $createSubFolder1")
        }
        val subFolder1Id = createSubFolder1.value

        val createSubFolder2 = storageService.createFolderInFolder(subFolder2Name, user, parentFolderId)

        when (createSubFolder2) {
            is Success -> assertTrue(createSubFolder2.value.value > 0)
            is Failure -> fail("Unexpected $createSubFolder2")
        }

        val subFolder2Id = createSubFolder2.value

        // Act: list subfolders in the parent folder
        val subFolders = storageService.getFoldersInFolder(user, parentFolderId, setLimit, setPage, setSort)

        // Assert: both subfolders are listed with correct names and IDs
        when (subFolders) {
            is Failure -> fail("Unexpected $subFolders")
            is Success -> {
                assertTrue(subFolders.value.content.any { it.folderId.value == subFolder2Id.value && it.folderName == subFolder2Name })
                assertTrue(subFolders.value.content.any { it.folderId.value == subFolder2Id.value && it.folderName == subFolder2Name })
                assertEquals(2, subFolders.value.content.size)
                assertEquals(true, subFolders.value.first)
                assertEquals(2, subFolders.value.totalElements)
            }
        }

        // Act: upload a file to the first subfolder
        val subFile = fileCreation()
        val uploadSubFile = storageService.uploadFileInFolder(subFile, subFile.encryption, user, subFolder1Id)

        // Assert: file upload to subfolder is successful
        when (uploadSubFile) {
            is Success -> assertTrue(uploadSubFile.value.value > 0)
            is Failure -> fail("Unexpected $uploadSubFile")
        }
        val subFileId = uploadSubFile.value

        // Act: retrieve the file from the subfolder
        val getSubFile = storageService.getFileInFolder(user, subFolder1Id, subFileId)

        // Assert: file is present in the subfolder with correct ID
        when (getSubFile) {
            is Failure -> fail("Unexpected $getSubFile")
            is Success -> {
                assertEquals(subFileId, getSubFile.value.fileId)
            }
        }

        // Act: delete all files and the parent folder
        storageService.deleteFile(testUser, fileId)
        storageService.deleteFile(testUser, subFileId)
        storageService.deleteFolder(user, parentFolderId)

        // Act: attempt to list subfolders in the deleted parent folder
        val getSubFolderError = storageService.getFoldersInFolder(user, parentFolderId, setLimit, setPage, setSort)

        // Assert: listing subfolders fails with FolderNotFound error
        when (getSubFolderError) {
            is Success -> {
                fail("Unexpected $getSubFolderError")
            }
            is Failure -> assertEquals(GetFoldersInFolderError.FolderNotFound, getSubFolderError.value)
        }
    }
}
