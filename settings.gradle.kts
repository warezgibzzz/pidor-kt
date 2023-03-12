@file:Suppress("UnstableApiUsage")

pluginManagement {
    plugins {
        // Update this in libs.version.toml when you change it here
        kotlin("jvm") version "1.7.20"
        kotlin("plugin.serialization") version "1.7.20"

        // Update this in libs.version.toml when you change it here
        id("io.gitlab.arturbosch.detekt") version "1.22.0"

        id("com.github.jakemarsden.git-hooks") version "0.0.2"
        id("com.github.johnrengelman.shadow") version "7.0.0"
    }
}

rootProject.name = "pidorKt"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}

include("app")
