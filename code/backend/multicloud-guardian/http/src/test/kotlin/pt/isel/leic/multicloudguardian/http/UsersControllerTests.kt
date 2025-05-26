package pt.isel.leic.multicloudguardian.http

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient
import pt.isel.leic.multicloudguardian.domain.preferences.LocationType
import pt.isel.leic.multicloudguardian.domain.preferences.PerformanceType
import pt.isel.leic.multicloudguardian.http.model.TokenResponse
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UsersControllerTests {
    // One of the very few places where we use property injection
    @LocalServerPort
    var port: Int = 0

    @Test
    fun `can create an user`() {
        // given: an HTTP client

        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port/api").build()

        // and: a random user
        val username = newTestUserName()
        val password = newTestPassword()
        val email = newTestEmail()

        // when: creating a user
        // then: the response is 201 Created

        client
            .post()
            .uri("/users")
            .bodyValue(
                mapOf(
                    "username" to username,
                    "email" to email,
                    "password" to password,
                    "location" to newTestLocation(),
                    "performance" to newTestPerformance(),
                ),
            ).exchange()
            .expectStatus()
            .isCreated
            .expectHeader()
            .value("location") {
                assertTrue(it.startsWith("/api/users/"))
            }
    }

    companion object {
        private fun newTestUserName() = "user-${abs(Random.nextLong())}"

        private fun newTestEmail() = "email-${abs(Random.nextLong())}@example.com"

        private fun newTestPassword() = "Password@${abs(Random.nextInt())}"

        private fun newTestLocation() = LocationType.entries.random()

        private fun newTestPerformance() = PerformanceType.entries.random()

        private const val ADMIN_USERNAME = "Test2425"
        private const val ADMIN_EMAIL = "admin2425@gmail.com"
        private const val ADMIN_PASSWORD = "Test_2425"

        private fun newTestSalt() = "salt-${abs(Random.nextLong())}"

        private fun newTestIteration() = abs(Random.nextInt(15000, 20000))

        val PASSWORD = newTestPassword()

        private fun getTokenUserAdmin(client: WebTestClient): TokenResponse =
            client
                .post()
                .uri("/users/token")
                .bodyValue(
                    mapOf(
                        "username" to ADMIN_USERNAME,
                        "password" to ADMIN_PASSWORD,
                    ),
                ).exchange()
                .expectStatus()
                .isOk
                .expectBody(TokenResponse::class.java)
                .returnResult()
                .responseBody!!
    }
}
