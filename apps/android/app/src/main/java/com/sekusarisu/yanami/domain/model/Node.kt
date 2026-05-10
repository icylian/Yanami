package com.sekusarisu.yanami.domain.model

import androidx.compose.runtime.Immutable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 领域模型 — 服务器节点
 *
 * 合并节点基本信息（common:getNodes）+ 实时状态（common:getNodesLatestStatus）。 仅保留 UI 层需要的字段。
 */
@Immutable
data class Node(
        val uuid: String,
        val name: String,
        val region: String, // emoji 旗帜
        val group: String,
        val isOnline: Boolean,
        val cpuUsage: Double, // %
        val memUsed: Long, // bytes
        val memTotal: Long,
        val swapUsed: Long,
        val swapTotal: Long,
        val diskUsed: Long,
        val diskTotal: Long,
        val netIn: Long, // bytes/s
        val netOut: Long,
        val netTotalUp: Long, // total bytes
        val netTotalDown: Long,
        val uptime: Long, // seconds
        val os: String,
        val cpuName: String,
        val cpuCores: Int,
        val weight: Int,
        val load1: Double,
        val load5: Double,
        val load15: Double,
        val process: Int,
        val connectionsTcp: Int,
        val connectionsUdp: Int,
        // 详情页额外字段
        val kernelVersion: String = "",
        val virtualization: String = "",
        val arch: String = "",
        val gpuName: String = "",
        val trafficLimit: Long = 0,
        val trafficLimitType: String = "",
        val expiredAt: String? = null,
        // Cache parsed expiry once when the model is created to avoid repeated exception-heavy parsing.
        val expiryInstant: Instant? =
                expiredAt
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { parseNodeExpiryInstant(it, ZoneId.systemDefault()) }
)

@Immutable
data class TrafficLimitUsage(
        val currentUsage: Long,
        val limit: Long,
        val type: String,
        val usagePercent: Double
)

@Immutable
data class NodeExpiryStatus(
        val expiryInstant: Instant,
        val remainingSeconds: Long,
        val isExpired: Boolean
)

fun Node.calculateTrafficLimitUsage(): TrafficLimitUsage? {
        if (trafficLimit <= 0) return null

        val normalizedType = trafficLimitType.lowercase()
        val effectiveType =
                if (normalizedType in SUPPORTED_TRAFFIC_LIMIT_TYPES) normalizedType else "sum"
        val currentUsage =
                when (effectiveType) {
                        "sum" -> netTotalUp + netTotalDown
                        "max" -> maxOf(netTotalUp, netTotalDown)
                        "min" -> minOf(netTotalUp, netTotalDown)
                        "up" -> netTotalUp
                        "down" -> netTotalDown
                        else -> netTotalUp + netTotalDown
                }

        return TrafficLimitUsage(
                currentUsage = currentUsage,
                limit = trafficLimit,
                type = effectiveType,
                usagePercent = currentUsage.toDouble() / trafficLimit * 100
        )
}

private val SUPPORTED_TRAFFIC_LIMIT_TYPES = setOf("sum", "max", "min", "up", "down")

fun Node.calculateExpiryStatus(
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
): NodeExpiryStatus? {
        val parsedExpiryInstant =
                expiryInstant
                        ?: expiredAt?.trim()?.takeIf { it.isNotEmpty() }?.let {
                                parseNodeExpiryInstant(it, zoneId)
                        }
                        ?: return null
        val remainingSeconds = parsedExpiryInstant.epochSecond - now.epochSecond
        return NodeExpiryStatus(
                expiryInstant = parsedExpiryInstant,
                remainingSeconds = remainingSeconds,
                isExpired = remainingSeconds <= 0
        )
}

private fun parseNodeExpiryInstant(value: String, zoneId: ZoneId): Instant? {
        return runCatching { Instant.parse(value) }.getOrNull()
                ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
                ?: runCatching {
                            LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                    .atZone(zoneId)
                                    .toInstant()
                    }
                        .getOrNull()
                ?: runCatching {
                            LocalDateTime.parse(
                                            value,
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                    )
                                    .atZone(zoneId)
                                    .toInstant()
                    }
                        .getOrNull()
                ?: runCatching {
                            LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
                                    .atStartOfDay(zoneId)
                                    .toInstant()
                    }
                        .getOrNull()
}
