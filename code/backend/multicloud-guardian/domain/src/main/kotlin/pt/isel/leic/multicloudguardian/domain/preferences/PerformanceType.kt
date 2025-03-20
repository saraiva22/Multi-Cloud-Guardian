package pt.isel.leic.multicloudguardian.domain.preferences

/**
 * Enum class representing the different performance types.
 * @property LOW Represents a low performance type.
 * @property MEDIUM Represents a medium performance type.
 * @property HIGH Represents a high performance type.
 */

enum class PerformanceType {
    LOW,
    MEDIUM,
    HIGH,
    ;

    companion object {
        fun fromInt(value: Int): PerformanceType = entries[value]
    }
}
