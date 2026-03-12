package com.sekusarisu.yanami.ui.widget

import kotlinx.serialization.Serializable

@Serializable
data class WidgetState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val serverName: String = "",
    val totalCount: Int = 0,
    val onlineCount: Int = 0,
    val offlineCount: Int = 0,
    val totalTrafficUp: Long = 0,
    val totalTrafficDown: Long = 0,
    val lastUpdated: Long = 0,
    val updateIntervalMinutes: Int = 30
)
