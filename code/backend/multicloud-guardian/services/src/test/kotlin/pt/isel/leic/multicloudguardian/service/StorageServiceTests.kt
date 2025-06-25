package pt.isel.leic.multicloudguardian.service

import pt.isel.leic.multicloudguardian.domain.folder.FolderType
import pt.isel.leic.multicloudguardian.domain.folder.InviteStatus
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Success
import pt.isel.leic.multicloudguardian.service.storage.CreateTempUrlFileError
import pt.isel.leic.multicloudguardian.service.storage.DownloadFileError
import pt.isel.leic.multicloudguardian.service.storage.GetFileByIdError
import pt.isel.leic.multicloudguardian.service.storage.GetFileInFolderError
import pt.isel.leic.multicloudguardian.service.storage.GetFolderByIdError
import pt.isel.leic.multicloudguardian.service.storage.GetFoldersInFolderError
import pt.isel.leic.multicloudguardian.service.storage.InviteStatusResult
import pt.isel.leic.multicloudguardian.service.storage.UploadFileError
import pt.isel.leic.multicloudguardian.service.utils.ServiceTests
import pt.isel.leic.multicloudguardian.service.utils.TestClock
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
        val folderType = FolderType.PRIVATE
        val createFolder = storageService.createFolder(folderName, user, folderType)

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
                assertEquals(folderName, getFolder.value.folder.folderName)
                assertEquals(userInfo, getFolder.value.folder.user)
                assertEquals(0, getFolder.value.members.size)
                assertEquals(createFolder.value, getFolder.value.folder.folderId)
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
        val getFile = storageService.getFileInFolder(user, getFolder.value.folder.folderId, uploadFileInFolder.value)

        // Assert: file retrieval from folder should return correct details
        when (getFile) {
            is Success -> {
                assertEquals(uploadFileInFolder.value, getFile.value.fileId)
                assertEquals(getFolder.value.folder.folderId, getFile.value.folderInfo?.id)
                assertEquals(getFolder.value.folder.folderName, getFile.value.folderInfo?.folderName)
                assertEquals(file.size, getFile.value.size)
                assertEquals(getFolder.value.folder.user, getFile.value.user)
            }
            is Failure -> fail("Unexpected $getFile")
        }

        // Act: delete the file from the folder
        val deleteFile = storageService.deleteFileInFolder(user, getFolder.value.folder.folderId, getFile.value.fileId)

        // Assert: file deletion from folder should succeed
        when (deleteFile) {
            is Success -> assertTrue(deleteFile.value)
            is Failure -> fail("Unexpected $deleteFile")
        }

        // Act: try to retrieve the deleted file from the folder
        val getDeletedFileResult = storageService.getFileInFolder(testUser, getFolder.value.folder.folderId, uploadFileInFolder.value)

        // Assert: retrieval should fail with FileNotFound error
        when (getDeletedFileResult) {
            is Success -> fail("Expected file to be deleted, but got $getDeletedFileResult")
            is Failure -> assertTrue(getDeletedFileResult.value is GetFileInFolderError.FileNotFound)
        }

        // Act: check folder size after file deletion
        val getFileAgain = storageService.getFolderById(user, createFolder.value)

        // Assert: folder size should be zero
        when (getFileAgain) {
            is Success -> assertEquals(0, getFileAgain.value.folder.size)
            is Failure -> fail("Unexpected $getFileAgain")
        }

        // Act: delete the folder
        val deleteFolder = storageService.deleteFolder(user, getFolder.value.folder.folderId)

        // Assert: folder deletion should succeed
        when (deleteFolder) {
            is Success -> assertTrue(deleteFolder.value)
            is Failure -> fail("Unexpected $deleteFile")
        }

        // Act: try to retrieve a file from the deleted folder
        val getDeletedFolderResult = storageService.getFileInFolder(testUser, getFolder.value.folder.folderId, uploadFileInFolder.value)

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
        val clock = TestClock()

        // Act: create and upload a single file
        val fileCreation = fileCreation()
        clock.advance(1.minutes)
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
                assertEquals(file.folderInfo, getFileResult.value.folderInfo)
                assertEquals(fileCreation.blobName, getFileResult.value.fileName)
                assertEquals(fileCreation.size, getFileResult.value.size)
                assertEquals(fileCreation.contentType, getFileResult.value.contentType)
                assertEquals(fileCreation.encryption, getFileResult.value.encryption)
            }
            is Failure -> fail("Unexpected $getFileResult")
        }

        // Act: create and upload two more files

        val fileContent1 = fileCreation()
        clock.advance(1.minutes)
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
        val folderType = FolderType.PRIVATE
        val parentFolderName = "ParentFolder"
        val subFolder1Name = "SubFolder1"
        val subFolder2Name = "SubFolder2"
        val user = testUser
        val setLimit = DEFAULT_LIMIT
        val setPage = DEFAULT_PAGE
        val setSort = DEFAULT_SORT

        // Act: create a parent folder for the user
        val createParentFolder = storageService.createFolder(parentFolderName, user, folderType)

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

        // Assert: parent folder metadata is updated (number of files and updatedAt)
        when (parentFolder) {
            is Success -> {
                assertEquals(1, parentFolder.value.folder.numberFiles)
                assertTrue(parentFolder.value.folder.updatedAt.epochSeconds > parentFolder.value.folder.createdAt.epochSeconds)
            }
            is Failure -> fail("Unexpected $parentFolder")
        }

        // Assert: uploaded file is present in the parent folder with correct metadata
        when (fileResult) {
            is Success -> {
                assertEquals(parentFolderId, fileResult.value.folderInfo?.id)
                assertEquals(parentFolder.value.folder.folderName, fileResult.value.folderInfo?.folderName)
                assertEquals(file.blobName, fileResult.value.fileName)
            }
            is Failure -> fail("Unexpected $fileResult")
        }

        // Act: create two subfolders inside the parent folder
        val createSubFolder1 = storageService.createFolderInFolder(subFolder1Name, user, folderType, parentFolderId)

        when (createSubFolder1) {
            is Success -> assertTrue(createSubFolder1.value.value > 0)
            is Failure -> fail("Unexpected $createSubFolder1")
        }
        val subFolder1Id = createSubFolder1.value

        val createSubFolder2 = storageService.createFolderInFolder(subFolder2Name, user, folderType, parentFolderId)

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
        val folderType = FolderType.PRIVATE

        // Act: create a new folder
        val createFolderResult = storageService.createFolder(folderName, user, folderType)
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
        val folderType = FolderType.PRIVATE

        // Act: create a new folder
        val createFolderResult = storageService.createFolder(folderName, user, folderType)
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
        val folderType = FolderType.PRIVATE

        // Act: create a new folder
        val createFolderResult = storageService.createFolder(folderName, user, folderType)
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
        // Arrange: initialize storage service and test user
        val clock = testClock
        val storageService = createStorageService(clock)
        val user = testUser
        val folderType = FolderType.PRIVATE

        // Act: create two normal folders
        val folderName1 = "Folder1"
        val folderName2 = "Folder2"
        val createFolder1 = storageService.createFolder(folderName1, user, folderType)
        clock.advance(1.minutes)
        val createFolder2 = storageService.createFolder(folderName2, user, folderType)
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
        val createSubFolder = storageService.createFolderInFolder(subFolderName, user, folderType, folderId1)

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
        assertEquals(folderId1, subFolder.parentFolderInfo?.id)
        assertEquals(folderName1, subFolder.parentFolderInfo?.folderName)

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
        // Arrange: initialize storage service and test user
        val clock = testClock
        val storageService = createStorageService(clock)
        val user = testUser
        val folderType = FolderType.PRIVATE

        // Act: create root folder
        val rootFolderResult = storageService.createFolder("RootFolder", user, folderType)
        val rootFolderId =
            when (rootFolderResult) {
                is Success -> rootFolderResult.value
                is Failure -> fail("Unexpected $rootFolderResult")
            }

        // Act: create first subfolder inside root
        clock.advance(1.minutes)
        val subFolder1Result = storageService.createFolderInFolder("SubFolder1", user, folderType, rootFolderId)
        val subFolder1Id =
            when (subFolder1Result) {
                is Success -> subFolder1Result.value
                is Failure -> fail("Unexpected $subFolder1Result")
            }

        // Act: create second subfolder inside first subfolder
        clock.advance(1.minutes)
        val subFolder2Result = storageService.createFolderInFolder("SubFolder2", user, folderType, subFolder1Id)
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
                assertEquals(3, subFolder2Info.value.folder.numberFiles)
                assertTrue(subFolder2Info.value.folder.updatedAt.epochSeconds > subFolder2Info.value.folder.createdAt.epochSeconds)
            }
            is Failure -> fail("Unexpected $subFolder2Info")
        }

        // Act: delete one file and check folder metadata again
        clock.advance(1.minutes)
        storageService.deleteFileInFolder(user, subFolder2Id, file2Id)
        val subFolder2InfoAfterDelete = storageService.getFolderById(user, subFolder2Id)
        when (subFolder2InfoAfterDelete) {
            is Success -> {
                assertEquals(2, subFolder2InfoAfterDelete.value.folder.numberFiles)
                assertTrue(
                    subFolder2InfoAfterDelete.value.folder.updatedAt.epochSeconds > subFolder2Info.value.folder.updatedAt.epochSeconds,
                )
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

    @Test
    fun `move file between root and folder updates folder metadata correctly`() {
        // Arrange: initialize storage service and test user
        val clock = TestClock()
        val storageService = createStorageService(clock)
        val user = testUser
        val folderType = FolderType.PRIVATE

        // Act:  Upload file to root
        val fileContent = fileCreation()
        val uploadFileResult = storageService.uploadFile(fileContent, fileContent.encryption, user)
        val fileId =
            when (uploadFileResult) {
                is Success -> uploadFileResult.value
                is Failure -> fail("Unexpected $uploadFileResult")
            }

        // Act: Create a folder
        val folderName = "TestFolder"
        val createFolderResult = storageService.createFolder(folderName, user, folderType)
        val folderId =
            when (createFolderResult) {
                is Success -> createFolderResult.value
                is Failure -> fail("Unexpected $createFolderResult")
            }

        // Act: Move file from root to folder
        clock.advance(1.minutes)
        val moveToFolderResult = storageService.moveFile(user, fileId, folderId)

        // Assert: file moved successfully
        when (moveToFolderResult) {
            is Success -> assertTrue(moveToFolderResult.value)
            is Failure -> fail("Unexpected $moveToFolderResult")
        }

        // Assert: folder metadata updated
        val folderAfterMove = storageService.getFolderById(user, folderId)

        // Act:  Check folder metadata after moving file in
        when (folderAfterMove) {
            is Success -> {
                assertEquals(1, folderAfterMove.value.folder.numberFiles)
                assertTrue(folderAfterMove.value.folder.updatedAt.epochSeconds > folderAfterMove.value.folder.createdAt.epochSeconds)
            }
            is Failure -> fail("Unexpected $folderAfterMove")
        }

        // Act: Move file back to root (folderId = null)
        clock.advance(1.minutes)
        val moveToRootResult = storageService.moveFile(user, fileId, null)

        // Assert: file moved back to root successfully
        when (moveToRootResult) {
            is Success -> assertTrue(moveToRootResult.value)
            is Failure -> fail("Unexpected $moveToRootResult")
        }

        // Act: Check folder metadata after moving file out
        val folderAfterMoveOut = storageService.getFolderById(user, folderId)

        // Assert: folder metadata updated correctly after moving file out
        when (folderAfterMoveOut) {
            is Success -> {
                assertEquals(0, folderAfterMoveOut.value.folder.numberFiles)
                assertTrue(folderAfterMoveOut.value.folder.updatedAt.epochSeconds > folderAfterMove.value.folder.updatedAt.epochSeconds)
            }
            is Failure -> fail("Unexpected $folderAfterMoveOut")
        }

        // Cleanup
        storageService.deleteFile(user, fileId)
        storageService.deleteFolder(user, folderId)
    }

    @Test
    fun `move file to folder updates file folderId and file appears in folder`() {
        // Arrange: initialize storage service and test user
        val clock = TestClock()
        val storageService = createStorageService(clock)
        val user = testUser
        val folderType = FolderType.PRIVATE

        // Act: Upload file to root
        val fileContent = fileCreation()
        val uploadFileResult = storageService.uploadFile(fileContent, fileContent.encryption, user)

        // Assert: file upload should succeed
        val fileId =
            when (uploadFileResult) {
                is Success -> uploadFileResult.value
                is Failure -> fail("Unexpected $uploadFileResult")
            }

        // Act: Create a folder
        val folderName = "TestFolder"
        val createFolderResult = storageService.createFolder(folderName, user, folderType)

        // Assert: folder creation should succeed
        val folderId =
            when (createFolderResult) {
                is Success -> createFolderResult.value
                is Failure -> fail("Unexpected $createFolderResult")
            }

        // Act: Move file from root to folder
        clock.advance(1.minutes)
        val moveToFolderResult = storageService.moveFile(user, fileId, folderId)

        // Assert: file should be moved successfully
        when (moveToFolderResult) {
            is Success -> assertTrue(moveToFolderResult.value)
            is Failure -> fail("Unexpected $moveToFolderResult")
        }

        // Act: Check file's folderId is updated
        val getFile = storageService.getFileById(user, fileId)

        // Assert: file should now belong to the created folder
        when (getFile) {
            is Success -> {
                assertEquals(folderId, getFile.value.folderInfo?.id)
                assertEquals(folderName, getFile.value.folderInfo?.folderName)
            }
            is Failure -> fail("Unexpected $getFile")
        }

        // Act: Check files in the folder
        val filesInFolder = storageService.getFilesInFolder(user, folderId, DEFAULT_LIMIT, DEFAULT_PAGE, DEFAULT_SORT)

        // Assert: file should appear in the folder's file list
        when (filesInFolder) {
            is Success -> assertTrue(filesInFolder.value.content.any { it.fileId == fileId })
            is Failure -> fail("Unexpected $filesInFolder")
        }

        // Cleanup
        storageService.deleteFile(user, fileId)
        storageService.deleteFolder(user, folderId)
    }

    @Test
    fun `swap files between folder and subfolder updates folderId and folder metadata`() {
        // Arrange: initialize storage service and test user
        val clock = TestClock()
        val storageService = createStorageService(clock)
        val user = testUser
        val folderType = FolderType.PRIVATE

        // Act: Create folder
        val folderResult = storageService.createFolder("Folder", user, folderType)

        // Assert: folder creation should succeed
        val folderId =
            when (folderResult) {
                is Success -> folderResult.value
                is Failure -> fail("Unexpected $folderResult")
            }

        // Act: Create subfolder inside the folder
        val subFolderResult = storageService.createFolderInFolder("SubFolder", user, folderType, folderId)

        // Assert: subfolder creation should succeed
        val subFolderId =
            when (subFolderResult) {
                is Success -> subFolderResult.value
                is Failure -> fail("Unexpected $subFolderResult")
            }

        // Upload file1 to folder, file2 to subfolder
        val fileContent1 = fileCreation()
        val fileContent2 = fileCreation()
        val file1Result = storageService.uploadFileInFolder(fileContent1, fileContent1.encryption, user, folderId)
        val file2Result = storageService.uploadFileInFolder(fileContent2, fileContent2.encryption, user, subFolderId)
        val file1Id =
            when (file1Result) {
                is Success -> file1Result.value
                is Failure -> fail("Unexpected $file1Result")
            }
        val file2Id =
            when (file2Result) {
                is Success -> file2Result.value
                is Failure -> fail("Unexpected $file2Result")
            }

        // Swap: move file1 to subfolder, file2 to folder
        clock.advance(1.minutes)

        // Act: Move file1 to subfolder, file2 to folder
        val move1 = storageService.moveFile(user, file1Id, subFolderId)
        val move2 = storageService.moveFile(user, file2Id, folderId)

        // Assert: both moves should succeed
        when (move1) {
            is Success -> assertTrue(move1.value)
            is Failure -> fail("Unexpected $move1")
        }
        when (move2) {
            is Success -> assertTrue(move2.value)
            is Failure -> fail("Unexpected $move2")
        }

        // Act: Check file1 is now in subfolder, file2 in folder
        val getFile1 = storageService.getFileById(user, file1Id)
        when (getFile1) {
            is Success -> {
                assertEquals(subFolderId, getFile1.value.folderInfo?.id)
                assertEquals("SubFolder", getFile1.value.folderInfo?.folderName)
            }
            is Failure -> fail("Unexpected $getFile1")
        }
        // Check file2 is now in folder
        val getFile2 = storageService.getFileById(user, file2Id)
        when (getFile2) {
            is Success -> {
                assertEquals(folderId, getFile2.value.folderInfo?.id)
                assertEquals("Folder", getFile2.value.folderInfo?.folderName)
            }
            is Failure -> fail("Unexpected $getFile2")
        }

        // Act: Check files in folder and subfolder
        val filesInFolder = storageService.getFilesInFolder(user, folderId, DEFAULT_LIMIT, DEFAULT_PAGE, DEFAULT_SORT)
        val filesInSubFolder = storageService.getFilesInFolder(user, subFolderId, DEFAULT_LIMIT, DEFAULT_PAGE, DEFAULT_SORT)

        // Assert: file1 should be in subfolder, file2 in folder
        when (filesInFolder) {
            is Success -> assertTrue(filesInFolder.value.content.any { it.fileId == file2Id })
            is Failure -> fail("Unexpected $filesInFolder")
        }
        when (filesInSubFolder) {
            is Success -> assertTrue(filesInSubFolder.value.content.any { it.fileId == file1Id })
            is Failure -> fail("Unexpected $filesInSubFolder")
        }

        // Act: Check folder metadata after swapping files
        val folderInfo = storageService.getFolderById(user, folderId)
        val subFolderInfo = storageService.getFolderById(user, subFolderId)

        // Assert: folder metadata should reflect the correct number of files
        when (folderInfo) {
            is Success -> assertEquals(1, folderInfo.value.folder.numberFiles)
            is Failure -> fail("Unexpected $folderInfo")
        }
        when (subFolderInfo) {
            is Success -> assertEquals(1, subFolderInfo.value.folder.numberFiles)
            is Failure -> fail("Unexpected $subFolderInfo")
        }

        // Cleanup
        storageService.deleteFileInFolder(user, folderId, file2Id)
        storageService.deleteFileInFolder(user, subFolderId, file1Id)
        storageService.deleteFolder(user, subFolderId)
        storageService.deleteFolder(user, folderId)
    }

    @Test
    fun `move files from subfolder to root and folder updates all listings and metadata`() {
        // Arrange: initialize storage service and test user
        val clock = TestClock()
        val storageService = createStorageService(clock)
        val user = testUser
        val folderType = FolderType.PRIVATE

        // Act: Create a parent folder
        val folderResult = storageService.createFolder("ParentFolder", user, folderType)

        // Assert: folder creation should succeed
        val folderId =
            when (folderResult) {
                is Success -> folderResult.value
                is Failure -> fail("Unexpected $folderResult")
            }

        // Act: Create a subfolder inside the parent folder
        val subFolderResult = storageService.createFolderInFolder("SubFolder", user, folderType, folderId)

        // Assert: subfolder creation should succeed
        val subFolderId =
            when (subFolderResult) {
                is Success -> subFolderResult.value
                is Failure -> fail("Unexpected $subFolderResult")
            }

        // Act: Upload two files to the subfolder
        val fileContent1 = fileCreation()
        val fileContent2 = fileCreation()
        val file1Result = storageService.uploadFileInFolder(fileContent1, fileContent1.encryption, user, subFolderId)
        val file2Result = storageService.uploadFileInFolder(fileContent2, fileContent2.encryption, user, subFolderId)

        // Assert: both file uploads should succeed
        val file1Id =
            when (file1Result) {
                is Success -> file1Result.value
                is Failure -> fail("Unexpected $file1Result")
            }
        val file2Id =
            when (file2Result) {
                is Success -> file2Result.value
                is Failure -> fail("Unexpected $file2Result")
            }

        // Act: Check files in the subfolder
        val filesInSubFolder = storageService.getFilesInFolder(user, subFolderId, DEFAULT_LIMIT, DEFAULT_PAGE, DEFAULT_SORT)

        // Assert: both files are in the subfolder
        when (filesInSubFolder) {
            is Success -> {
                assertEquals(2, filesInSubFolder.value.content.size)
                assertTrue(filesInSubFolder.value.content.any { it.fileId == file1Id })
                assertTrue(filesInSubFolder.value.content.any { it.fileId == file2Id })
            }
            is Failure -> fail("Unexpected $filesInSubFolder")
        }

        // Act: get all files and folders for the user
        val allFiles = storageService.getFiles(user, DEFAULT_LIMIT, DEFAULT_PAGE, DEFAULT_SORT)

        // Assert: getFiles returns both files
        assertTrue(allFiles.content.any { it.fileId == file1Id })
        assertTrue(allFiles.content.any { it.fileId == file2Id })

        // Act: get all folders for the user
        val allFolders = storageService.getFolders(user, DEFAULT_LIMIT, DEFAULT_PAGE, DEFAULT_SORT)

        // Assert: getFolders returns both parent and subfolder
        assertTrue(allFolders.content.any { it.folderId == folderId })
        assertTrue(allFolders.content.any { it.folderId == subFolderId })

        // Act: Move files from subfolder to root and parent folder
        clock.advance(1.minutes)
        val move1 = storageService.moveFile(user, file1Id, null)
        val move2 = storageService.moveFile(user, file2Id, folderId)

        // Assert: both moves should succeed
        when (move1) {
            is Success -> assertTrue(move1.value)
            is Failure -> fail("Unexpected $move1")
        }
        when (move2) {
            is Success -> assertTrue(move2.value)
            is Failure -> fail("Unexpected $move2")
        }

        // Act: Check file1 is now in root, file2 in parent folder
        val getFile1 = storageService.getFileById(user, file1Id)

        // Assert: file1 should have no folderId (root)
        when (getFile1) {
            is Success -> assertEquals(null, getFile1.value.folderInfo)
            is Failure -> fail("Unexpected $getFile1")
        }

        // Act: Check file2 is now in parent folder
        val getFile2 = storageService.getFileById(user, file2Id)

        // Assert: file2 should have folderId set to parent folder
        when (getFile2) {
            is Success -> {
                assertEquals(folderId, getFile2.value.folderInfo?.id)
                assertEquals("ParentFolder", getFile2.value.folderInfo?.folderName)
            }
            is Failure -> fail("Unexpected $getFile2")
        }

        // Assert: subfolder is now empty
        val filesInSubFolderAfter = storageService.getFilesInFolder(user, subFolderId, DEFAULT_LIMIT, DEFAULT_PAGE, DEFAULT_SORT)
        when (filesInSubFolderAfter) {
            is Success -> assertTrue(filesInSubFolderAfter.value.content.isEmpty())
            is Failure -> fail("Unexpected $filesInSubFolderAfter")
        }

        // Assert: parent folder has file2
        val filesInParentFolder = storageService.getFilesInFolder(user, folderId, DEFAULT_LIMIT, DEFAULT_PAGE, DEFAULT_SORT)
        when (filesInParentFolder) {
            is Success -> {
                assertEquals(1, filesInParentFolder.value.content.size)
                assertTrue(filesInParentFolder.value.content.any { it.fileId == file2Id })
            }
            is Failure -> fail("Unexpected $filesInParentFolder")
        }

        // Assert: getFiles shows correct folderId for each file
        val allFilesAfter = storageService.getFiles(user, DEFAULT_LIMIT, DEFAULT_PAGE, DEFAULT_SORT)
        val file1 = allFilesAfter.content.find { it.fileId == file1Id }
        val file2 = allFilesAfter.content.find { it.fileId == file2Id }
        assertNotNull(file1)
        assertNotNull(file2)
        assertEquals(null, file1.folderInfo)
        assertEquals(folderId, file2.folderInfo?.id)
        assertEquals("ParentFolder", file2.folderInfo?.folderName)

        // Assert: folder metadata
        val parentFolderInfo = storageService.getFolderById(user, folderId)
        val subFolderInfo = storageService.getFolderById(user, subFolderId)
        when (parentFolderInfo) {
            is Success -> assertEquals(1, parentFolderInfo.value.folder.numberFiles)
            is Failure -> fail("Unexpected $parentFolderInfo")
        }
        when (subFolderInfo) {
            is Success -> assertEquals(0, subFolderInfo.value.folder.numberFiles)
            is Failure -> fail("Unexpected $subFolderInfo")
        }

        // Cleanup
        storageService.deleteFile(user, file1Id)
        storageService.deleteFileInFolder(user, folderId, file2Id)
        storageService.deleteFolder(user, subFolderId)
        storageService.deleteFolder(user, folderId)
    }

    @Test
    fun `upload file to root, move to folder, download and verify content`() {
        // Arrange: initialize storage service and test user
        val clock = TestClock()
        val storageService = createStorageService(clock)
        val user = testUser
        val folderType = FolderType.PRIVATE

        // Upload file to root
        val fileContent = fileCreation()
        val uploadFileResult = storageService.uploadFile(fileContent, fileContent.encryption, user)
        val fileId =
            when (uploadFileResult) {
                is Success -> uploadFileResult.value
                is Failure -> fail("Unexpected $uploadFileResult")
            }

        // Create a folder
        val folderResult = storageService.createFolder("TestFolder", user, folderType)
        val folderId =
            when (folderResult) {
                is Success -> folderResult.value
                is Failure -> fail("Unexpected $folderResult")
            }

        // Move file from root to folder
        clock.advance(1.minutes)
        val moveResult = storageService.moveFile(user, fileId, folderId)
        when (moveResult) {
            is Success -> assertTrue(moveResult.value)
            is Failure -> fail("Unexpected $moveResult")
        }

        // Act:
        val getFile = storageService.getFileById(user, fileId)
        when (getFile) {
            is Success -> {
                // Assert: file's folderId is updated to the new folder
                assertEquals(folderId, getFile.value.folderInfo?.id)
                assertEquals("TestFolder", getFile.value.folderInfo?.folderName)
            }
            is Failure -> fail("Unexpected $getFile")
        }

        // Download the file from the folder
        val downloadResult = storageService.downloadFileInFolder(user, folderId, getFile.value.fileId)
        when (downloadResult) {
            is Success -> {
                // Assert content and metadata
                assertContentEquals(fileContent.fileContent, downloadResult.value.first.fileContent)
                assertEquals(fileContent.blobName, downloadResult.value.first.fileName)
                assertEquals(fileContent.contentType, downloadResult.value.first.mimeType)
                assertEquals(fileContent.encryption, downloadResult.value.first.encrypted)
            }
            is Failure -> fail("Unexpected $downloadResult")
        }

        // Cleanup
        storageService.deleteFileInFolder(user, folderId, fileId)
        storageService.deleteFolder(user, folderId)
    }

    @Test
    fun `create shared folder, move file, and verify only owner is member`() {
        // Arrange: initialize storage service and test user
        val clock = TestClock()
        val storageService = createStorageService(clock)
        val user = testUser
        val folderType = FolderType.SHARED

        // Upload file to root
        val fileContent = fileCreation()
        val uploadFileResult = storageService.uploadFile(fileContent, fileContent.encryption, user)
        val fileId =
            when (uploadFileResult) {
                is Success -> uploadFileResult.value
                is Failure -> fail("Unexpected $uploadFileResult")
            }

        // Create a shared folder
        val folderResult = storageService.createFolder("SharedFolder", user, folderType)
        val folderId =
            when (folderResult) {
                is Success -> folderResult.value
                is Failure -> fail("Unexpected $folderResult")
            }

        // Move file from root to shared folder
        clock.advance(1.minutes)
        val moveResult = storageService.moveFile(user, fileId, folderId)
        when (moveResult) {
            is Success -> assertTrue(moveResult.value)
            is Failure -> fail("Unexpected $moveResult")
        }

        // Act: get folder by id
        val getFolderResult = storageService.getFolderById(user, folderId)
        when (getFolderResult) {
            is Success -> {
                val folderInfo = getFolderResult.value.folder
                // Assert: folder type is SHARED and only the owner is a member
                assertEquals(FolderType.SHARED, folderInfo.type)
                assertEquals(1, getFolderResult.value.members.size)
                assertEquals(
                    user.id,
                    getFolderResult.value.members
                        .first()
                        .id,
                )
            }
            is Failure -> fail("Unexpected $getFolderResult")
        }

        // Cleanup
        storageService.deleteFileInFolder(user, folderId, fileId)
        storageService.deleteFolder(user, folderId)
    }

    @Test
    fun `invite user to shared folder, accept invite, and verify membership`() {
        // Arrange: initialize storage service and users
        val storageService = createStorageService()
        val owner = testUser
        val invitedUser = testUser2
        val folderType = FolderType.SHARED
        val folderName = "SharedFolder"

        // Create a shared folder
        val folderResult = storageService.createFolder(folderName, owner, folderType)
        val folderId =
            when (folderResult) {
                is Success -> folderResult.value
                is Failure -> fail("Unexpected $folderResult")
            }

        // Invite the second user to the shared folder
        val inviteResult = storageService.inviteFolder(folderId, owner, invitedUser.username)
        val inviteCode =
            when (inviteResult) {
                is Success -> inviteResult.value // Assume this returns the invite code or link
                is Failure -> fail("Unexpected $inviteResult")
            }

        // Invited user accepts the invite
        val acceptResult = storageService.validateFolderInvite(invitedUser, folderId, inviteCode, InviteStatus.ACCEPT)
        when (acceptResult) {
            is Success ->
                when (val result = acceptResult.value) {
                    is InviteStatusResult.InviteAccepted -> {
                        val folder = result.folderMembers.folder
                        assertEquals(folderName, folder.folderName)
                        assertEquals(folderId, folder.folderId)
                        assertEquals(folderType, folder.type)
                    }
                    is InviteStatusResult.InviteRejected -> fail("Expected InviteAccepted but got ${acceptResult.value}")
                }

            is Failure -> fail("Unexpected $acceptResult")
        }

        // Cleanup
        storageService.deleteFolder(owner, folderId)
    }

    @Test
    fun `private and shared folder workflow with invites, search, and membership changes`() {
        // Arrange: initialize storage service and users
        val storageService = createStorageService()
        val owner = testUser
        val invitedUser = testUser2
        val privateFolderName = "PrivateFolder"
        val privateFolderType = FolderType.PRIVATE

        // Act: Owner creates a private folder and uploads a file
        val privateFolderId =
            when (val res = storageService.createFolder(privateFolderName, owner, privateFolderType)) {
                is Success -> res.value
                is Failure -> fail("Unexpected $res")
            }

        // Act: Owner uploads a file to the private folder
        val fileContent = fileCreation()
        val privateFileId =
            when (val res = storageService.uploadFileInFolder(fileContent, fileContent.encryption, owner, privateFolderId)) {
                is Success -> res.value
                is Failure -> fail("Unexpected $res")
            }

        // Act: Owner creates a shared folder and invites another user
        val sharedFolderName = "SharedFolder"
        val sharedFolderType = FolderType.SHARED
        val sharedFolderId =
            when (val res = storageService.createFolder(sharedFolderName, owner, sharedFolderType)) {
                is Success -> res.value
                is Failure -> fail("Unexpected $res")
            }

        // Act: Owner invites the second user to the shared folder
        val inviteCode =
            when (val res = storageService.inviteFolder(sharedFolderId, owner, invitedUser.username)) {
                is Success -> res.value
                is Failure -> fail("Unexpected $res")
            }

        // Assert: Invited user accepts the invite
        val acceptResult = storageService.validateFolderInvite(invitedUser, sharedFolderId, inviteCode, InviteStatus.ACCEPT)
        when (acceptResult) {
            is Success ->
                when (val result = acceptResult.value) {
                    is InviteStatusResult.InviteAccepted -> {
                        val folder = result.folderMembers.folder
                        assertEquals(sharedFolderName, folder.folderName)
                        assertEquals(sharedFolderId, folder.folderId)
                        assertEquals(sharedFolderType, folder.type)
                    }
                    is InviteStatusResult.InviteRejected -> fail("Expected InviteAccepted but got ${acceptResult.value}")
                }

            is Failure -> fail("Unexpected $acceptResult")
        }

        // Act: Invited user checks folders
        val invitedPrivateFolderName = "InvitedPrivate"
        val invitedPrivateFolderId =
            when (val res = storageService.createFolder(invitedPrivateFolderName, invitedUser, FolderType.PRIVATE)) {
                is Success -> res.value
                is Failure -> fail("Unexpected $res")
            }

        // Assert: Invited user can see both private and shared folders
        val folders = storageService.getFolders(invitedUser, 10, 0, "created_asc", true)
        assertTrue(folders.content.any { it.folderName == sharedFolderName })
        assertTrue(folders.content.any { it.folderName == invitedPrivateFolderName })

        // Act: Invited user searches for folders with "Priva" keyword
        val searchFolders = storageService.getFolders(invitedUser, 10, 0, "created_asc", true, "Priva")

        // Assert: Should only find the invited private folder
        assertEquals(1, searchFolders.totalElements)
        assertEquals(invitedPrivateFolderName, searchFolders.content[0].folderName)

        // Act: Invited user uploads a file to the shared folder
        val sharedFileContent = fileCreation()
        val sharedFileId =
            when (
                val res =
                    storageService.uploadFileInFolder(
                        sharedFileContent,
                        sharedFileContent.encryption,
                        invitedUser,
                        sharedFolderId,
                    )
            ) {
                is Success -> res.value
                is Failure -> fail("Unexpected $res")
            }

        // Assert: Check the file is in the shared folder
        val sharedFile =
            when (val res = storageService.getFileInFolder(invitedUser, sharedFolderId, sharedFileId)) {
                is Success -> res.value
                is Failure -> fail("Unexpected $res")
            }
        assertEquals(invitedUser.id, sharedFile.user.id)
        assertEquals(invitedUser.username, sharedFile.user.username)
        assertEquals(invitedUser.email, sharedFile.user.email)
        assertEquals(sharedFileContent.blobName, sharedFile.fileName)
        assertEquals(sharedFileContent.contentType, sharedFile.contentType)

        //  Act: Owner checks folders again (should see both private and shared)
        val leaveResult = storageService.leaveFolder(invitedUser, sharedFolderId)
        assertTrue(leaveResult is Success)

        // Assert: Owner checks folders after invited user leaves shared folder
        val foldersAfterLeave = storageService.getFolders(invitedUser, 10, 0, "created_asc")

        // Assert: Invited user should no longer see the shared folder
        assertEquals(1, foldersAfterLeave.totalElements)
        assertTrue(foldersAfterLeave.content.none { it.folderName == sharedFolderName })
        assertTrue(foldersAfterLeave.content.any { it.folderName == invitedPrivateFolderName })

        // Cleanup
        storageService.deleteFileInFolder(owner, privateFolderId, privateFileId)
        storageService.deleteFolder(owner, privateFolderId)
        storageService.deleteFileInFolder(invitedUser, sharedFolderId, sharedFileId)
        storageService.deleteFolder(owner, sharedFolderId)
        storageService.deleteFolder(invitedUser, invitedPrivateFolderId)
    }

    @Test
    fun `shared folder workflow with multiple users, permissions, search, and cleanup`() {
        // Arrange: initialize storage service and users
        val storageService = createStorageService()
        val user1 = testUser
        val user2 = testUser2
        val user3 = testUser3

        // User1 creates a private folder and uploads a file
        val privateFolderName = "privateFolder"
        val privateFileName = "PrivateFile"
        val privateFolderId =
            when (val res = storageService.createFolder(privateFolderName, user1, FolderType.PRIVATE)) {
                is Success -> res.value
                is Failure -> fail("Unexpected $res")
            }
        val privateFileContent = fileCreation().copy(blobName = privateFileName)
        val privateFileId =
            when (val res = storageService.uploadFileInFolder(privateFileContent, privateFileContent.encryption, user1, privateFolderId)) {
                is Success -> res.value
                is Failure -> fail("Unexpected $res")
            }

        // User1 creates a shared folder and invites user2 and user3
        val sharedFolderName = "folderShared"
        val sharedFolderId =
            when (val res = storageService.createFolder(sharedFolderName, user1, FolderType.SHARED)) {
                is Success -> res.value
                is Failure -> fail("Unexpected $res")
            }
        val inviteCode2 =
            when (val res = storageService.inviteFolder(sharedFolderId, user1, user2.username)) {
                is Success -> res.value
                is Failure -> fail("Unexpected $res")
            }
        val inviteCode3 =
            when (val res = storageService.inviteFolder(sharedFolderId, user1, user3.username)) {
                is Success -> res.value
                is Failure -> fail("Unexpected $res")
            }

        // User2 and User3 accept the invite
        when (val res = storageService.validateFolderInvite(user2, sharedFolderId, inviteCode2, InviteStatus.ACCEPT)) {
            is Success ->
                when (val result = res.value) {
                    is InviteStatusResult.InviteAccepted -> assertEquals(sharedFolderName, result.folderMembers.folder.folderName)
                    is InviteStatusResult.InviteRejected -> fail("Expected InviteAccepted but got ${res.value}")
                }
            is Failure -> fail("Unexpected $res")
        }
        when (val res = storageService.validateFolderInvite(user3, sharedFolderId, inviteCode3, InviteStatus.ACCEPT)) {
            is Success ->
                when (val result = res.value) {
                    is InviteStatusResult.InviteAccepted -> assertEquals(sharedFolderName, result.folderMembers.folder.folderName)
                    is InviteStatusResult.InviteRejected -> fail("Expected InviteAccepted but got ${res.value}")
                }
            is Failure -> fail("Unexpected $res")
        }

        // User2 uploads a file to the shared folder
        val fileTestName = "FileTest"
        val fileTestContent = fileCreation().copy(blobName = fileTestName)
        val fileTestId =
            when (val res = storageService.uploadFileInFolder(fileTestContent, fileTestContent.encryption, user2, sharedFolderId)) {
                is Success -> res.value
                is Failure -> fail("Unexpected $res")
            }

        // User1 and User3 try to view the file uploaded by User2
        val fileByUser1 = storageService.getFileInFolder(user1, sharedFolderId, fileTestId)
        when (fileByUser1) {
            is Success -> assertEquals(fileTestName, fileByUser1.value.fileName)
            is Failure -> fail("User1 should see the file")
        }
        val fileByUser3 = storageService.getFileInFolder(user3, sharedFolderId, fileTestId)
        when (fileByUser3) {
            is Success -> assertEquals(fileTestName, fileByUser3.value.fileName)
            is Failure -> fail("User3 should see the file")
        }

        // User1 searches for files with "Fil" and should see both PrivateFile and FileTest
        val searchFiles = storageService.getFiles(user1, 10, 0, "created_asc", true, "Fil")
        assertTrue(searchFiles.content.any { it.fileName == privateFileName })
        assertTrue(searchFiles.content.any { it.fileName == fileTestName })

        // User3 tries to delete User2's file in the shared folder and should not succeed
        val deleteByUser3 = storageService.deleteFileInFolder(user3, sharedFolderId, fileTestId)
        when (deleteByUser3) {
            is Success -> fail("User3 should not be able to delete User2's file")
            is Failure -> assertTrue(true)
        }

        // User1 uploads a file to the shared folder and deletes User2's file
        val user1SharedFileName = "User1SharedFile"
        val user1SharedFileContent = fileCreation().copy(blobName = user1SharedFileName)
        val user1SharedFileId =
            when (
                val res =
                    storageService.uploadFileInFolder(
                        user1SharedFileContent,
                        user1SharedFileContent.encryption,
                        user1,
                        sharedFolderId,
                    )
            ) {
                is Success -> res.value
                is Failure -> fail("Unexpected $res")
            }
        val deleteByUser1 = storageService.deleteFileInFolder(user1, sharedFolderId, fileTestId)
        when (deleteByUser1) {
            is Success -> assertTrue(deleteByUser1.value)
            is Failure -> fail("User1 should be able to delete User2's file")
        }

        // User3 leaves the shared folder and tries to view User1's file in the shared folder
        val leaveResult = storageService.leaveFolder(user3, sharedFolderId)
        assertTrue(leaveResult is Success)
        val fileAfterLeave = storageService.getFileInFolder(user3, sharedFolderId, user1SharedFileId)
        when (fileAfterLeave) {
            is Success -> fail("User3 should not see the file after leaving the folder")
            is Failure -> assertTrue(true)
        }

        // User3 tries to view User1's private file and private folder
        val privateFileByUser3 = storageService.getFileInFolder(user3, privateFolderId, privateFileId)
        when (privateFileByUser3) {
            is Success -> fail("User3 should not see User1's private file")
            is Failure -> assertTrue(true)
        }
        val privateFolderByUser3 = storageService.getFolderById(user3, privateFolderId)
        when (privateFolderByUser3) {
            is Success -> fail("User3 should not see User1's private folder")
            is Failure -> assertTrue(true)
        }

        // Cleanup: delete all created files and folders
        storageService.deleteFileInFolder(user1, sharedFolderId, user1SharedFileId)
        storageService.deleteFolder(user1, sharedFolderId)
        storageService.deleteFileInFolder(user1, privateFolderId, privateFileId)
        storageService.deleteFolder(user1, privateFolderId)
    }

    @Test
    fun `shared folder - invited user cannot access before accepting invite`() {
        // Arrange: create shared folder and invite user2
        val storageService = createStorageService()
        val owner = testUser
        val invited = testUser2

        // Act: owner creates a shared folder and invites the user
        val sharedFolderId =
            when (val res = storageService.createFolder("Shared", owner, FolderType.SHARED)) {
                is Success -> res.value
                is Failure -> fail("Unexpected $res")
            }

        // Act: owner invites the user to the shared folder
        val inviteCode =
            when (val res = storageService.inviteFolder(sharedFolderId, owner, invited.username)) {
                is Success -> res.value
                is Failure -> fail("Unexpected $res")
            }

        // Act: invited user tries to access folder before accepting
        val folderResult = storageService.getFolderById(invited, sharedFolderId)

        // Assert: should fail (no access)
        when (folderResult) {
            is Success -> fail("Unexpected $folderResult")
            is Failure -> assertEquals(GetFolderByIdError.FolderNotFound, folderResult.value)
        }

        // Cleanup
        storageService.deleteFolder(owner, sharedFolderId)
    }

    @Test
    fun `shared folder - user cannot invite others if not owner`() {
        // Arrange: create shared folder, invite user2, user2 accepts
        val storageService = createStorageService()
        val owner = testUser
        val user2 = testUser2
        val user3 = testUser3
        val sharedFolderId = (storageService.createFolder("Shared", owner, FolderType.SHARED) as Success).value
        val code2 = (storageService.inviteFolder(sharedFolderId, owner, user2.username) as Success).value
        storageService.validateFolderInvite(user2, sharedFolderId, code2, InviteStatus.ACCEPT)

        // Act: user2 tries to invite user3
        val inviteResult = storageService.inviteFolder(sharedFolderId, user2, user3.username)

        // Assert: should fail (not owner)
        assertTrue(inviteResult is Failure)

        // Cleanup
        storageService.deleteFolder(owner, sharedFolderId)
    }

    @Test
    fun `shared folder - user cannot access after leaving`() {
        // Arrange: create shared folder, invite user2, user2 accepts
        val storageService = createStorageService()
        val owner = testUser
        val user2 = testUser2
        val sharedFolderId = (storageService.createFolder("Shared", owner, FolderType.SHARED) as Success).value
        val code2 = (storageService.inviteFolder(sharedFolderId, owner, user2.username) as Success).value
        storageService.validateFolderInvite(user2, sharedFolderId, code2, InviteStatus.ACCEPT)

        // Act: user2 leaves the folder
        val leaveResult = storageService.leaveFolder(user2, sharedFolderId)

        // Assert: user2 cannot access the folder anymore
        assertTrue(leaveResult is Success)
        val folderResult = storageService.getFolderById(user2, sharedFolderId)
        assertTrue(folderResult is Failure)

        // Cleanup
        storageService.deleteFolder(owner, sharedFolderId)
    }

    @Test
    fun `private folder - encrypted file has different cloud name`() {
        // Arrange: create private folder and upload encrypted file
        val storageService = createStorageService()
        val user = testUser
        val folderId = (storageService.createFolder("PrivateEnc", user, FolderType.PRIVATE) as Success).value
        val fileContent = fileCreation(true)
        val fileId = (storageService.uploadFileInFolder(fileContent, true, user, folderId) as Success).value

        // Act: get file metadata and check cloud name
        val fileResult = storageService.getFileInFolder(user, folderId, fileId)

        // Assert: cloud name should not match original name
        when (fileResult) {
            is Success -> assertTrue(fileResult.value.fileFakeName != fileContent.blobName)
            is Failure -> fail("File should exist")
        }

        // Cleanup
        storageService.deleteFileInFolder(user, folderId, fileId)
        storageService.deleteFolder(user, folderId)
    }

    @Test
    fun `private folder - encrypted file can be downloaded and matches content type`() {
        // Arrange: create private folder and upload encrypted file
        val storageService = createStorageService()
        val user = testUser
        val folderId = (storageService.createFolder("PrivateEnc2", user, FolderType.PRIVATE) as Success).value
        val fileContent = fileCreation(true)
        val fileId = (storageService.uploadFileInFolder(fileContent, true, user, folderId) as Success).value

        // Act: download the file
        val downloadResult = storageService.downloadFileInFolder(user, folderId, fileId)

        // Assert: download succeeds and content type matches
        when (downloadResult) {
            is Success -> assertEquals(fileContent.contentType, downloadResult.value.first.mimeType)
            is Failure -> fail("Download should succeed")
        }

        // Cleanup
        storageService.deleteFileInFolder(user, folderId, fileId)
        storageService.deleteFolder(user, folderId)
    }

    @Test
    fun `private folder - encrypted file not visible to other users`() {
        // Arrange: user1 uploads encrypted file to private folder
        val storageService = createStorageService()
        val user1 = testUser
        val user2 = testUser2
        val folderId = (storageService.createFolder("PrivateEnc3", user1, FolderType.PRIVATE) as Success).value
        val fileContent = fileCreation(true)
        val fileId = (storageService.uploadFileInFolder(fileContent, true, user1, folderId) as Success).value

        // Act: user2 tries to access the file
        val fileResult = storageService.getFileInFolder(user2, folderId, fileId)

        // Assert: should fail (no access)
        assertTrue(fileResult is Failure)

        // Cleanup
        storageService.deleteFileInFolder(user1, folderId, fileId)
        storageService.deleteFolder(user1, folderId)
    }
}
