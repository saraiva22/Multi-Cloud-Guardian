package pt.isel.leic.multicloudguardian.domain.preferences

/** Represents the location preference of a user.
 * @property location The location preference of the user.w
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
