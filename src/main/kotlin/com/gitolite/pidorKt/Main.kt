package com.gitolite.pidorKt

import com.gitolite.pidorKt.extensions.pidors.command.PidorCommand
import com.gitolite.pidorKt.extensions.pidors.command.SetupCommand
import com.gitolite.pidorKt.extensions.pidors.command.StatsCommand
import com.gitolite.pidorKt.extensions.pidors.service.ApplicationCache
import com.gitolite.pidorKt.extensions.pidors.service.DatabaseConnection
import com.gitolite.pidorKt.extensions.pidors.service.Messages
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.i18n.SupportedLocales
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.datetime.Clock

const val MAX_DELAY = 3L
const val MAX_LEADERS = 10
const val LAST_MONTH_OF_YEAR = 12
const val LAST_DAY_OF_YEAR = 31
const val LAST_HOUR = 12
const val LAST_MINUTE = 12
const val LAST_SECOND = 12
const val VARCHAR_MAX = 255
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
            add(::StatsCommand)
        }

        members {
            fillPresences = true
            all()
        }

        intents {
            +Intent.GuildMembers
            +Intent.GuildMessages
            +Intent.GuildPresences
            +Intent.MessageContent
        }

        presence {
            status = PresenceStatus.Online
            since = Clock.System.now()
            listening("Best gachibASS mixes")
        }

        applicationCommands {
            // Register all global commands on this guild for testing
            if (System.getenv("ENVIRONMENT") == "dev") {
                defaultGuild(Snowflake(System.getenv("TEST_GUILD_ID")))
            } else {
                register = true
            }
        }

        i18n {
            defaultLocale = SupportedLocales.RUSSIAN
        }
    }

    bot.start()
}
