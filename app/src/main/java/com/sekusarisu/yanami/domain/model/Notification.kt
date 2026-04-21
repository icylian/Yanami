package com.sekusarisu.yanami.domain.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

data class OfflineNotificationConfig(
    val clientUuid: String,
    val enabled: Boolean,
    val gracePeriod: Int,
    val lastNotified: String? = null
)

enum class LoadNotificationMetric(val apiValue: String) {
    CPU("cpu"),
    RAM("ram"),
    DISK("disk"),
    NET_IN("net_in"),
    NET_OUT("net_out");

    companion object {
        fun fromApiValue(value: String): LoadNotificationMetric? {
            return entries.firstOrNull { it.apiValue.equals(value.trim(), ignoreCase = true) }
        }
    }
}

data class LoadNotificationTask(
    val id: Int,
    val name: String,
    val clients: List<String>,
    val metric: String,
    val threshold: Double,
    val ratio: Double,
    val interval: Int,
    val lastNotified: String? = null
)

data class LoadNotificationDraft(
    val name: String = "",
    val metric: String = LoadNotificationMetric.CPU.apiValue,
    val threshold: String = "0",
    val ratio: String = "0",
    val interval: String = "60",
    val selectedClientUuids: Set<String> = emptySet()
)

fun LoadNotificationTask.toDraft(): LoadNotificationDraft =
    LoadNotificationDraft(
        name = name,
        metric = metric,
        threshold = threshold.toString(),
        ratio = ratio.toString(),
        interval = interval.toString(),
        selectedClientUuids = clients.toSet()
    )

fun LoadNotificationDraft.toCreatePayload(): JsonObject {
    val validated = validateLoadNotificationDraft()
    return buildJsonObject {
        put("name", validated.name)
        put("metric", validated.metric)
        put("threshold", validated.threshold)
        put("ratio", validated.ratio)
        put("interval", validated.interval)
        putJsonArray("clients") {
            validated.clients.forEach { uuid -> add(uuid) }
        }
    }
}

fun LoadNotificationDraft.applyTo(task: LoadNotificationTask): LoadNotificationTask {
    val validated = validateLoadNotificationDraft()
    return task.copy(
        name = validated.name,
        metric = validated.metric,
        threshold = validated.threshold,
        ratio = validated.ratio,
        interval = validated.interval,
        clients = validated.clients
    )
}

fun LoadNotificationTask.toEditPayload(): JsonObject =
    buildJsonObject {
        put("id", id)
        put("name", name.trim())
        put("metric", metric.trim())
        put("threshold", threshold)
        put("ratio", ratio)
        put("interval", interval)
        putJsonArray("clients") {
            clients.forEach { uuid -> add(uuid) }
        }
    }

fun List<OfflineNotificationConfig>.toEditPayload(): JsonArray =
    buildJsonArray {
        forEach { config ->
            add(
                buildJsonObject {
                    put("client", config.clientUuid)
                    put("enable", config.enabled)
                    put("grace_period", config.gracePeriod)
                }
            )
        }
    }

private data class ValidatedLoadNotificationDraft(
    val name: String,
    val metric: String,
    val threshold: Double,
    val ratio: Double,
    val interval: Int,
    val clients: List<String>
)

private fun LoadNotificationDraft.validateLoadNotificationDraft(): ValidatedLoadNotificationDraft {
    val normalizedName = name.trim()
    val normalizedMetric =
        LoadNotificationMetric.fromApiValue(metric)?.apiValue
            ?: throw IllegalArgumentException("metric 不在允许范围内")
    val parsedThreshold =
        threshold.trim().toDoubleOrNull()
            ?: throw IllegalArgumentException("threshold 必须是数字")
    val parsedRatio =
        ratio.trim().toDoubleOrNull()
            ?: throw IllegalArgumentException("ratio 必须是数字")
    val parsedInterval =
        interval.trim().toIntOrNull()
            ?: throw IllegalArgumentException("interval 必须是整数")
    val normalizedClients = selectedClientUuids.map { it.trim() }.filter { it.isNotEmpty() }.sorted()

    require(normalizedName.isNotEmpty()) { "name 不能为空" }
    require(parsedInterval > 0) { "interval 必须大于 0" }
    require(normalizedClients.isNotEmpty()) { "至少选择一个客户端" }

    return ValidatedLoadNotificationDraft(
        name = normalizedName,
        metric = normalizedMetric,
        threshold = parsedThreshold,
        ratio = parsedRatio,
        interval = parsedInterval,
        clients = normalizedClients
    )
}
