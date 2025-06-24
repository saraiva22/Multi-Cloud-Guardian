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
        const val GET_BY_USERNAME = "$PREFIX/users/info"
        const val CREDENTIALS = "$PREFIX/users/credentials"
        const val STORAGE_DETAILS = "$PREFIX/users/storage"
        const val SEARCH_USERS = "$PREFIX/users"
        const val HOME = "$PREFIX/me"
        const val NOTIFICATIONS = "$PREFIX/users/notifications"

        fun byId(id: Int): URI = UriTemplate(GET_BY_ID).expand(id)

        fun byUsername(username: String): URI = UriTemplate(GET_BY_USERNAME).expand(username)

        fun credentials(): URI = URI(CREDENTIALS)

        fun storage(): URI = URI(STORAGE_DETAILS)

        fun home(): URI = URI(HOME)

        fun login(): URI = URI(TOKEN)

        fun logout(): URI = URI(LOGOUT)

        fun register(): URI = URI(CREATE)
    }

    object Files {
        const val UPLOAD = "$PREFIX/files"
        const val GET_FILES = "$PREFIX/files"
        const val GET_BY_ID = "$PREFIX/files/{fileId}"
        const val CREATE_URL = "$PREFIX/files/{fileId}/temp-url"
        const val DOWNLOAD_FILE = "$PREFIX/files/{fileId}/download"
        const val DELETE = "$PREFIX/files/{fileId}"
        const val MOVE_FILE = "$PREFIX/files/{fileId}/move"

        fun uploadFile(): URI = URI(UPLOAD)

        fun byId(id: Int): URI = UriTemplate(GET_BY_ID).expand(id)

        fun createUrl(id: Int): URI = UriTemplate(CREATE_URL).expand(id)

        fun downloadFile(id: Int): URI = UriTemplate(DOWNLOAD_FILE).expand(id)

        fun deleteFile(id: Int): URI = UriTemplate(DELETE).expand(id)

        fun moveFile(id: Int): URI = UriTemplate(MOVE_FILE).expand(id)
    }

    object Folders {
        const val CREATE = "$PREFIX/folders"
        const val CREATE_FOLDER_IN_FOLDER = "$PREFIX/folders/{folderId}"
        const val GET_FOLDERS = "$PREFIX/folders"
        const val GET_FOLDER_BY_ID = "$PREFIX/folders/{folderId}"
        const val GET_FOLDERS_IN_FOLDER = "$PREFIX/folders/{folderId}/folders"
        const val GET_FILES_IN_FOLDER = "$PREFIX/folders/{folderId}/files"
        const val GET_FILE_IN_FOLDER = "$PREFIX/folders/{folderId}/files/{fileId}"
        const val UPLOAD_FILE_IN_FOLDER = "$PREFIX/folders/{folderId}/files"
        const val DOWNLOAD_FILE_IN_FOLDER = "$PREFIX/folders/{folderId}/files/{fileId}/download"
        const val DELETE_FOLDER = "$PREFIX/folders/{folderId}"
        const val DELETE_FILE_IN_FOLDER = "$PREFIX/folders/{folderId}/files/{fileId}"
        const val CREATE_INVITE_FOLDER = "$PREFIX/folders/{folderId}/invites"
        const val VALIDATE_FOLDER_INVITE = "$PREFIX/folders/{folderId}/invites/{inviteId}"
        const val RECEIVED_FOLDER_INVITES = "$PREFIX/folders/invites/received"
        const val SENT_FOLDER_INVITES = "$PREFIX/folders/invites/sent"
        const val LEAVE_SHARED_FOLDER = "$PREFIX/folders/{folderId}/leave"

        fun register(): URI = URI(CREATE)

        fun folderById(id: Int): URI = UriTemplate(GET_FOLDER_BY_ID).expand(id)

        fun foldersInFolder(folderId: Int): URI = UriTemplate(GET_FOLDERS_IN_FOLDER).expand(folderId)

        fun filesInFolder(folderId: Int): URI = UriTemplate(GET_FILES_IN_FOLDER).expand(folderId)

        fun createFolderInFolder(folderId: Int): URI = UriTemplate(CREATE_FOLDER_IN_FOLDER).expand(folderId)

        fun fileInFolder(
            folderId: Int,
            fileId: Int,
        ): URI = UriTemplate(GET_FILE_IN_FOLDER).expand(folderId, fileId)

        fun uploadFileInFolder(folderId: Int): URI = UriTemplate(UPLOAD_FILE_IN_FOLDER).expand(folderId)

        fun downloadFileInFolder(
            folderId: Int,
            fileId: Int,
        ): URI = UriTemplate(DOWNLOAD_FILE_IN_FOLDER).expand(folderId, fileId)

        fun deleteFolder(id: Int): URI = UriTemplate(DELETE_FOLDER).expand(id)

        fun deleteFileInFolder(
            folderId: Int,
            fileId: Int,
        ): URI = UriTemplate(DELETE_FILE_IN_FOLDER).expand(folderId, fileId)

        fun inviteFolder(folderId: Int): URI = UriTemplate(CREATE_INVITE_FOLDER).expand(folderId)

        fun validateFolderInvite(
            folderId: Int,
            inviteId: Int,
        ): URI = UriTemplate(VALIDATE_FOLDER_INVITE).expand(folderId, inviteId)

        fun leaveFolder(folderId: Int): URI = UriTemplate(LEAVE_SHARED_FOLDER).expand(folderId)
    }
}
