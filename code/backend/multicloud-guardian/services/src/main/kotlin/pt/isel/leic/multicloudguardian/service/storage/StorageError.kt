package pt.isel.leic.multicloudguardian.service.storage

import org.jclouds.blobstore.BlobStoreContext
import pt.isel.leic.multicloudguardian.domain.file.File
import pt.isel.leic.multicloudguardian.domain.utils.Either
import pt.isel.leic.multicloudguardian.domain.utils.Id

sealed class CreateContextJCloudError {
    data object InvalidCredential : CreateContextJCloudError()

    data object ErrorCreatingContext : CreateContextJCloudError()

    data object ErrorCreatingGlobalBucket : CreateContextJCloudError()
}

typealias CreateContextJCloudResult = Either<CreateContextJCloudError, BlobStoreContext>

sealed class

FileCreationError {
    data object ErrorCreatingGlobalBucket : FileCreationError()

    data object FileStorageError : FileCreationError()

    data object ErrorCreatingContext : FileCreationError()

    data object ErrorEncryptingFile : FileCreationError()

    data object FileNameAlreadyExists : FileCreationError()

    data object InvalidCredential : FileCreationError()
}

typealias FileCreationResult = Either<FileCreationError, Pair<Id, String>>

sealed class GetFileByIdError {
    data object FileNotFound : GetFileByIdError()

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
}
typealias DownloadFileResult = Either<DownloadFileError, Boolean>

sealed class DeleteFileError {
    data object FileNotFound : DeleteFileError()

    data object InvalidCredential : DeleteFileError()

    data object ErrorCreatingContext : DeleteFileError()

    data object ErrorCreatingGlobalBucket : DeleteFileError()

    data object ErrorDeletingFile : DeleteFileError()
}

typealias DeleteFileResult = Either<DeleteFileError, Boolean>

sealed class CreationFolderInRootError {
    data object FolderNameAlreadyExists : CreationFolderInRootError()

    data object ErrorCreatingGlobalBucket : CreationFolderInRootError()

    data object ErrorCreatingContext : CreationFolderInRootError()

    data object InvalidCredential : CreationFolderInRootError()

    data object ErrorCreatingFolder : CreationFolderInRootError()
}

typealias CreationFolderInRootResult = Either<CreationFolderInRootError, Id>

sealed class CreationFolderInSubFolderError {
    data object FolderNameAlreadyExists : CreationFolderInSubFolderError()

    data object ErrorCreatingGlobalBucket : CreationFolderInSubFolderError()

    data object ErrorCreatingContext : CreationFolderInSubFolderError()

    data object InvalidCredential : CreationFolderInSubFolderError()

    data object ErrorCreatingFolder : CreationFolderInSubFolderError()

    data object ParentFolderNotFound : CreationFolderInSubFolderError()
}

typealias CreationFolderInSubFolderResult = Either<CreationFolderInSubFolderError, Id>
