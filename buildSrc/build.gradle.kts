plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    kotlin("plugin.serialization") version "1.9.22"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    target {
        compilations.all {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_21.toString()
            }
        }
    }
}

repositories {
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
    mavenCentral()
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        create("fabricMod") {
            id = "fabric-mod"
            implementationClass = "FabricModPlugin"
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.23")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("net.fabricmc:fabric-loom:1.6-SNAPSHOT")
    implementation("com.modrinth.minotaur:Minotaur:2.+")
    implementation("net.swiftzer.semver:semver:1.3.0")
}
