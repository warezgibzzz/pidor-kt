package com.gitolite.pidorKt.extensions.pidors.command

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
import io.github.reactivecircus.cache4k.Cache
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.withIndex
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.CurrentDate
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.time.LocalDate
import kotlin.time.DurationUnit
import kotlin.time.toDuration

const val MAX_DELAY = 5L

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
                if (exec.get(guild!!.id) !== null) {
                    respondEphemeral { content = "Подожди мой сладкий ;)" }
                } else {
                    exec.put(guild!!.id, true)
                    val pidorObj: Pidor
                    val pidor: Member
                    val guildConfig = transaction(db) {
                        Guild.find {
                            Guilds.discordId eq guild!!.id.value.toLong()
                        }.first()
                    }
                    val members = guild!!.members.filter {
                        it.roleIds.contains(Snowflake(guildConfig.role))
                    }

                    val pidorObjRes: Pidor? = findExistingPidorOrNull(guildConfig)

                    when {
                        pidorObjRes === null -> {
                            pidor = members.withIndex().first {
                                it.index == (0 until members.count()).random()
                            }.value

                            createPidorForGuild(pidor, guildConfig)

                            playScenario(pidor)
                        }

                        else -> {
                            pidorObj = pidorObjRes
                            pidor = members.first { it.id.value.toLong() == pidorObj.user }

                            respond {
                                content = messagesDto.responses.random().replace("#user", pidor.mention)
                            }
                        }
                    }

                    exec.invalidate(guild!!.id)
                }
            }
        }
    }

    private fun findExistingPidorOrNull(guildConfig: Guild): Pidor? {
        val pidorObjRes: Pidor? = transaction(db) {
            Pidor.find {
                (Pidors.guild eq guildConfig.id).and(Pidors.chosenAt eq CurrentDate)
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
                chosenAt = LocalDate.now()
                user = member.id.value.toLong()
                guild = guildObject
            }
        }
    }
}
