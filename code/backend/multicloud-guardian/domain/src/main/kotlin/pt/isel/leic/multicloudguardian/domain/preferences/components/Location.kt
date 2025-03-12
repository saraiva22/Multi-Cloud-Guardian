package pt.isel.leic.multicloudguardian.domain.preferences.components

import pt.isel.leic.multicloudguardian.domain.components.Component
import pt.isel.leic.multicloudguardian.domain.utils.Either
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Success

class Location private constructor(val value: String) : Component {
    companion object {
        private val validLocation = LocationType.entries.associateBy { it.value }

        operator fun invoke(value: String): Either<LocationError, Location> =
            if (value in validLocation) Success(Location(value))
            else Failure(LocationError.InvalidLocation)

        fun fromString(value: String): LocationType? = validLocation[value]
    }

    enum class LocationType(val value: String) {
        NORTH_AMERICA("north-america"),
        SOUTH_AMERICA("south-america"),
        EUROPE("europe"),
        OTHERS("others")
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Location) return false
        if (value != other.value) return false
        return true
    }
}

sealed class LocationError {
    data object InvalidLocation : LocationError()
}