import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
    `kotlin-common`
}

configurations {
    implementation.run {
        extendsFrom(includeable)
        extendsFrom(maybeInclude)
    }
}