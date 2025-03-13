package pt.isel.leic.multicloudguardian.domain.preferences

import org.junit.jupiter.api.Test
import pt.isel.leic.multicloudguardian.domain.provider.ProviderType

import kotlin.test.assertEquals

class PreferencesDomainTests {
    @Test
    fun `example Test`() {
        // give: Set up the performance and location preferences
        val performance = PerformanceType.MEDIUM
        val location = LocationType.EUROPE
        val preferences = Preferences(location, performance)

        // when: Associate the provider based on preferences
        val prefDomain = PreferencesDomain()
        val providerType = prefDomain.associationProvider(preferences)

        // then: Verify that the correct provider is produced
        assertEquals(ProviderType.GOOGLE, providerType)
    }

    @Test
    fun test1() {
        // give: Set up the performance and location preference
        val performance = PerformanceType.fromString("low")
        val location = LocationType.fromString("south-america")
        if (performance != null && location != null) {
            val preferences = Preferences(location, performance)

            // when: Associate the provider based on preferences
            val prefDomain = PreferencesDomain()
            val providerType = prefDomain.associationProvider(preferences)

            // then: Verify that the correct provider is produced
            assertEquals(ProviderType.BACKBLAZE, providerType)
        }

    }
}