package com.gitolite.pidorKt.extensions.pidors.service

import com.gitolite.pidorKt.extensions.pidors.model.Guilds
import com.gitolite.pidorKt.extensions.pidors.model.Pidors
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.loadModule
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseConnection : Extension() {
    override val name: String
        get() = "extensions.pidors.service.databaseConnection"

    override suspend fun setup() {
        val dbConnString = System.getenv("DB_URL")
        val db: Database = Database.connect(
            dbConnString,
            driver = System.getenv("DB_DRIVER"),
            user = System.getenv("DB_USER"),
            password = System.getenv("DB_PASSWORD")
        )

        transaction {
            SchemaUtils.createMissingTablesAndColumns(Guilds, Pidors)

            SchemaUtils.statementsRequiredToActualizeScheme(Guilds, Pidors).forEach {
                this.exec(it)
            }
            SchemaUtils.checkMappingConsistence(Guilds, Pidors).forEach {
                this.exec(it)
            }
        }

        loadModule {
            single(createdAtStart = true) {
                db
            }
        }
    }
}
