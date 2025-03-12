package pt.isel.leic.multicloudguardian.domain.provider

import pt.isel.leic.multicloudguardian.domain.components.Component
import pt.isel.leic.multicloudguardian.domain.utils.Either
import pt.isel.leic.multicloudguardian.domain.utils.Failure
import pt.isel.leic.multicloudguardian.domain.utils.Success

class Provider private constructor(val value: String) : Component {
    companion object {
        private val validProviders = ProviderType.entries.associateBy { it.value }

        operator fun invoke(value: String): Either<ProviderError, Provider> =
            if (value in validProviders) Success(Provider(value))
            else Failure(ProviderError.InvalidProvider)

        fun fromString(value: String): ProviderType? = validProviders[value]
    }

    enum class ProviderType(val value: String) {
        AMAZON("aws-s3"),
        AZURE("azureblob"),
        GOOGLE("google-cloud-storage"),
        BACKBLAZE("b2")
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Provider) return false
        if (value != other.value) return false
        return true
    }
}

sealed class ProviderError {
    data object InvalidProvider : ProviderError()
}
