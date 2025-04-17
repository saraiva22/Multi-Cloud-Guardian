package pt.isel.leic.multicloudguardian.service.storage

import org.jclouds.blobstore.BlobStoreContext
import pt.isel.leic.multicloudguardian.domain.file.File
import pt.isel.leic.multicloudguardian.domain.file.FileDownload
import pt.isel.leic.multicloudguardian.domain.utils.Either
import pt.isel.leic.multicloudguardian.domain.utils.Id

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

    data object FileIsEncrypted : GetFileByIdError()

    data object InvalidCredential : GetFileByIdError()

    data object ErrorCreatingContext : GetFileByIdError()

    data object ErrorCreatingGlobalBucket : GetFileByIdError()
}

typealias GetFileResult = Either<GetFileByIdError, Pair<File, String>>

sealed class DownloadFileError {
    data object FileNotFound : DownloadFileError()

    data object InvalidCredential : DownloadFileError()

    data object ErrorCreatingContext : DownloadFileError()

    data object ErrorCreatingGlobalBucket : DownloadFileError()

    data object ErrorDownloadingFile : DownloadFileError()

    data object ErrorDecryptingFile : DownloadFileError()

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
}

typealias DeleteFileResult = Either<DeleteFileError, Boolean>

sealed class CreationFolderError {
    data object FolderNameAlreadyExists : CreationFolderError()

    data object ErrorCreatingGlobalBucket : CreationFolderError()

    data object ErrorCreatingContext : CreationFolderError()

    data object InvalidCredential : CreationFolderError()

    data object ErrorCreatingFolder : CreationFolderError()

    data object ParentFolderNotFound : CreationFolderError()
}

typealias CreationFolderResult = Either<CreationFolderError, Id>
