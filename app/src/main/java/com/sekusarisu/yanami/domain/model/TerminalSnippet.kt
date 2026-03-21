package com.sekusarisu.yanami.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TerminalSnippet(
        val id: String,
        val title: String,
        val content: String,
        val appendEnter: Boolean = false
)
