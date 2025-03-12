package pt.isel.leic.multicloudguardian.domain.preferences

import org.springframework.stereotype.Component
import pt.isel.leic.multicloudguardian.domain.preferences.components.Location
import pt.isel.leic.multicloudguardian.domain.preferences.components.Performance
import pt.isel.leic.multicloudguardian.domain.provider.Provider


class PreferencesDomain(
    private val preferences: Preferences
) {

    fun associationProvider(pref: Preferences): Provider.ProviderType {
        return when (pref.performance.value) {
            Performance.PerformanceType.LOW.value -> Provider.ProviderType.BACKBLAZE
            Performance.PerformanceType.MEDIUM.value -> Provider.ProviderType.GOOGLE
            else -> when (pref.location.value) {
                Location.LocationType.NORTH_AMERICA.value -> Provider.ProviderType.AZURE
                Location.LocationType.EUROPE.value -> Provider.ProviderType.AZURE
                else -> Provider.ProviderType.AMAZON
            }
        }
    }
}