package pt.isel.leic.multicloudguardian.domain.preferences

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pt.isel.leic.multicloudguardian.domain.components.Id
import pt.isel.leic.multicloudguardian.domain.provider.ProviderType
import pt.isel.leic.multicloudguardian.domain.utils.get

import kotlin.test.assertEquals

class PreferencesDomainTests {

    private val userId = Id(1).get()
    private val prefId = Id(1).get()


    @Test
    fun `example Test`() {
        // give: Set up the performance and location preferences
        val performance = PerformanceType.MEDIUM
        val location = LocationType.EUROPE
        val preferences = Preferences(prefId, userId, location, performance)

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
            val preferences = Preferences(userId,prefId,location, performance)

            // when: Associate the provider based on preferences
            val prefDomain = PreferencesDomain()
            val providerType = prefDomain.associationProvider(preferences)

            // then: Verify that the correct provider is produced
            assertEquals(ProviderType.BACKBLAZE, providerType)
        }
    }

    @Test
    fun `Failure test`(){
        // give: Set up the performance and location preference
        val performance = PerformanceType.fromString("low")
        val location = LocationType.fromString("south-america")
        if (performance != null && location != null) {
            val preferences = Preferences(userId,prefId,location, performance)

            // when: Associate the provider based on preferences
            val prefDomain = PreferencesDomain()
            val providerType = prefDomain.associationProvider(preferences)

            // then: Verify that the correct provider is produced
            assertEquals(ProviderType.BACKBLAZE, providerType)
        }
    }

    @Test
    fun `Exception test`(){
       val exception = assertThrows<IllegalArgumentException>{
           Id(-1).get()
       }
        assertEquals(exception.message ,"Cannot get value from a failure" )

    }
}