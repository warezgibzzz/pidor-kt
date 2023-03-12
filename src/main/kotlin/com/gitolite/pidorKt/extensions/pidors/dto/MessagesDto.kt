package com.gitolite.pidorKt.extensions.pidors.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessagesDto(
    val responses: List<String>,
    val scenarios: List<Scenario>
)
