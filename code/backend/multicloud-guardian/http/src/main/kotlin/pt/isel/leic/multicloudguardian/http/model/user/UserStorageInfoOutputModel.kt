package pt.isel.leic.multicloudguardian.http.model.user

import pt.isel.leic.multicloudguardian.domain.preferences.CostType
import pt.isel.leic.multicloudguardian.domain.preferences.LocationType

data class UserStorageInfoOutputModel(
    val id: Int,
    val username: String,
    val email: String,
    val locationType: LocationType,
    val costType: CostType,
)
