package pt.isel.leic.multicloudguardian.domain.preferences

import org.springframework.stereotype.Component
import pt.isel.leic.multicloudguardian.domain.provider.ProviderType

@Component
class PreferencesDomain {
    fun associationProvider(
        cost: CostType,
        location: LocationType,
    ): ProviderType =
        when (cost) {
            CostType.LOW -> ProviderType.BACK_BLAZE
            CostType.MEDIUM -> ProviderType.GOOGLE
            else ->
                when (location) {
                    LocationType.NORTH_AMERICA -> ProviderType.AZURE
                    LocationType.EUROPE -> ProviderType.AZURE
                    else -> ProviderType.AMAZON
                }
        }
}
