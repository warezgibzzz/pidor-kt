package com.gitolite.pidorKt.extensions.pidors.command

import com.gitolite.pidorKt.extensions.pidors.model.Guild
import com.gitolite.pidorKt.extensions.pidors.model.Guilds
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.stringChoice
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.suggestStringMap
import dev.kord.common.entity.Permission
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.inject
import java.util.*

class SetupCommand : Extension() {
    override val name: String
        get() = "extensions.pidors.command.setup"

    private val db: Database by inject()
    override suspend fun setup() {
        ephemeralSlashCommand() {
            name = "setup"
            description = "Настройка викторины"

            check {
                anyGuild()
                failIf {
                    hasPermission(Permission.ManageGuild).equals(false)
                }
            }

            setupRoleCommand()
            setupChannelCommand()
            setupTimezoneCommand()
        }
    }

    private suspend fun EphemeralSlashCommand<Arguments, ModalForm>.setupRoleCommand() {
        ephemeralSubCommand(::SetupRoleArgs) {
            name = "role"
            description = "Выбрать роль"

            action {
                val requestedGuild: Guild = determineOrCreateGuild()

                transaction(db) {
                    requestedGuild.role = arguments.targetRole.id.value.toLong()
                }

                respond {
                    content = "Роль ${arguments.targetRole.mention} была выбрана для участия в розыгрыше!"
                }
            }
        }
    }
    private suspend fun EphemeralSlashCommand<Arguments, ModalForm>.setupChannelCommand() {
        ephemeralSubCommand(::SetupChannelArgs) {
            name = "channel"
            description = "Выбрать канал розыгрыша"

            action {
                val requestedGuild: Guild = determineOrCreateGuild()

                transaction(db) {
                    requestedGuild.pidorChannel = arguments.targetChannel.id.value.toLong()
                }

                respond {
                    content = "Розыгрыш был привязан в канал ${arguments.targetChannel.mention}"
                }
            }
        }
    }

    private suspend fun EphemeralSlashCommand<Arguments, ModalForm>.setupTimezoneCommand() {
        ephemeralSubCommand(::SetupTimezoneArgs) {
            name = "timezone"
            description = "Выбрать часоой пояс"

            action {
                val requestedGuild: Guild = determineOrCreateGuild()

                transaction(db) {
                    requestedGuild.timeZone = arguments.targetTimezone
                }

                respond {
                    content = "Настроен часовой пояс розыгрыша: `${arguments.targetTimezone}`"
                }
            }
        }
    }

    private fun <T : Arguments> EphemeralSlashCommandContext<T, ModalForm>.determineOrCreateGuild(): Guild {
        val guildConfig: Guild
        val guildRes = transaction(db) {
            Guild.find {
                Guilds.discordId eq guild!!.id.value.toLong()
            }.firstOrNull()
        }

        when {
            guildRes === null -> {
                guildConfig = transaction(db) {
                    Guild.new {
                        discordId = guild!!.id.value.toLong()
                    }
                }
            }

            else -> {
                guildConfig = guildRes
            }
        }

        return guildConfig
    }

    inner class SetupRoleArgs : Arguments() {
        val targetRole by role {
            name = "role"
            description = "Какая роль может учавствовать в розыгрыше"
        }
    }

    inner class SetupChannelArgs : Arguments() {
        val targetChannel by channel {
            name = "channel"
            description = "К какому каналу привязан розыгрыш"
        }
    }

    inner class SetupTimezoneArgs : Arguments() {
        val targetTimezone by stringChoice {
            name = "timezone"
            description = "В каком часовом поясе проводить"

            autoComplete {
                suggestStringMap(getTimezoneMap())
            }
        }
    }

    private fun getTimezoneMap(): Map<String, String> {
        return TimeZone.getAvailableIDs().toList()
            .associateBy(
                { it },
                { it }
            )
    }
}
