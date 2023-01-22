package com.gitolite.pidorKt.extensions.pidors.model

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime

object Pidors : IntIdTable() {
    val chosenAt = datetime("chosenAt")
    val user: Column<Long> = long("user")
    val guild = reference("guild", Guilds)

    init {
        uniqueIndex("guild_chosen_idx", chosenAt, guild)
    }
}

class Pidor(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Pidor>(Pidors)

    var chosenAt by Pidors.chosenAt
    var user by Pidors.user
    var guild by Guild referencedOn Pidors.guild
}
