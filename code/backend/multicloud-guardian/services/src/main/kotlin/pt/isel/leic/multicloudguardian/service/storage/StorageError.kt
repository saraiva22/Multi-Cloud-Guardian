package pt.isel.leic.multicloudguardian.service.storage

import pt.isel.leic.multicloudguardian.domain.utils.Either
import pt.isel.leic.multicloudguardian.domain.utils.Id

sealed class FileCreationError {
    data object FileError : FileCreationError()
}

typealias FileCreationResult = Either<FileCreationError, Id>
