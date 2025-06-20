package pt.isel.leic.multicloudguardian.service.storage

import org.jclouds.blobstore.BlobStoreContext
import pt.isel.leic.multicloudguardian.domain.file.File
import pt.isel.leic.multicloudguardian.domain.file.FileDownload
import pt.isel.leic.multicloudguardian.domain.folder.Folder
import pt.isel.leic.multicloudguardian.domain.utils.Either
import pt.isel.leic.multicloudguardian.domain.utils.Id
import pt.isel.leic.multicloudguardian.domain.utils.PageResult

sealed class CreateContextJCloudError {
    data object InvalidCredential : CreateContextJCloudError()

    data object ErrorCreatingContext : CreateContextJCloudError()

    data object ErrorCreatingGlobalBucket : CreateContextJCloudError()
}

typealias CreateContextJCloudResult = Either<CreateContextJCloudError, BlobStoreContext>

sealed class UploadFileError {
    data object ErrorCreatingGlobalBucketUpload : UploadFileError()

    data object FileStorageError : UploadFileError()

    data object ErrorCreatingContextUpload : UploadFileError()

    data object FileNameAlreadyExists : UploadFileError()

    data object InvalidCredential : UploadFileError()

    data object ParentFolderNotFound : UploadFileError()
}

typealias UploadFileResult = Either<UploadFileError, Id>

sealed class GetFileByIdError {
    data object FileNotFound : GetFileByIdError()
}

typealias GetFileResult = Either<GetFileByIdError, File>

sealed class CreateTempUrlFileError {
    data object FileNotFound : CreateTempUrlFileError()

    data object EncryptedFile : CreateTempUrlFileError()

    data object InvalidCredential : CreateTempUrlFileError()

    data object ErrorCreatingContext : CreateTempUrlFileError()

    data object ErrorCreatingGlobalBucket : CreateTempUrlFileError()
}

typealias CreateTempUrlFileResult = Either<CreateTempUrlFileError, Pair<File, String>>

sealed class DownloadFileError {
    data object FileNotFound : DownloadFileError()

    data object InvalidCredential : DownloadFileError()

    data object ErrorCreatingContext : DownloadFileError()

    data object ErrorCreatingGlobalBucket : DownloadFileError()

    data object ErrorDownloadingFile : DownloadFileError()

    data object InvalidKey : DownloadFileError()

    data object ParentFolderNotFound : DownloadFileError()
}
typealias DownloadFileResult = Either<DownloadFileError, Pair<FileDownload, String?>>

sealed class DeleteFileError {
    data object FileNotFound : DeleteFileError()

    data object InvalidCredential : DeleteFileError()

    data object ErrorCreatingContext : DeleteFileError()

    data object ErrorCreatingGlobalBucket : DeleteFileError()

    data object ErrorDeletingFile : DeleteFileError()

    data object ParentFolderNotFound : DeleteFileError()
}

typealias DeleteFileResult = Either<DeleteFileError, Boolean>

sealed class DeleteFolderError {
    data object FolderNotFound : DeleteFolderError()

    data object InvalidCredential : DeleteFolderError()

    data object ErrorCreatingContext : DeleteFolderError()

    data object ErrorCreatingGlobalBucket : DeleteFolderError()

    data object ErrorDeletingFolder : DeleteFolderError()
}

typealias DeleteFolderResult = Either<DeleteFolderError, Boolean>

sealed class CreationFolderError {
    data object FolderNameAlreadyExists : CreationFolderError()

    data object ErrorCreatingGlobalBucket : CreationFolderError()

    data object ErrorCreatingContext : CreationFolderError()

    data object InvalidCredential : CreationFolderError()

    data object ErrorCreatingFolder : CreationFolderError()

    data object ParentFolderNotFound : CreationFolderError()
}

typealias CreationFolderResult = Either<CreationFolderError, Id>

sealed class GetFolderByIdError {
    data object FolderNotFound : GetFolderByIdError()
}

typealias GetFolderResult = Either<GetFolderByIdError, Folder>

sealed class GetFileInFolderError {
    data object FolderNotFound : GetFileInFolderError()

    data object FileNotFound : GetFileInFolderError()
}

typealias GetFileInFolderResult = Either<GetFileInFolderError, File>

sealed class GetFilesInFolderError {
    data object FolderNotFound : GetFilesInFolderError()
}

typealias GetFilesInFolderResult = Either<GetFilesInFolderError, PageResult<File>>

sealed class GetFoldersInFolderError {
    data object FolderNotFound : GetFoldersInFolderError()
}

typealias GetFoldersInFolderResult = Either<GetFoldersInFolderError, PageResult<Folder>>

sealed class MoveFileError {
    data object FileNotFound : MoveFileError()

    data object FolderNotFound : MoveFileError()

    data object FileNameAlreadyExists : MoveFileError()

    data object ErrorCreatingGlobalBucket : MoveFileError()

    data object ErrorCreatingContext : MoveFileError()

    data object InvalidCredential : MoveFileError()

    data object MoveBlobError : MoveFileError()

    data object MoveBlobNotFound : MoveFileError()
}

typealias MoveFileResult = Either<MoveFileError, Boolean>

sealed class InviteFolderError {
    data object UserAlreadyInFolder : InviteFolderError()

    data object FolderNotFound : InviteFolderError()

    data object FolderIsPrivate : InviteFolderError()

    data object GuestNotFound : InviteFolderError()

    data object UserIsNotOwner : InviteFolderError()
}

typealias InviteFolderResult = Either<InviteFolderError, Id>

sealed class ValidateFolderInviteError {
    data object UserAlreadyInFolder : ValidateFolderInviteError()

    data object InvalidInvite : ValidateFolderInviteError()

    data object FolderIsPrivate : ValidateFolderInviteError()

    data object FolderNotFound : ValidateFolderInviteError()
}

typealias ValidateFolderInviteResult = Either<ValidateFolderInviteError, Folder>

sealed class LeaveFolderError {
    data object UserNotInFolder : LeaveFolderError()

    data object FolderNotFound : LeaveFolderError()

    data object FolderIsPrivate : LeaveFolderError()

    data object ErrorLeavingFolder : LeaveFolderError()
}

typealias LeaveFolderResult = Either<LeaveFolderError, Unit>
