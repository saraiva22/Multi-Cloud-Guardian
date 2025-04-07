package pt.isel.leic.multicloudguardian.service.storage.jclouds

import org.jclouds.blobstore.BlobStoreContext
import pt.isel.leic.multicloudguardian.domain.utils.Either

sealed class CreateGlobalBucketError {
    data object ErrorCreatingGlobalBucket : CreateGlobalBucketError()
}

typealias CreateGlobalBucketResult = Either<CreateGlobalBucketError, Boolean>

sealed class CreateBlobStorageContextError {
    data object InvalidCredential : CreateBlobStorageContextError()

    data object ErrorCreatingContext : CreateBlobStorageContextError()
}

typealias CreateBlobStorageContextResult = Either<CreateBlobStorageContextError, BlobStoreContext>

sealed class UploadBlobError {
    data object ErrorUploadingBlob : UploadBlobError()
}

typealias UploadBlobResult = Either<UploadBlobError, Boolean>

sealed class DownloadBlobError {
    data object ErrorDownloadingBlob : DownloadBlobError()
}

typealias DownloadBlobResult = Either<DownloadBlobError, Boolean>

sealed class DeleteBlobError {
    data object ErrorDeletingBlob : DeleteBlobError()
}
typealias DeleteBlobResult = Either<DeleteBlobError, Unit>

sealed class CreateFolderError {
    data object ErrorCreatingFolder : CreateFolderError()
}
typealias CreateFolderResult = Either<CreateFolderError, Boolean>
