package pt.isel.leic.multicloudguardian.domain.preferences

/** Represents the location preference of a user.
 * @property location The location preference of the user.w
 */

enum class LocationType(val location: String) {
    NORTH_AMERICA("north-america"),
    SOUTH_AMERICA("south-america"),
    EUROPE("europe"),
    OTHERS("others");

    companion object {
        fun fromString(value: String): LocationType? = entries.find { it.location == value }
        fun isLocationType(value: String): Boolean = entries.any { it.location == value }
    }
}


