package pt.isel.leic.multicloudguardian.domain.components

import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Success

class PositiveValue private constructor(val value: Int) : Component {
    companion object {
        operator fun invoke(value: Int): PositiveValueResult =
            if (value > 0) {
                Success(PositiveValue(value))
            } else {
                Failure(PositiveValueError.InvalidPositiveValue(value))
            }
    }

    override fun hashCode(): Int = value

    override fun toString(): String = value.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PositiveValue) return false
        if (value != other.value) return false
        return true
    }
}