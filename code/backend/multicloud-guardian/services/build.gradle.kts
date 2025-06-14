plugins {
    kotlin("jvm")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

group = "pt.isel.leic.multicloudguardian"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Module dependencies
    api(project(":multicloud-guardian:domain"))
    implementation(project(":multicloud-guardian:repository"))
    // To get the DI annotation
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")

    // To use Kotlin specific date and time functions
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // To use SLF4J
    implementation("org.slf4j:slf4j-api:2.0.16")

    // To use the JDBI-based repository implementation on the tests
    testImplementation(project(":multicloud-guardian:repository-jdbi"))
    testImplementation("org.jdbi:jdbi3-core:3.37.1")
    testImplementation("org.postgresql:postgresql:42.7.2")

    // Dependency for jclouds
    implementation("org.apache.jclouds:jclouds-all:2.7.0")

    // AWS S3 - SDK to generate Signed URLs
    implementation("software.amazon.awssdk:s3:2.20.143")

    // Google Cloud Storage - SDK to generate Signed URLs
    implementation("com.google.cloud:google-cloud-storage:2.27.0")

    // Azure Blob Storage - SDK to generate Signed URLs
    implementation("com.azure:azure-storage-blob:12.19.1")

    // BackBlaze B2 - HTTP ClientSDK to generate Signed URLs
    implementation("com.backblaze.b2:b2-sdk-httpclient:6.3.0")
    implementation("com.backblaze.b2:b2-sdk-core:6.3.0")

    testImplementation(kotlin("test"))
    testImplementation(project(":multicloud-guardian:host"))
    // To use WebTestClient on tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    if (System.getenv("DB_URL") == null) {
        environment("DB_URL", "jdbc:postgresql://localhost:5432/db?user=dbuser&password=changeit")
    }
}
kotlin {
    jvmToolchain(21)
}
