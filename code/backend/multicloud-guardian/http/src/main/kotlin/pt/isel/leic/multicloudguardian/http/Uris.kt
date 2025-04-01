package pt.isel.leic.multicloudguardian.http

import org.springframework.web.util.UriTemplate
import java.net.URI

object Uris {
    const val PREFIX = "/api"
    const val HOME = PREFIX

    fun home(): URI = URI(HOME)

    object Users {
        const val CREATE = "$PREFIX/users"
        const val TOKEN = "$PREFIX/users/token"
        const val LOGOUT = "$PREFIX/logout"
        const val GET_BY_ID = "$PREFIX/users/{id}"
        const val HOME = "$PREFIX/me"

        fun byId(id: Int): URI = UriTemplate(GET_BY_ID).expand(id)

        fun home(): URI = URI(HOME)

        fun login(): URI = URI(TOKEN)

        fun logout(): URI = URI(LOGOUT)

        fun register(): URI = URI(CREATE)
    }

    object Files {
        const val CREATE = "$PREFIX/files"
        const val GET_FILES = "$PREFIX/files"
        const val GET_BY_ID = "$PREFIX/files/{fileId}"
        const val UPLOAD_FILE = "$PREFIX/files/upload"
        const val DOWNLOAD_FILE = "$PREFIX/files/{fileId}/download"
        const val DELETE = "$PREFIX/files/{fileId}"

        fun register(): URI = URI(CREATE)

        fun byId(id: Int): URI = UriTemplate(GET_BY_ID).expand(id)

        fun updateFile(): URI = URI(UPLOAD_FILE)

        fun getFiles(): URI = URI(GET_FILES)

        fun downloadFile(id: Int): URI = UriTemplate(DOWNLOAD_FILE).expand(id)

        fun deleteFile(id: Int): URI = UriTemplate(DELETE).expand(id)
    }

    object Folders {
        const val CREATE = "$PREFIX/folders"
        const val GET_FOLDERS = "$PREFIX/folders"
        const val GET_FOLDER_BY_ID = "$PREFIX/folders/{folderId}"
        const val GET_FILES_IN_FOLDER = "$PREFIX/folders/{folderId}/files"
        const val GET_FILE_IN_FOLDER = "$PREFIX/folders/{folderId}/files/{fileId}"
        const val UPLOAD_FILE_IN_FOLDER = "$PREFIX/folders/{folderId}/files/upload"
        const val DOWNLOAD_FOLDER = "$PREFIX/folders/{folderId}/download"
        const val DOWNLOAD_FILE_IN_FOLDER = "$PREFIX/folders/{folderId}/files/{fileId}/download"
        const val DELETE_FOLDER = "$PREFIX/folders/{folderId}"
        const val DELETE_FILE_IN_FOLDER = "$PREFIX/folders/{folderId}/files/{fileId}"

        fun register(): URI = URI(CREATE)

        fun folderById(id: Int): URI = UriTemplate(GET_FOLDERS).expand(id)

        fun getFolders(): URI = URI(GET_FOLDERS)

        fun filesInFolder(folderId: Int): URI = UriTemplate(GET_FILES_IN_FOLDER).expand(folderId)

        fun fileInFolder(
            folderId: Int,
            fileId: Int,
        ): URI = UriTemplate(GET_FILE_IN_FOLDER).expand(folderId, fileId)

        fun fileInFolderById(id: Int): URI = UriTemplate(GET_FOLDER_BY_ID).expand(id)

        fun updateFileInFolder(id: Int): URI = UriTemplate(UPLOAD_FILE_IN_FOLDER).expand(id)

        fun downloadFolder(folderId: Int): URI = UriTemplate(DOWNLOAD_FOLDER).expand(folderId)

        fun downloadFileInFolder(
            folderId: Int,
            fileId: Int,
        ): URI = UriTemplate(DOWNLOAD_FILE_IN_FOLDER).expand(folderId, fileId)

        fun deleteFolder(id: Int): URI = UriTemplate(DELETE_FOLDER).expand(id)

        fun deleteFileInFolder(
            folderId: Int,
            fileId: Int,
        ): URI = UriTemplate(DELETE_FILE_IN_FOLDER).expand(folderId, fileId)
    }
}
