plugins {
    `fabric-mod`
}

fabricMod {
    supportedVersions = listOf("1.20.5-beta.1")
}

dependencies {
    maybeInclude(fabricMod.module("fabric-command-api-v2"))
}