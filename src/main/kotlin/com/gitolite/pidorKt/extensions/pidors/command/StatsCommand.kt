@file:Suppress("WildcardImport", "NoWildcardImports")

package com.gitolite.pidorKt.extensions.pidors.command

import com.gitolite.pidorKt.*
import com.gitolite.pidorKt.extensions.pidors.model.Guild
import com.gitolite.pidorKt.extensions.pidors.model.Guilds
import com.gitolite.pidorKt.extensions.pidors.model.Pidors
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.PublicSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Snowflake
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.inject
import java.util.*
import java.util.TimeZone.getTimeZone
import kotlinx.datetime.TimeZone as TimeZoneKt

class StatsCommand : Extension() {
    override val name: String
        get() = "extensions.pidors.command.stats"

    private val db: Database by inject()
    override suspend fun setup() {
        publicSlashCommand {
            name = "stats"
            description = "Доска почета"

            check {
                anyGuild()
            }

            publicSubCommand {
                name = "total"
                description = "Доска почета за все время"

                action {
                    val pidors = getAllTimeLadder()

                    createLadderEmbedResponse(
                        "Топ-10 лидеров за все время",
                        "Посмотрите же на этих красавцев",
                        pidors
                    )
                }
            }

            publicSubCommand {
                name = "month"
                description = "Доска почета в текущем месяце"

                action {
                    val days = getBeginAndEndMonthDays(LadderRange.MONTH)

                    val pidors = getLadder(days)

                    createLadderEmbedResponse(
                        "Топ-10 лидеров за месяц",
                        "Посмотрите же на этих красавцев",
                        pidors
                    )
                }
            }

            publicSubCommand {
                name = "year"
                description = "Доска почета в текущем году"

                action {
                    val days = getBeginAndEndMonthDays(LadderRange.YEAR)

                    val pidors = getLadder(days)

                    createLadderEmbedResponse(
                        "Топ-10 лидеров за год",
                        "Посмотрите же на этих красавцев",
                        pidors
                    )
                }
            }
        }
    }

    private suspend fun PublicSlashCommandContext<Arguments, ModalForm>.createLadderEmbedResponse(
        title: String,
        description: String,
        pidors: List<Pair<Long, Long>>
    ) {
        respond {
            embed {
                this.title = title
                this.description = description

                pidors.forEach { row ->
                    val member = guild!!.members.firstOrNull { Snowflake(row.first) == it.id }
                    val memberName: String = member?.displayName ?: "Ушедший пидор"

                    field {
                        name = memberName
                        value = "`${row.second}` раз"
                        inline = true
                    }
                }
            }
        }
    }

    private fun PublicSlashCommandContext<Arguments, ModalForm>.getLadder(
        days: Pair<java.time.LocalDateTime, java.time.LocalDateTime>
    ): List<Pair<Long, Long>> {
        val pidors = transaction(db) {
            val count = Pidors.user.count().alias("count")
            (Pidors innerJoin Guilds)
                .slice(count, Pidors.user)
                .select(
                    (Guilds.discordId eq guild!!.id.value.toLong())
                )
                .andWhere {
                    Pidors.chosenAt.between(
                        days.first,
                        days.second,
                    )
                }
                .orderBy(Pidors.user.count() to SortOrder.DESC)
                .orderBy(Pidors.user to SortOrder.DESC)
                .groupBy(Pidors.user)
                .limit(MAX_LEADERS)
                .map {
                    Pair(it[Pidors.user], it[count])
                }
        }
        return pidors
    }

    private fun PublicSlashCommandContext<Arguments, ModalForm>.getAllTimeLadder() =
        transaction(db) {
            val count = Pidors.user.count().alias("count")
            (Pidors innerJoin Guilds)
                .slice(count, Pidors.user)
                .select(Guilds.discordId eq guild!!.id.value.toLong())
                .orderBy(Pidors.user.count() to SortOrder.DESC)
                .groupBy(Pidors.user)
                .limit(MAX_LEADERS)
                .map {
                    Pair(it[Pidors.user], it[count])
                }
        }

    private fun PublicSlashCommandContext<Arguments, ModalForm>
    .getBeginAndEndMonthDays(period: LadderRange): Pair<java.time.LocalDateTime, java.time.LocalDateTime> {
        val currentGuildConfig = guildConfig()

        val now: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZoneKt.of(currentGuildConfig.timeZone))
        val cal: Calendar = Calendar.getInstance(getTimeZone(currentGuildConfig.timeZone))

        val firstDay: java.time.LocalDateTime
        val lastDay: java.time.LocalDateTime

        when (period) {
            LadderRange.MONTH -> {
                firstDay = LocalDateTime(now.year, now.month, 1, 0, 0, 0)
                    .toJavaLocalDateTime()
                lastDay = LocalDateTime(
                    now.year,
                    now.month,
                    cal.getActualMaximum(Calendar.DAY_OF_MONTH),
                    LAST_HOUR,
                    LAST_MINUTE,
                    LAST_SECOND
                ).toJavaLocalDateTime()
            }

            else -> {
                firstDay = LocalDateTime(now.year, Month(1), 1, 0, 0, 0)
                    .toJavaLocalDateTime()
                lastDay = LocalDateTime(
                    now.year,
                    LAST_MONTH_OF_YEAR,
                    LAST_DAY_OF_YEAR,
                    LAST_HOUR,
                    LAST_MINUTE,
                    LAST_SECOND
                ).toJavaLocalDateTime()
            }
        }

        return Pair(firstDay, lastDay)
    }

    private fun PublicSlashCommandContext<Arguments, ModalForm>.guildConfig() =
        transaction(db) {
            Guild.find { Guilds.discordId eq guild!!.id.value.toLong() }.first()
        }

    internal enum class LadderRange {
        MONTH, YEAR
    }
}
