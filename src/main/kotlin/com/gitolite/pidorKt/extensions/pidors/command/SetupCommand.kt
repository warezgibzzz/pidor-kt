package com.gitolite.pidorKt.extensions.pidors.command

import com.gitolite.pidorKt.extensions.pidors.model.Guild
import com.gitolite.pidorKt.extensions.pidors.model.Guilds
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Permission
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.inject

class SetupCommand : Extension() {
    override val name: String
        get() = "extensions.pidors.command.setup"

    override suspend fun setup() {
        publicSlashCommand() {
            name = "setup"
            description = "Настройка викторины"

            val db: Database by inject()

            check {
                anyGuild()
                failIf {
                    hasPermission(Permission.ManageGuild).equals(false)
                }
            }

            publicSubCommand(::SetupArgs) {
                name = "role"
                description = "Выбрать роль"

                action {
                    transaction(db) {
                        val requestedGuild: Guild
                        val requesterGuildRes = Guild.find {
                            Guilds.discordId eq guild!!.id.value.toLong()
                        }

                        if (requesterGuildRes.empty()) {
                            requestedGuild = Guild.new {
                                discordId = guild!!.id.value.toLong()
                            }
                        } else {
                            requestedGuild = requesterGuildRes.first()
                        }

                        requestedGuild.role = arguments.targetRole.id.value.toLong()
                    }

                    respond {
                        content = "Роль ${arguments.targetRole.mention} была выбрана для участия в розыгрыше!"
                    }
                }
            }
        }
    }
    inner class SetupArgs : Arguments() {
        val targetRole by role {
            this.name = "роль"
            this.description = "Какая роль может учавствовать в розыгрыше"
        }
    }
}
