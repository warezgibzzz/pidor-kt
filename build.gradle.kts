
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import java.util.*

group = "com.gitolite"
version = "1.0.0"
description = "DiscordKt starter template"

plugins {
    application

    kotlin("jvm")
    kotlin("plugin.serialization")

    id("com.github.jakemarsden.git-hooks")
    id("com.github.johnrengelman.shadow")
    id("io.gitlab.arturbosch.detekt")
}

repositories {
    mavenCentral()

    maven {
        name = "Sonatype Snapshots"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }

    maven {
        name = "Kotlin Discord"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }
}

dependencies {
    // Core
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.serialization.json)

    // Logging
    implementation(libs.groovy)
    implementation(libs.logback)
    implementation(libs.logging)

    // Discord
    implementation(libs.kord.core)
    implementation(libs.kord.extensions)

    // Database
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.dao)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.time)
    implementation(libs.jdbc.driver.postgresql)
    implementation(libs.jdbc.driver.postgresqlng)

    // Util
    implementation(libs.cache4k)
}

application {
    // Define the main class for the application.
    mainClass.set("com.gitolite.pidorKt.MainKt")
}

gitHooks {
    setHooks(
        mapOf("pre-commit" to "detekt")
    )
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config = files("$projectDir/detekt.yml")
    baseline = file("$projectDir/baseline.xml")
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "16"
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
        md.required.set(true)
    }
}
tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = "11"
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.gitolite.pidorKt.MainKt"
        )
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"

        Properties().apply {
            setProperty("name", project.name)
            setProperty("description", project.description)
            setProperty("version", version.toString())
            setProperty("url", "https://github.com/warezgibzzz/pidor-kt")

            store(file("src/main/resources/bot.properties").outputStream(), null)
        }
    }

    shadowJar {
        archiveFileName.set("Bot.jar")
        manifest {
            attributes("Main-Class" to "com.gitolite.pidorKt.MainKt")
        }
    }
}
