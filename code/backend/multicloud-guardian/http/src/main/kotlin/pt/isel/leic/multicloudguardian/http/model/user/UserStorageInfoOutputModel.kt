package pt.isel.leic.multicloudguardian.http.model.user

import pt.isel.leic.multicloudguardian.domain.preferences.LocationType
import pt.isel.leic.multicloudguardian.domain.preferences.PerformanceType

data class UserStorageInfoOutputModel(
    val id: Int,
    val username: String,
    val email: String,
    val locationType: LocationType,
    val performanceType: PerformanceType,
)
