package com.gitolite.pidorKt.extensions.pidors.command

import com.gitolite.pidorKt.MAX_DELAY
import com.gitolite.pidorKt.extensions.pidors.dto.MessagesDto
import com.gitolite.pidorKt.extensions.pidors.model.Guild
import com.gitolite.pidorKt.extensions.pidors.model.Guilds
import com.gitolite.pidorKt.extensions.pidors.model.Pidor
import com.gitolite.pidorKt.extensions.pidors.model.Pidors
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.PublicSlashCommandContext
import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.types.respondEphemeral
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Member
import dev.kord.core.kordLogger
import io.github.reactivecircus.cache4k.Cache
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.withIndex
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class PidorCommand : Extension() {
    override val name: String
        get() = "extensions.pidors.command.pidor"

    private val exec: Cache<Snowflake, Boolean> by inject(named("exec"))
    private val db: Database by inject()
    private val messagesDto: MessagesDto by inject()

    override suspend fun setup() {
        publicSlashCommand {
            name = "pidor"
            description = "Узнать, кто же он?"

            check {
                anyGuild()
            }

            action {
                val guildConfig = currentGuild()
                val membersCount = guild!!.members.count()
                val membersCountByRole = guild!!.members.filter {
                    it.roleIds.contains(Snowflake(guildConfig.role!!))
                }.count()

                kordLogger.info {
                    "Members count: $membersCount"
                }

                kordLogger.info {
                    "Members count: $membersCountByRole"
                }

                when {
                    exec.get(guild!!.id) !== null -> {
                        respondEphemeral { content = "Подожди мой сладкий ;)" }
                    }
                    else -> {
                        exec.put(guild!!.id, true)
                        when (guildConfig.role) {
                            null -> {
                                respond {
                                    content = "Роль розыгрыша не настроена"
                                }
                            }
                            else -> {
                                val pidorObjRes: Pidor? = findExistingPidorOrNull(guildConfig)

                                execute(pidorObjRes, guildConfig)
                            }
                        }

                        exec.invalidate(guild!!.id)
                    }
                }
            }
        }
    }

    private fun PublicSlashCommandContext<Arguments, ModalForm>.currentGuild(): Guild {
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

    private suspend fun PublicSlashCommandContext<Arguments, ModalForm>.execute(
        pidorObjRes: Pidor?,
        guildConfig: Guild
    ) {
        when {
            pidorObjRes === null -> {
                val members = guild!!.members.filter {
                    it.roleIds.contains(Snowflake(guildConfig.role!!))
                }

                val pidor = members.withIndex().first {
                    it.index == (0 until members.count()).random()
                }.value

                createPidorForGuild(pidor, guildConfig)

                playScenario(pidor)
            }

            else -> {
                val members = guild!!.members
                val count = members.count()
                kordLogger.debug {
                    count
                }

                val pidor = members.first { it.id.value.toLong() == pidorObjRes.user }

                respond {
                    val res = messagesDto.responses.random().replace("#user", pidor.mention)
                    content = res
                }
            }
        }
    }

    private fun findExistingPidorOrNull(guildConfig: Guild): Pidor? {
        val pidorObjRes: Pidor? = transaction(db) {
            Pidor.find {
                (Pidors.guild eq guildConfig.id).and(
                    Pidors.chosenAt.between(
                        LocalDate.now(ZoneId.of(guildConfig.timeZone)).atStartOfDay(),
                        LocalDateTime.now(ZoneId.of(guildConfig.timeZone))
                    )
                )
            }.firstOrNull()
        }
        return pidorObjRes
    }

    private suspend fun PublicSlashCommandContext<Arguments, ModalForm>.playScenario(
        pidor: Member
    ) {
        messagesDto.scenarios.random().messages.forEach {
            respond {
                content = it.replace("#user", pidor.mention)
            }
            delay((1L..MAX_DELAY).random().toDuration(DurationUnit.SECONDS).inWholeMilliseconds)
        }
    }

    private fun createPidorForGuild(member: Member, guildObject: Guild): Pidor {
        return transaction(db) {
            Pidor.new {
                chosenAt = LocalDateTime.now(ZoneId.of(guildObject.timeZone))
                user = member.id.value.toLong()
                guild = guildObject
            }
        }
    }
}
