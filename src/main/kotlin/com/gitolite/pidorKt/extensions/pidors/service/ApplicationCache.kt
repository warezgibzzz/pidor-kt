package com.gitolite.pidorKt.extensions.pidors.service

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.common.entity.Snowflake
import io.github.reactivecircus.cache4k.Cache
import org.koin.core.qualifier.named

class ApplicationCache : Extension() {
    override val name: String
        get() = "extensions.pidors.service.applicationCache"

    override suspend fun setup() {
        loadModule {
            single(named("exec"), createdAtStart = true) {
                Cache.Builder().build<Snowflake, Boolean>()
            }
        }
    }
}
