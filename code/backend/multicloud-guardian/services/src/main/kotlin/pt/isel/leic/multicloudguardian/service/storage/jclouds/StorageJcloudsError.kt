package pt.isel.leic.multicloudguardian.service.storage.jclouds

import org.jclouds.blobstore.BlobStoreContext
import pt.isel.leic.multicloudguardian.domain.utils.Either

sealed class CreateGlobalBucketError {
    data object ErrorCreatingGlobalBucket : CreateGlobalBucketError()
}

typealias CreateGlobalBucketResult = Either<CreateGlobalBucketError, Boolean>

sealed class CreateBlobStorageContext {
    data object InvalidCredential : CreateBlobStorageContext()

    data object ErrorCreatingContext : CreateBlobStorageContext()
}

typealias CreateBlobStorageContextResult = Either<CreateBlobStorageContext, BlobStoreContext>
