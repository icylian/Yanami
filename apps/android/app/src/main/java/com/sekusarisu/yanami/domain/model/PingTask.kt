package com.sekusarisu.yanami.domain.model

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

enum class PingTaskType(val apiValue: String) {
    ICMP("icmp"),
    TCP("tcp"),
    HTTP("http");

    companion object {
        fun fromApiValue(value: String): PingTaskType {
            return entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) } ?: ICMP
        }
    }
}

data class AdminPingTask(
    val id: Int,
    val weight: Int,
    val name: String,
    val clients: List<String>,
    val type: PingTaskType,
    val target: String,
    val interval: Int
)

data class AdminPingTaskDraft(
    val name: String = "",
    val target: String = "",
    val type: PingTaskType = PingTaskType.ICMP,
    val interval: String = "60",
    val selectedClientUuids: Set<String> = emptySet()
)

fun AdminPingTask.toDraft(): AdminPingTaskDraft =
    AdminPingTaskDraft(
        name = name,
        target = target,
        type = type,
        interval = interval.toString(),
        selectedClientUuids = clients.toSet()
    )

fun AdminPingTaskDraft.toCreatePayload(): JsonObject {
    val validated = validatePingTaskDraft()
    return buildJsonObject {
        put("name", validated.name)
        put("target", validated.target)
        put("type", validated.type.apiValue)
        put("interval", validated.interval)
        putJsonArray("clients") {
            validated.clients.forEach { uuid -> add(uuid) }
        }
    }
}

fun AdminPingTaskDraft.applyTo(task: AdminPingTask): AdminPingTask {
    val validated = validatePingTaskDraft()
    return task.copy(
        name = validated.name,
        target = validated.target,
        type = validated.type,
        interval = validated.interval,
        clients = validated.clients
    )
}

fun AdminPingTask.toEditPayload(): JsonObject {
    return buildJsonObject {
        put("id", id)
        put("weight", weight)
        put("name", name.trim())
        put("target", target.trim())
        put("type", type.apiValue)
        put("interval", interval)
        putJsonArray("clients") {
            clients.forEach { uuid -> add(uuid) }
        }
    }
}

private data class ValidatedPingTaskDraft(
    val name: String,
    val target: String,
    val type: PingTaskType,
    val interval: Int,
    val clients: List<String>
)

private fun AdminPingTaskDraft.validatePingTaskDraft(): ValidatedPingTaskDraft {
    val parsedInterval =
        interval.trim().toIntOrNull()
            ?: throw IllegalArgumentException("interval 必须是整数")

    val normalizedName = name.trim()
    val normalizedTarget = target.trim()
    val normalizedClients = selectedClientUuids.map { it.trim() }.filter { it.isNotEmpty() }.sorted()

    require(normalizedName.isNotEmpty()) { "name 不能为空" }
    require(normalizedTarget.isNotEmpty()) { "target 不能为空" }
    require(parsedInterval > 0) { "interval 必须大于 0" }
    require(normalizedClients.isNotEmpty()) { "至少选择一个客户端" }

    return ValidatedPingTaskDraft(
        name = normalizedName,
        target = normalizedTarget,
        type = type,
        interval = parsedInterval,
        clients = normalizedClients
    )
}
