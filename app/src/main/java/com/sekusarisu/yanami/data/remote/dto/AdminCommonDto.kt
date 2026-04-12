package com.sekusarisu.yanami.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AdminEnvelopeDto(
        val status: String = "",
        val message: String = "",
        val data: JsonElement? = null
)
