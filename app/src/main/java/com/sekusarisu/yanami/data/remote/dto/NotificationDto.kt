package com.sekusarisu.yanami.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OfflineNotificationDto(
    @SerialName("client") val clientUuid: String = "",
    @SerialName("enable") val enabled: Boolean = false,
    @SerialName("grace_period") val gracePeriod: Int = 0,
    @SerialName("last_notified") val lastNotified: String? = null
)

@Serializable
data class LoadNotificationDto(
    val id: Int = 0,
    val name: String = "",
    val clients: List<String> = emptyList(),
    val metric: String = "",
    val threshold: Double = 0.0,
    val ratio: Double = 0.0,
    val interval: Int = 60,
    @SerialName("last_notified") val lastNotified: String? = null
)

@Serializable
data class LoadNotificationCreateResultDto(@SerialName("task_id") val taskId: Int = 0)
