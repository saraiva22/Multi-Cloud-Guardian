package pt.isel.leic.multicloudguardian.service.user

import pt.isel.leic.multicloudguardian.domain.user.User
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

typealias UserSearchResult = Either<UserSearchError, User>
