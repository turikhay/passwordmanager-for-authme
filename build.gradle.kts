plugins {
    base
}

// Idea (ijDownloadSources task) fails to download source jar for buildSrc dependencies
// (e.g. Fabric API) because it is only using repositories from the root project (i.e. here)
repositories {
    mavenCentral()
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
}