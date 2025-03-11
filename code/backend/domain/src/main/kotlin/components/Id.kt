package components

import utils.Failure
import utils.Success

class Id private constructor(val value: Int) : Component {
    companion object {
        operator fun invoke(value: Int): GetIdResult {
            return if (value > 0) Success(Id(value))
            else Failure(IdError.InvalidIdError(value))
        }
    }

    override fun toString(): String = value.toString()

    override fun hashCode(): Int = value.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Id) return false
        if (value != other.value) return false
        return true
    }
}


