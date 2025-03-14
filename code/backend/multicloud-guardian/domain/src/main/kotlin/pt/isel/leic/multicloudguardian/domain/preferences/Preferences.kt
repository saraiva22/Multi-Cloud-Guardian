package pt.isel.leic.multicloudguardian.domain.preferences

import pt.isel.leic.multicloudguardian.domain.components.Id

/** Represents the preferences of a user.
 * @property preferencesId The unique identifier for the preferences.
 * @property userId The unique identifier for the user.
 * @property location The location preference of the user.
 * @property performance The performance preference of the user.
 */

data class Preferences(
    val preferencesId: Id,
    val userId: Id,
    val location: LocationType,
    val performance: PerformanceType
)



