plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    kotlin("jvm") version "2.0.10" apply false
    kotlin("plugin.spring") version "1.9.25" apply false
    id("org.springframework.boot") version "3.3.3"  apply false
    id("io.spring.dependency-management") version "1.1.6"  apply false
}
include("multicloud-guardian")
include("multicloud-guardian:domain")
include("multicloud-guardian:host")
include("multicloud-guardian:http")
include("multicloud-guardian:repository")
include("multicloud-guardian:repository-jdbi")
include("multicloud-guardian:services")

