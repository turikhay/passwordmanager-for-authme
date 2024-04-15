plugins {
    `fabric-mod`
}

fabricMod {
    supportedVersions = listOf("1.20.3", "1.20.4")
}

dependencies {
    maybeInclude(fabricMod.module("fabric-command-api-v2"))
}