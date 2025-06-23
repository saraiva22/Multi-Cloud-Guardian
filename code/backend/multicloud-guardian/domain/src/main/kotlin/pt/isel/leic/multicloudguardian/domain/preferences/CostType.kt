package pt.isel.leic.multicloudguardian.domain.preferences

/**
 * Enum class representing the different cost types.
 * @property LOW Represents a low cost type.
 * @property MEDIUM Represents a medium cost type.
 * @property HIGH Represents a high cost type.
 */

enum class CostType {
    LOW,
    MEDIUM,
    HIGH,
    ;

    companion object {
        fun fromInt(value: Int): CostType = entries[value]
    }
}
