package pt.isel.leic.multicloudguardian.service

import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.service.storage.CreateTempUrlFileError
import pt.isel.leic.multicloudguardian.service.storage.DownloadFileError
import pt.isel.leic.multicloudguardian.service.storage.GetFileByIdError
import pt.isel.leic.multicloudguardian.service.storage.GetFileInFolderError
import pt.isel.leic.multicloudguardian.service.storage.GetFoldersInFolderError
import pt.isel.leic.multicloudguardian.service.storage.UploadFileError
import pt.isel.leic.multicloudguardian.service.utils.ServiceTests
import pt.isel.leic.multicloudguardian.service.utils.TestClock
import kotlin.test.Test
import kotlin.test.assertContentEquals
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
            is Success -> {
                assertEquals(createFileAzure.value.value, getFileResultAzure.value.fileId.value)
                assertEquals(file.blobName, getFileResultAzure.value.fileName)
                assertEquals(file.contentType, getFileResultAzure.value.contentType)
                assertEquals(file.encryption, getFileResultAzure.value.encryption)
                assertEquals(file.size, getFileResultAzure.value.size)
            }
            is Failure -> fail("Unexpected $getFileResultAzure")
        }

        when (getFileResultBackBlaze) {
            is Success -> {
                assertEquals(createFileBackBlaze.value.value, getFileResultBackBlaze.value.fileId.value)
                assertEquals(file.blobName, getFileResultBackBlaze.value.fileName)
                assertEquals(file.contentType, getFileResultBackBlaze.value.contentType)
                assertEquals(file.encryption, getFileResultBackBlaze.value.encryption)
                assertEquals(file.size, getFileResultBackBlaze.value.size)
            }
            is Failure -> fail("Unexpected $getFileResultBackBlaze")
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
            is Success -> {
                assertEquals(folderName, getFolder.value.folderName)
                assertEquals(userInfo, getFolder.value.user)
                assertEquals(createFolder.value, getFolder.value.folderId)
            }
            is Failure -> fail("Unexpected $getFolder")
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
            is Success -> {
                assertEquals(uploadFileInFolder.value, getFile.value.fileId)
                assertEquals(getFolder.value.folderId, getFile.value.folderId)
                assertEquals(file.size, getFile.value.size)
                assertEquals(getFolder.value.user, getFile.value.user)
            }
            is Failure -> fail("Unexpected $getFile")
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
            is Failure -> fail("Unexpected $getFileResult")
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
            is Success -> {
                assertEquals(parentFolderId, fileResult.value.folderId)
                assertEquals(file.blobName, fileResult.value.fileName)
            }
            is Failure -> fail("Unexpected $fileResult")
        }

        // Assert: parent folder metadata is updated (number of files and updatedAt)
        when (parentFolder) {
            is Success -> {
                assertEquals(1, parentFolder.value.numberFiles)
                assertTrue(parentFolder.value.updatedAt.epochSeconds > parentFolder.value.createdAt.epochSeconds)
            }
            is Failure -> fail("Unexpected $parentFolder")
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
            is Success -> {
                assertTrue(subFolders.value.content.any { it.folderId.value == subFolder2Id.value && it.folderName == subFolder2Name })
                assertTrue(subFolders.value.content.any { it.folderId.value == subFolder2Id.value && it.folderName == subFolder2Name })
                assertEquals(2, subFolders.value.content.size)
                assertEquals(true, subFolders.value.first)
                assertEquals(2, subFolders.value.totalElements)
            }
            is Failure -> fail("Unexpected $subFolders")
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
            is Success -> assertEquals(subFileId, getSubFile.value.fileId)
            is Failure -> fail("Unexpected $getSubFile")
        }

        // Act: delete all files and the parent folder
        storageService.deleteFile(testUser, fileId)
        storageService.deleteFile(testUser, subFileId)
        storageService.deleteFolder(user, parentFolderId)

        // Act: attempt to list subfolders in the deleted parent folder
        val getSubFolderError = storageService.getFoldersInFolder(user, parentFolderId, setLimit, setPage, setSort)

        // Assert: listing subfolders fails with FolderNotFound error
        when (getSubFolderError) {
            is Success -> fail("Unexpected $getSubFolderError")
            is Failure -> assertEquals(GetFoldersInFolderError.FolderNotFound, getSubFolderError.value)
        }
    }

    @Test
    fun `should create file, generate temporary URL, allow download, and handle deletion correctly`() {
        // Arrange: initialize storage service and create a file for the test user
        val storageService = createStorageService()
        val fileContent = fileCreation()
        val file = createFile(testUser, fileContent)
        val minute = 1L

        // / Act: generate a temporary URL for the uploaded file
        val generatedUrl = storageService.generateTemporaryFileUrl(testUser, file.fileId, minute)

        // Assert: temporary URL is generated and file metadata matches
        when (generatedUrl) {
            is Success -> {
                assertEquals(file, generatedUrl.value.first)
                assertTrue(generatedUrl.value.second.isNotEmpty())
            }
            is Failure -> fail("Unexpected $generatedUrl")
        }

        // Act: download the file using the storage service
        val downloadFile = storageService.downloadFile(testUser, file.fileId)

        // Assert: downloaded file matches the uploaded file's content and metadata
        when (downloadFile) {
            is Success -> {
                assertEquals(file.fileName, downloadFile.value.first.fileName)
                assertContentEquals(fileContent.fileContent, downloadFile.value.first.fileContent)
                assertEquals(file.encryption, downloadFile.value.first.encrypted)
                assertEquals(file.contentType, downloadFile.value.first.mimeType)
            }
            is Failure -> fail("Unexpected $downloadFile")
        }

        // Act: delete the file
        storageService.deleteFile(testUser, file.fileId)

        // Act: attempt to download the deleted file
        val downloadFileAgain = storageService.downloadFile(testUser, file.fileId)

        // Assert: downloading a deleted file returns FileNotFound error
        when (downloadFileAgain) {
            is Success -> {
                fail("Unexpected $downloadFileAgain")
            }
            is Failure -> assertEquals(DownloadFileError.FileNotFound, downloadFileAgain.value)
        }

        // Act: attempt to generate a temporary URL for the deleted file
        val generatedUrlAgain = storageService.generateTemporaryFileUrl(testUser, file.fileId, minute)

        // Assert: generating a URL for a deleted file returns FileNotFound error
        when (generatedUrlAgain) {
            is Success -> {
                fail("Unexpected $generatedUrlAgain")
            }
            is Failure -> assertEquals(CreateTempUrlFileError.FileNotFound, generatedUrlAgain.value)
        }
    }

    @Test
    fun `should create folder, upload file into folder, and download file from folder`() {
        // Arrange: initialize storage service and test user
        val storageService = createStorageService()
        val folderName = "TestFolder"
        val user = testUser

        // Act: create a new folder
        val createFolderResult = storageService.createFolder(folderName, user)
        val folderId =
            when (createFolderResult) {
                is Success -> createFolderResult.value
                is Failure -> fail("Unexpected $createFolderResult")
            }

        // Act: create and upload a file into the created folder
        val fileContent = fileCreation()
        val uploadFileResult = storageService.uploadFileInFolder(fileContent, fileContent.encryption, user, folderId)
        val fileId =
            when (uploadFileResult) {
                is Success -> uploadFileResult.value
                is Failure -> fail("Unexpected $uploadFileResult")
            }

        // Act: download the file from the folder
        val downloadFileResult = storageService.downloadFileInFolder(user, folderId, fileId)

        // Assert: downloaded file matches the uploaded file's properties
        when (downloadFileResult) {
            is Success -> {
                assertEquals(fileContent.blobName, downloadFileResult.value.first.fileName)
                assertEquals(fileContent.encryption, downloadFileResult.value.first.encrypted)
                assertEquals(fileContent.contentType, downloadFileResult.value.first.mimeType)
                assertContentEquals(fileContent.fileContent, downloadFileResult.value.first.fileContent)
            }
            is Failure -> fail("Unexpected $downloadFileResult")
        }

        // Cleanup: delete file and folder
        storageService.deleteFileInFolder(user, folderId, fileId)
        storageService.deleteFolder(user, folderId)

        // Act: try to download the deleted file again
        val downloadAfterDelete = storageService.downloadFileInFolder(user, folderId, fileId)

        // Assert: download should fail after deletion
        when (downloadAfterDelete) {
            is Success -> fail("Unexpected $downloadAfterDelete")
            is Failure -> assertEquals(DownloadFileError.ParentFolderNotFound, downloadAfterDelete.value)
        }
    }

    @Test
    fun `should create folder, upload encrypted file, download, delete and fail to generate temp url`() {
        // Arrange: initialize storage service and test user
        val storageService = createStorageService()
        val folderName = "TestFolderEncrypted"
        val user = testUser

        // Act: create a new folder
        val createFolderResult = storageService.createFolder(folderName, user)
        val folderId =
            when (createFolderResult) {
                is Success -> createFolderResult.value
                is Failure -> fail("Unexpected $createFolderResult")
            }

        // Act: create and upload an encrypted file into the created folder
        val fileContent = fileCreation(true)
        val uploadFileResult = storageService.uploadFileInFolder(fileContent, fileContent.encryption, user, folderId)
        val fileId =
            when (uploadFileResult) {
                is Success -> uploadFileResult.value
                is Failure -> fail("Unexpected $uploadFileResult")
            }

        // Act: download the file from the folder
        val downloadFileResult = storageService.downloadFileInFolder(user, folderId, fileId)

        // Assert: downloaded file matches the uploaded file's properties
        when (downloadFileResult) {
            is Success -> {
                assertEquals(fileContent.blobName, downloadFileResult.value.first.fileName)
                assertEquals(true, downloadFileResult.value.first.encrypted)
                assertEquals(fileContent.contentType, downloadFileResult.value.first.mimeType)
                assertContentEquals(fileContent.fileContent, downloadFileResult.value.first.fileContent)
                assertEquals(fileContent.encryptedKey, downloadFileResult.value.second)
            }
            is Failure -> fail("Unexpected $downloadFileResult")
        }

        // Act: try to generate a temporary URL for the deleted file
        val tempUrlResult = storageService.generateTemporaryFileUrl(user, fileId, 1L)

        // Assert: generating a URL for a deleted file should fail
        when (tempUrlResult) {
            is Success -> fail("Unexpected $tempUrlResult")
            is Failure -> assertEquals(CreateTempUrlFileError.EncryptedFile, tempUrlResult.value)
        }

        // Cleanup: delete file and folder
        storageService.deleteFileInFolder(user, folderId, fileId)
        storageService.deleteFolder(user, folderId)
    }

    @Test
    fun `should create folder, upload file, delete file, and fail to get file in folder`() {
        // Arrange: initialize storage service and test user
        val storageService = createStorageService()
        val folderName = "TestFolderForDelete"
        val user = testUser

        // Act: create a new folder
        val createFolderResult = storageService.createFolder(folderName, user)
        val folderId =
            when (createFolderResult) {
                is Success -> createFolderResult.value
                is Failure -> fail("Unexpected $createFolderResult")
            }

        // Act: create and upload a file into the created folder
        val fileContent = fileCreation()
        val uploadFileResult = storageService.uploadFileInFolder(fileContent, fileContent.encryption, user, folderId)
        val fileId =
            when (uploadFileResult) {
                is Success -> uploadFileResult.value
                is Failure -> fail("Unexpected $uploadFileResult")
            }

        // Act: delete the file from the folder
        val deleteFileResult = storageService.deleteFileInFolder(user, folderId, fileId)
        when (deleteFileResult) {
            is Success -> assertTrue(deleteFileResult.value)
            is Failure -> fail("Unexpected $deleteFileResult")
        }

        // Act: try to get the deleted file from the folder
        val getDeletedFileResult = storageService.getFileInFolder(user, folderId, fileId)

        // Assert: retrieval should fail with FileNotFound error
        when (getDeletedFileResult) {
            is Success -> fail("Expected file to be deleted, but got $getDeletedFileResult")
            is Failure -> assertTrue(getDeletedFileResult.value is GetFileInFolderError.FileNotFound)
        }

        // Cleanup: delete the folder
        storageService.deleteFolder(user, folderId)
    }

    @Test
    fun `should create two normal folders and a subfolder, verify count and parent linkage`() {
        // Arrange
        val clock = testClock
        val storageService = createStorageService(clock)
        val user = testUser

        // Act: create two normal folders
        val folderName1 = "Folder1"
        val folderName2 = "Folder2"
        val createFolder1 = storageService.createFolder(folderName1, user)
        clock.advance(1.minutes)
        val createFolder2 = storageService.createFolder(folderName2, user)
        clock.advance(1.minutes)

        val folderId1 =
            when (createFolder1) {
                is Success -> createFolder1.value
                is Failure -> fail("Unexpected $createFolder1")
            }

        val folderId2 =
            when (createFolder2) {
                is Success -> createFolder2.value
                is Failure -> fail("Unexpected $createFolder2")
            }

        // Act: create a subfolder inside folder1
        val subFolderName = "SubFolder"
        clock.advance(1.minutes)
        val createSubFolder = storageService.createFolderInFolder(subFolderName, user, folderId1)

        val subFolderId =
            when (createSubFolder) {
                is Success -> createSubFolder.value
                is Failure -> fail("Unexpected $createSubFolder")
            }

        // Act: get all folders for the user
        val getFolders = storageService.getFolders(user, DEFAULT_LIMIT, DEFAULT_PAGE, DEFAULT_SORT)

        // Assert: should have 3 folders
        assertEquals(3, getFolders.content.size)

        // Assert: subfolder's parent is folder1
        val subFolder =
            getFolders.content.find { it.folderId == subFolderId }
                ?: fail("Subfolder not found in folder list")
        assertEquals(folderId1, subFolder.parentFolderId)

        // Act: retrieve folders with limit and descending creation order
        val newLimit = 2
        val newSort = "created_desc"
        val getFoldersLimited = storageService.getFolders(user, newLimit, DEFAULT_PAGE, newSort)

        // Assert: content[0] is the last created (subFolder), content[1] is the penultimate (folder2)
        assertEquals(2, getFoldersLimited.content.size)
        assertEquals(subFolderId, getFoldersLimited.content[0].folderId)
        assertEquals(folderId2, getFoldersLimited.content[1].folderId)

        // Cleanup
        storageService.deleteFolder(user, folderId2)
        storageService.deleteFolder(user, subFolderId)
        storageService.deleteFolder(user, folderId1)
    }

    @Test
    fun `should create nested folders, insert files with clock updates, verify folder metadata, delete file and cleanup`() {
        // Arrange
        val clock = testClock
        val storageService = createStorageService(clock)
        val user = testUser

        // Act: create root folder
        val rootFolderResult = storageService.createFolder("RootFolder", user)
        val rootFolderId =
            when (rootFolderResult) {
                is Success -> rootFolderResult.value
                is Failure -> fail("Unexpected $rootFolderResult")
            }

        // Act: create first subfolder inside root
        clock.advance(1.minutes)
        val subFolder1Result = storageService.createFolderInFolder("SubFolder1", user, rootFolderId)
        val subFolder1Id =
            when (subFolder1Result) {
                is Success -> subFolder1Result.value
                is Failure -> fail("Unexpected $subFolder1Result")
            }

        // Act: create second subfolder inside first subfolder
        clock.advance(1.minutes)
        val subFolder2Result = storageService.createFolderInFolder("SubFolder2", user, subFolder1Id)
        val subFolder2Id =
            when (subFolder2Result) {
                is Success -> subFolder2Result.value
                is Failure -> fail("Unexpected $subFolder2Result")
            }

        // Act: insert 3 files into the deepest subfolder, advancing clock each time
        clock.advance(1.minutes)
        val file1 = fileCreation()
        val file1Result = storageService.uploadFileInFolder(file1, file1.encryption, user, subFolder2Id)
        val file1Id =
            when (file1Result) {
                is Success -> file1Result.value
                is Failure -> fail("Unexpected $file1Result")
            }

        clock.advance(1.minutes)
        val file2 = fileCreation()
        val file2Result = storageService.uploadFileInFolder(file2, file2.encryption, user, subFolder2Id)
        val file2Id =
            when (file2Result) {
                is Success -> file2Result.value
                is Failure -> fail("Unexpected $file2Result")
            }

        clock.advance(1.minutes)
        val file3 = fileCreation()
        val file3Result = storageService.uploadFileInFolder(file3, file3.encryption, user, subFolder2Id)
        val file3Id =
            when (file3Result) {
                is Success -> file3Result.value
                is Failure -> fail("Unexpected $file3Result")
            }

        // Act: get files in the deepest subfolder
        val filesInSubFolder2 = storageService.getFilesInFolder(user, subFolder2Id, DEFAULT_LIMIT, DEFAULT_PAGE, DEFAULT_SORT)

        // Assert: all 3 files are present
        when (filesInSubFolder2) {
            is Success -> {
                assertEquals(3, filesInSubFolder2.value.content.size)
                assertTrue(filesInSubFolder2.value.content.any { it.fileId == file1Id })
                assertTrue(filesInSubFolder2.value.content.any { it.fileId == file2Id })
                assertTrue(filesInSubFolder2.value.content.any { it.fileId == file3Id })
            }
            is Failure -> fail("Unexpected $filesInSubFolder2")
        }

        // Assert: folder metadata updated (number of files and updatedAt)
        val subFolder2Info = storageService.getFolderById(user, subFolder2Id)
        when (subFolder2Info) {
            is Success -> {
                assertEquals(3, subFolder2Info.value.numberFiles)
                assertTrue(subFolder2Info.value.updatedAt.epochSeconds > subFolder2Info.value.createdAt.epochSeconds)
            }
            is Failure -> fail("Unexpected $subFolder2Info")
        }

        // Act: delete one file and check folder metadata again
        clock.advance(1.minutes)
        storageService.deleteFileInFolder(user, subFolder2Id, file2Id)
        val subFolder2InfoAfterDelete = storageService.getFolderById(user, subFolder2Id)
        when (subFolder2InfoAfterDelete) {
            is Success -> {
                assertEquals(2, subFolder2InfoAfterDelete.value.numberFiles)
                assertTrue(subFolder2InfoAfterDelete.value.updatedAt.epochSeconds > subFolder2Info.value.updatedAt.epochSeconds)
            }
            is Failure -> fail("Unexpected $subFolder2InfoAfterDelete")
        }

        // Cleanup: delete remaining files and folders
        storageService.deleteFileInFolder(user, subFolder2Id, file1Id)
        storageService.deleteFileInFolder(user, subFolder2Id, file3Id)
        storageService.deleteFolder(user, subFolder2Id)
        storageService.deleteFolder(user, subFolder1Id)
        storageService.deleteFolder(user, rootFolderId)
    }
}
