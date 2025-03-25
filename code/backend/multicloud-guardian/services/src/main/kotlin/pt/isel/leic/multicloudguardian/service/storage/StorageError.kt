package pt.isel.leic.multicloudguardian.service.storage

import pt.isel.leic.multicloudguardian.domain.utils.Either
import pt.isel.leic.multicloudguardian.domain.utils.Id

sealed class FileCreationError {
    data object ErrorCreatingGlobalBucket : FileCreationError()

    data object FileStorageError : FileCreationError()

    data object ErrorCreatingContext : FileCreationError()

    data object FileNameAlreadyExists : FileCreationError()
}

typealias FileCreationResult = Either<FileCreationError, Id>
