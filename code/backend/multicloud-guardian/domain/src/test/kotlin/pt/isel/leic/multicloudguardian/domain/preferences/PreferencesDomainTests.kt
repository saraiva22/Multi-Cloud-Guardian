package pt.isel.leic.multicloudguardian.domain.preferences

import org.junit.jupiter.api.Test
import pt.isel.leic.multicloudguardian.domain.provider.ProviderType
import kotlin.test.assertEquals

class PreferencesDomainTests {
    @Test
    fun `Verification of the association of the provider based on preferences - Performance LOW`() {
        // give: Set up the performance and location preferences
        val performance = CostType.LOW
        val location = LocationType.EUROPE

        // and:
        val performanceString = "low"
        val locationString = "europe"

        // when: Associate the provider based on preferences
        val prefDomain = PreferencesDomain()
        val providerType = prefDomain.associationProvider(performance, location)

        // then: Verify that the correct provider is produced
        assertEquals(ProviderType.BACK_BLAZE, providerType)
    }

    @Test
    fun `Verification of the association of the provider based on preferences - Performance Medium`() {
        // give: Set up the performance and location preferences
        val performance = CostType.MEDIUM
        val location = LocationType.EUROPE

        // when: Associate the provider based on preferences
        val prefDomain = PreferencesDomain()
        val providerType = prefDomain.associationProvider(performance, location)

        // then: Verify that the correct provider is produced
        assertEquals(ProviderType.GOOGLE, providerType)
    }

    @Test
    fun `Verification of the association of the provider based on preferences - Performance High and Location North-America and Europe `() {
        // give: Set up the performance and location preferences
        val performance = CostType.HIGH
        val location = LocationType.NORTH_AMERICA

        val location1 = LocationType.EUROPE

        // when: Associate the provider based on preferences
        val prefDomain = PreferencesDomain()
        val providerType = prefDomain.associationProvider(performance, location)
        val providerType1 = prefDomain.associationProvider(performance, location1)

        // then: Verify that the correct provider is produced
        assertEquals(ProviderType.AZURE, providerType)
        assertEquals(ProviderType.AZURE, providerType1)
    }

    @Test
    fun `Verification of the association of the provider based on preferences - Performance High and Location South-America and Others `() {
        // give: Set up the performance and location preferences
        val performance = CostType.HIGH
        val location = LocationType.SOUTH_AMERICA

        val location1 = LocationType.OTHERS

        // when: Associate the provider based on preferences
        val prefDomain = PreferencesDomain()
        val providerType = prefDomain.associationProvider(performance, location)
        val providerType1 = prefDomain.associationProvider(performance, location1)

        // then: Verify that the correct provider is produced
        assertEquals(ProviderType.AMAZON, providerType)
        assertEquals(ProviderType.AMAZON, providerType1)
    }
}
