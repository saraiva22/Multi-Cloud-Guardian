plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
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
    implementation(project(":multicloud-guardian:domain"))
    implementation(project(":multicloud-guardian:http"))
    implementation(project(":multicloud-guardian:services"))
    implementation(project(":multicloud-guardian:repository-jdbi"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // for JDBI and Postgres
    implementation("org.jdbi:jdbi3-core:3.37.1")
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("com.google.cloud.sql:postgres-socket-factory:1.16.1")

    // To use Kotlin specific date and time functions
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // To get password encode
    implementation("org.springframework.security:spring-security-core:6.3.0")

    // To use WebTestClient on tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    if (System.getenv("DB_URL") == null) {
        environment("DB_URL", "jdbc:postgresql:/localhost:5432/db?user=dbuser&password=changeit")
    }
}
kotlin {
    jvmToolchain(21)
}

/**
 * Docker related tasks
 */
task<Copy>("extractUberJar") {
    dependsOn("assemble")
    // opens the JAR containing everything...
    from(
        zipTree(
            layout.buildDirectory
                .file("libs/host-$version.jar")
                .get()
                .toString(),
        ),
    )
    // ... into the 'build/dependency' folder
    into("build/dependency")
}

val dockerImageTagJvm = "multi-cloud-guardian-jvm"
val dockerImageTagNginx = "multi-cloud-guardian-nginx"
val dockerImageTagPostgresTest = "multi-cloud-guardian-postgres-test"
val dockerImageTagUbuntu = "multi-cloud-guardian-ubuntu"

tasks.register<Exec>("buildImageJvm") {
    dependsOn("extractUberJar")
    commandLine("docker", "build", "-t", dockerImageTagJvm, "-f", "multicloudguardian/host/tests/Dockerfile-jvm", ".")
}

tasks.register<Exec>("buildImageNginx") {
    commandLine("docker", "build", "-t", dockerImageTagNginx, "-f", "multicloudguardian/host/tests/Dockerfile-nginx", ".")
}

tasks.register<Exec>("buildImagePostgresTest") {
    commandLine(
        "docker",
        "build",
        "-t",
        dockerImageTagPostgresTest,
        "-f",
        "multicloudguardian/host/tests/Dockerfile-postgres-test",
        ".",
    )
}

tasks.register<Exec>("buildImageUbuntu") {
    commandLine("docker", "build", "-t", dockerImageTagUbuntu, "-f", "multicloudguardian/host/tests/Dockerfile-ubuntu", ".")
}

tasks.register("buildImageAll") {
    dependsOn("buildImageJvm", "buildImageNginx", "buildImagePostgresTest", "buildImageUbuntu")
}

tasks.register<Exec>("allUp") {
    commandLine("docker", "compose", "up", "--force-recreate", "-d")
}

tasks.register<Exec>("allDown") {
    commandLine("docker", "compose", "down")
}
