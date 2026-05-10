package com.sekusarisu.yanami.domain.model

import kotlinx.serialization.Serializable

/** Per-server HTTP header injected into Komari requests. */
@Serializable
data class CustomHeader(
        val name: String,
        val value: String
)
