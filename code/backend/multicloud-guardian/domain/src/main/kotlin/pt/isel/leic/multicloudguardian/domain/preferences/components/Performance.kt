package pt.isel.leic.multicloudguardian.domain.preferences.components

import pt.isel.leic.multicloudguardian.domain.components.Component
import pt.isel.leic.multicloudguardian.domain.utils.Either
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Success

class Performance private constructor(val value: String) : Component {

    companion object {
        private val validPerformance = PerformanceType.entries.associateBy { it.value }

        operator fun invoke(value: String): Either<PerformanceError, Performance> =
            if (value in validPerformance) Success(Performance(value))
            else Failure(PerformanceError.InvalidPerformance)

        fun fromString(value: String): PerformanceType? = validPerformance[value]
    }

    enum class PerformanceType(val value: String) {
        LOW("low"),
        MEDIUM("medium"),
        HIGH("high");
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Performance) return false
        if (value != other.value) return false
        return true
    }
}

sealed class PerformanceError {
    data object InvalidPerformance : PerformanceError()
}