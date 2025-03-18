package pt.isel.leic.multicloudguardian.domain.preferences

import org.junit.jupiter.api.Test
import pt.isel.leic.multicloudguardian.domain.components.Id
import pt.isel.leic.multicloudguardian.domain.provider.ProviderType
import pt.isel.leic.multicloudguardian.domain.utils.get
import kotlin.test.assertEquals

class PreferencesDomainTests {

    @Test
    fun `Verification of the association of the provider based on preferences - Performance LOW`() {
        // give: Set up the performance and location preferences
        val performance = PerformanceType.LOW
        val location = LocationType.EUROPE

        // and:
        val performanceString = "low"
        val locationString = "europe"

        // when: Verify if the performance and location are valid

        val validatePerformance = PerformanceType.isPerformanceType(performanceString)
        val validateLocation = LocationType.isLocationType(locationString)

        // then: Verify that the performance and location are valid
        assertEquals(true, validatePerformance)
        assertEquals(true, validateLocation)

        // when: Associate the provider based on preferences
        val prefDomain = PreferencesDomain()
        val providerType = prefDomain.associationProvider(performance, location)

        // then: Verify that the correct provider is produced
        assertEquals(ProviderType.BACKBLAZE, providerType)
        assertEquals(performance, PerformanceType.fromString(performanceString))
        assertEquals(location, LocationType.fromString(locationString))

    }


    @Test
    fun `Verification of the association of the provider based on preferences - Performance Medium`() {
        // give: Set up the performance and location preferences
        val performance = PerformanceType.MEDIUM
        val location = LocationType.EUROPE

        // and:
        val performanceString = "medium"
        val locationString = "europe"

        // when: Verify if the performance and location are valid
        val validatePerformance = PerformanceType.isPerformanceType(performanceString)
        val validateLocation = LocationType.isLocationType(locationString)

        // then: Verify that the performance and location are valid
        assertEquals(true, validatePerformance)
        assertEquals(true, validateLocation)

        // when: Associate the provider based on preferences
        val prefDomain = PreferencesDomain()
        val providerType = prefDomain.associationProvider(performance, location)

        // then: Verify that the correct provider is produced
        assertEquals(ProviderType.GOOGLE, providerType)
        assertEquals(performance, PerformanceType.fromString(performanceString))
        assertEquals(location, LocationType.fromString(locationString))

    }

    @Test
    fun `Verification of the association of the provider based on preferences - Performance High and Location North-America and Europe `() {
        // give: Set up the performance and location preferences
        val performance = PerformanceType.HIGH
        val location = LocationType.NORTH_AMERICA

        val location1 = LocationType.EUROPE

        // and:
        val performanceString = "high"
        val locationString = "north-america"
        val locationString1 = "europe"

        // when: Verify if the performance and location are valid
        val validatePerformance = PerformanceType.isPerformanceType(performanceString)
        val validateLocation = LocationType.isLocationType(locationString)
        val validateLocation1 = LocationType.isLocationType(locationString1)

        // then: Verify that the performance and location are valid
        assertEquals(true, validatePerformance)
        assertEquals(true, validateLocation)
        assertEquals(true, validateLocation1)

        // when: Associate the provider based on preferences
        val prefDomain = PreferencesDomain()
        val providerType = prefDomain.associationProvider(performance, location)
        val providerType1 = prefDomain.associationProvider(performance, location1)

        // then: Verify that the correct provider is produced
        assertEquals(ProviderType.AZURE, providerType)
        assertEquals(ProviderType.AZURE, providerType1)
        assertEquals(performance, PerformanceType.fromString(performanceString))
        assertEquals(location, LocationType.fromString(locationString))
    }

    @Test
    fun `Verification of the association of the provider based on preferences - Performance High and Location South-America and Others `() {
        // give: Set up the performance and location preferences
        val performance = PerformanceType.HIGH
        val location = LocationType.SOUTH_AMERICA

        val location1 = LocationType.OTHERS

        // and:
        val performanceString = "high"
        val locationString = "south-america"
        val locationString1 = "others"

        // when: Verify if the performance and location are valid
        val validatePerformance = PerformanceType.isPerformanceType(performanceString)
        val validateLocation = LocationType.isLocationType(locationString)
        val validateLocation1 = LocationType.isLocationType(locationString1)

        // then: Verify that the performance and location are valid
        assertEquals(true, validatePerformance)
        assertEquals(true, validateLocation)
        assertEquals(true, validateLocation1)

        // when: Associate the provider based on preferences
        val prefDomain = PreferencesDomain()
        val providerType = prefDomain.associationProvider(performance,location)
        val providerType1 = prefDomain.associationProvider(performance,location1)
        // then: Verify that the correct provider is produced
        assertEquals(ProviderType.AMAZON, providerType)
        assertEquals(ProviderType.AMAZON, providerType1)
        assertEquals(performance, PerformanceType.fromString(performanceString))
        assertEquals(location, LocationType.fromString(locationString))
    }

    companion object {

    }
}