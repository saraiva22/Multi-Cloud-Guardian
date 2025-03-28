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

sealed class FileCreationError {
    data object ErrorCreatingGlobalBucket : FileCreationError()

    data object FileStorageError : FileCreationError()

    data object ErrorCreatingContext : FileCreationError()

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

typealias GetFileResult = Either<GetFileByIdError, File>
