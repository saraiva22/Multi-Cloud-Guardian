package pt.isel.leic.multicloudguardian.service.storage.jclouds

import org.jclouds.blobstore.BlobStoreContext
import pt.isel.leic.multicloudguardian.domain.utils.Either

sealed class CreateBlobStorageContext {
    data object InvalidCredential : CreateBlobStorageContext()

    data object ErrorCreatingContext : CreateBlobStorageContext()
}

typealias CreateBlobStorageContextResult = Either<CreateBlobStorageContext, BlobStoreContext>

sealed class CreateContainerError {
    data object ErrorCreatingContainer : CreateContainerError()
}

typealias CreateContainerResult = Either<CreateContainerError, Unit>
