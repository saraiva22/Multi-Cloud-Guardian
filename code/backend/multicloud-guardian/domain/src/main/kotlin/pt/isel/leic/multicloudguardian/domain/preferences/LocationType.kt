package pt.isel.leic.multicloudguardian.domain.preferences


enum class LocationType(val location: String) {
    NORTH_AMERICA("north-america"),
    SOUTH_AMERICA("south-america"),
    EUROPE("europe"),
    OTHERS("others");

    companion object {
        fun fromString(value: String): LocationType? = entries.find { it.location == value }
    }
}


