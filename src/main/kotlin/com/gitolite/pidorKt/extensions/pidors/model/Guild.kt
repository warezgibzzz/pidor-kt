package com.gitolite.pidorKt.extensions.pidors.model

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

object Guilds : LongIdTable() {
    val discordId: Column<Long> = long("discordId").uniqueIndex()
    val role: Column<Long> = long("role")
}

class Guild(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Guild>(Guilds)
    var discordId by Guilds.discordId
    var role by Guilds.role
}
