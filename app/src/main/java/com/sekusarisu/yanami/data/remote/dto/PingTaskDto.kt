package com.sekusarisu.yanami.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdminPingTaskDto(
    val id: Int = 0,
    val weight: Int = 0,
    val name: String = "",
    val clients: List<String> = emptyList(),
    val type: String = "icmp",
    val target: String = "",
    val interval: Int = 60
)

@Serializable
data class PingTaskCreateResultDto(@SerialName("task_id") val taskId: Int = 0)
