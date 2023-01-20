package com.gitolite.pidorKt.extensions.pidors.service

import com.gitolite.pidorKt.extensions.pidors.model.Guilds
import com.gitolite.pidorKt.extensions.pidors.model.Pidors
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.core.kordLogger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseConnection : Extension() {
    override val name: String
        get() = "extensions.pidors.service.databaseConnection"

    override suspend fun setup() {
        val dbConnString = System.getenv("DB_URL")
        kordLogger.debug { "org.postgresql.Driver" }
        kordLogger.debug { System.getenv("DB_DRIVER").toString() }
        val db: Database = Database.connect(
            dbConnString,
            driver = System.getenv("DB_DRIVER"),
            user = System.getenv("DB_USER"),
            password = System.getenv("DB_PASSWORD")
        )

        transaction {
            SchemaUtils.create(Guilds)
            SchemaUtils.create(Pidors)
            SchemaUtils.checkMappingConsistence()
        }

        loadModule {
            single(createdAtStart = true) {
                db
            }
        }
    }
}
