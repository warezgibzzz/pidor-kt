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
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Member
import dev.kord.core.kordLogger
import io.github.reactivecircus.cache4k.Cache
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
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


    @Suppress("TooGenericExceptionCaught")
    override suspend fun setup() {
        publicSlashCommand {
            name = "pidor"
            description = "Узнать, кто же он?"

            check {
                anyGuild()
            }

            action {
                val guildConfig = currentGuild()

                if (guildConfig.pidorChannel == null) {
                    respond {
                        content = "Нет привязки к каналу"
                    }

                    return@action
                }

                if (guildConfig.role == null) {
                    respond {
                        content = "Роль розыгрыша не настроена"
                    }
                    return@action
                }

                if (channel.id.value.toLong() != guildConfig.pidorChannel) {
                    val boundChannel = guild!!.getChannel(Snowflake(guildConfig.pidorChannel!!)).mention
                    respond {
                        content = "Не в том канале пытаешься это сделать, мой сладкий. Тебе в $boundChannel :)"
                    }
                    return@action
                }

                when {
                    exec.get(guild!!.id) !== null -> {
                        respond { content = "Подожди мой сладкий ;)" }
                    }
                    else -> {
                        exec.put(guild!!.id, true)

                        val pidorObjRes: Pidor? = findExistingPidorOrNull(guildConfig)
                        try {
                            execute(pidorObjRes, guildConfig)
                        } catch (e: Exception) {
                            kordLogger.error {
                                e.message
                            }
                            respond {
                                content = "Я что-то затупил, перезапусти розыгрыш"
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
                val pidor = guild!!.members.filter {
                    it.roleIds.contains(Snowflake(guildConfig.role!!))
                }.toList().random()

                createPidorForGuild(pidor, guildConfig)

                playScenario(pidor)
            }

            else -> {
                val pidor = guild!!.members.first { it.id.value.toLong() == pidorObjRes.user }

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
