package pt.isel.leic.multicloudguardian.domain.preferences

/** Represents the location preference of a user.
 * @property NORTH_AMERICA Represents the preference for North America.
 * @property SOUTH_AMERICA Represents the preference for South America.
 * @property EUROPE Represents the preference for Europe.
 * @property OTHERS Represents the preference for other locations.
 */

enum class LocationType {
    NORTH_AMERICA,
    SOUTH_AMERICA,
    EUROPE,
    OTHERS,
    ;

    companion object {
        fun fromInt(value: Int) = entries[value]
    }
}
