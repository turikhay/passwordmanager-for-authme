import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    maven {
        name = "Mojang"
        url = uri("https://libraries.minecraft.net/")
        content {
            includeGroup("com.mojang")
        }
    }
    maven {
        name = "Velocity"
        url = uri("https://nexus.velocitypowered.com/repository/maven-public/")
        content {
            includeGroup("org.spongepowered")
        }
    }
    maven {
        name = "sonatype-oss-snapshots1"
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        mavenContent {
            snapshotsOnly()
        }
    }
}

val adventure: Configuration by configurations.creating {}

configurations {
    implementation.extendsFrom(named("adventure"))
}

dependencies {
    implementation("com.mojang:brigadier:1.1.8")

    // Locked at the version that was released for Minecraft 1.18.x
    // If other mods use newer versions of Adventure, Fabric will upgrade this dependency (right?)
    adventure("net.kyori:adventure-api:4.9.3")

    implementation("com.google.guava:guava:33.1.0-jre")

    implementation("org.apache.commons:commons-lang3:3.13.0")
    implementation("org.slf4j:slf4j-api:2.0.12")

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    testCompileOnly("org.projectlombok:lombok:1.18.32")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.32")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.apache.logging.log4j:log4j-core:2.23.1")
    testImplementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.23.1")
}

tasks {
    test {
        useJUnitPlatform()
    }
}