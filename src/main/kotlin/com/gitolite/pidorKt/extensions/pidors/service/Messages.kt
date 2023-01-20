package com.gitolite.pidorKt.extensions.pidors.service

import com.gitolite.pidorKt.extensions.pidors.dto.MessagesDto
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.loadModule
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

class Messages : Extension() {
    @property:OptIn(ExperimentalSerializationApi::class)
    val messagesDto: MessagesDto = Json.decodeFromStream<MessagesDto>(
        this.javaClass.classLoader.getResourceAsStream("messages.json")!!
    )
    override val name: String
        get() = "extensions.pidors.service.messages"

    override suspend fun setup() {
        loadModule {
            single(createdAtStart = true) {
                messagesDto
            }
        }
    }
}
