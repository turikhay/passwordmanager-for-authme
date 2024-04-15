import org.gradle.api.artifacts.Configuration
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue

plugins {
    id("kotlin-convention")
}

val includeable: Configuration by configurations.creating {}

val maybeInclude: Configuration by configurations.creating {}

val exposedVersion = "0.49.0"
dependencies {
    includeable("io.github.oshai:kotlin-logging-jvm:6.0.3")
    includeable("org.jetbrains.exposed:exposed-core:$exposedVersion")
    includeable("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    maybeInclude("org.xerial:sqlite-jdbc:3.45.2.0")
}