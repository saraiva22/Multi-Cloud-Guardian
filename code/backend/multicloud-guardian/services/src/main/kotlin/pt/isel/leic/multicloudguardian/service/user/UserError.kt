package pt.isel.leic.multicloudguardian.service.user

import pt.isel.leic.multicloudguardian.domain.credentials.Credentials
import pt.isel.leic.multicloudguardian.domain.user.UserStorageInfo
import pt.isel.leic.multicloudguardian.domain.utils.Either
import pt.isel.leic.multicloudguardian.domain.utils.Id

sealed class UserCreationError {
    data object UserNameAlreadyExists : UserCreationError()

    data object InsecurePassword : UserCreationError()

    data object EmailAlreadyExists : UserCreationError()
}

typealias UserCreationResult = Either<UserCreationError, Id>

sealed class UserSearchError {
    data object UserNotFound : UserSearchError()
}

typealias UserSearchResult = Either<UserSearchError, UserStorageInfo>

sealed class UserCredentialsError {
    data object UserNotFound : UserCredentialsError()
}

typealias UserCredentialsResult = Either<UserCredentialsError, Credentials>
