package pt.isel.leic.multicloudguardian.domain.preferences

enum class PerformanceType {
    LOW,
    MEDIUM,
    HIGH,
    ;

    companion object {
        fun fromInt(value: Int): PerformanceType = entries[value]
    }
}
