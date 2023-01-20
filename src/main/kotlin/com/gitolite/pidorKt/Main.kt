package com.gitolite.pidorKt

import com.gitolite.pidorKt.extensions.pidors.command.PidorCommand
import com.gitolite.pidorKt.extensions.pidors.command.SetupCommand
import com.gitolite.pidorKt.extensions.pidors.service.ApplicationCache
import com.gitolite.pidorKt.extensions.pidors.service.DatabaseConnection
import com.gitolite.pidorKt.extensions.pidors.service.Messages
import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.common.entity.PresenceStatus
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent

val TOKEN: String = System.getenv("BOT_TOKEN")

@OptIn(PrivilegedIntent::class)
suspend fun main() {
    val bot = ExtensibleBot(TOKEN) {
        extensions {
            add(::ApplicationCache)
            add(::DatabaseConnection)
            add(::Messages)
            add(::PidorCommand)
            add(::SetupCommand)
        }

        members {
            all()
        }

        intents {
            +Intent.GuildMembers
            +Intent.GuildMessages
        }

        presence {
            status = PresenceStatus.DoNotDisturb

            listening("Best gachibASS mixes")
        }

        applicationCommands {
            // Register all global commands on this guild for testing
//            defaultGuild(Snowflake(System.getenv("TEST_GUILD_ID")))
            register = true
        }
    }

    bot.start()
}
