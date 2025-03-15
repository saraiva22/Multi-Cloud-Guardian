package pt.isel.leic.multicloudguardian.domain.preferences


enum class PerformanceType(val performance: String) {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");


    companion object {
        fun fromString(value: String): PerformanceType? = entries.find { it.performance == value }
        fun isPerformanceType(value: String): Boolean = entries.any { it.performance == value }
    }

}
