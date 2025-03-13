package pt.isel.leic.multicloudguardian.domain.preferences

import org.springframework.stereotype.Component
import pt.isel.leic.multicloudguardian.domain.provider.ProviderType

@Component
class PreferencesDomain {

    fun associationProvider(pref: Preferences): ProviderType {
        return when (pref.performance) {
            PerformanceType.LOW -> ProviderType.BACKBLAZE
            PerformanceType.MEDIUM -> ProviderType.GOOGLE
            else -> when (pref.location) {
                LocationType.NORTH_AMERICA -> ProviderType.AZURE
                LocationType.EUROPE -> ProviderType.AZURE
                else -> ProviderType.AMAZON
            }
        }
    }
}