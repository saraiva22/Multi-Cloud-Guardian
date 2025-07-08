package pt.isel.leic.multicloudguardian.service.utils

import jakarta.inject.Named
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

@Named
class TestClock : Clock {
    // Initialized this way to reduce precision to seconds
    private var testNow: Instant = Instant.fromEpochSeconds(Clock.System.now().epochSeconds)

    fun advance(duration: Duration) {
        testNow = testNow.plus(duration)
    }

    override fun now() = testNow
}
