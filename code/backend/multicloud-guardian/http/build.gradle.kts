plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot") version "3.3.3"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

group = "pt.isel.leic.multicloudguardian"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Module dependencies
    implementation(project(":multicloud-guardian:domain"))
    implementation(project(":multicloud-guardian:services"))

    // To use Spring MVC and the Servlet API
    implementation("org.springframework:spring-webmvc:6.1.13")
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")

    // for Spring validation
    implementation("org.springframework.boot:spring-boot-starter-validation:3.0.4")

    // To use SLF4J
    implementation("org.slf4j:slf4j-api:2.0.16")

    // for jackson json library
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")

    // To use Kotlin specific date and time functions
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
