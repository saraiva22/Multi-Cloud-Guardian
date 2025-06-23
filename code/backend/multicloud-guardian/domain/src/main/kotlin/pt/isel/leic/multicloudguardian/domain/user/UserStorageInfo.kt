package pt.isel.leic.multicloudguardian.domain.user

import pt.isel.leic.multicloudguardian.domain.preferences.CostType
import pt.isel.leic.multicloudguardian.domain.preferences.LocationType
import pt.isel.leic.multicloudguardian.domain.user.components.Email
import pt.isel.leic.multicloudguardian.domain.user.components.Username
import pt.isel.leic.multicloudguardian.domain.utils.Id

data class UserStorageInfo(
    val id: Id,
    val username: Username,
    val email: Email,
    val locationType: LocationType,
    val costType: CostType,
)
