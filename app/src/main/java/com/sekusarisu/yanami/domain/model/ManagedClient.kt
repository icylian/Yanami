package com.sekusarisu.yanami.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class ManagedClient(
        val uuid: String,
        val token: String,
        val name: String,
        val cpuName: String,
        val virtualization: String,
        val arch: String,
        val cpuCores: Int,
        val os: String,
        val kernelVersion: String,
        val gpuName: String,
        val ipv4: String,
        val ipv6: String,
        val region: String,
        val remark: String,
        val publicRemark: String,
        val memTotal: Long,
        val swapTotal: Long,
        val diskTotal: Long,
        val version: String,
        val weight: Int,
        val price: Double,
        val billingCycle: Int,
        val autoRenewal: Boolean,
        val currency: String,
        val expiredAt: String?,
        val group: String,
        val tags: String,
        val hidden: Boolean,
        val trafficLimit: Long,
        val trafficLimitType: String,
        val createdAt: String,
        val updatedAt: String
)

data class ManagedClientDraft(
        val name: String = "",
        val group: String = "",
        val tags: String = "",
        val remark: String = "",
        val publicRemark: String = "",
        val weight: String = "0",
        val price: String = "0",
        val billingCycle: String = "0",
        val autoRenewal: Boolean = false,
        val currency: String = "$",
        val expiredAt: String = "",
        val hidden: Boolean = false,
        val trafficLimit: String = "0",
        val trafficLimitType: String = "max"
)

data class ManagedClientCreateResult(val uuid: String, val token: String)

fun ManagedClient.calculateExpiryStatus(
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
): NodeExpiryStatus? {
        val raw = expiredAt?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val expiryInstant = parseManagedClientExpiryInstant(raw, zoneId) ?: return null
        val remainingSeconds = expiryInstant.epochSecond - now.epochSecond
        return NodeExpiryStatus(
                expiryInstant = expiryInstant,
                remainingSeconds = remainingSeconds,
                isExpired = remainingSeconds <= 0
        )
}

fun ManagedClient.toDraft(): ManagedClientDraft =
        ManagedClientDraft(
                name = name,
                group = group,
                tags = tags,
                remark = remark,
                publicRemark = publicRemark,
                weight = weight.toString(),
                price = price.toString(),
                billingCycle = billingCycle.toString(),
                autoRenewal = autoRenewal,
                currency = currency,
                expiredAt = expiredAt.orEmpty(),
                hidden = hidden,
                trafficLimit = trafficLimit.toString(),
                trafficLimitType = trafficLimitType
        )

fun ManagedClientDraft.toUpdatePayload(): JsonObject {
        val parsedWeight =
                weight.trim().toIntOrNull()
                        ?: throw IllegalArgumentException("weight 必须是整数")
        val parsedPrice =
                price.trim().toDoubleOrNull()
                        ?: throw IllegalArgumentException("price 必须是数字")
        val parsedBillingCycle =
                billingCycle.trim().toIntOrNull()
                        ?: throw IllegalArgumentException("billing_cycle 必须是整数")
        val parsedTrafficLimit =
                trafficLimit.trim().toLongOrNull()
                        ?: throw IllegalArgumentException("traffic_limit 必须是整数")

        require(name.trim().isNotEmpty()) { "name 不能为空" }
        require(parsedWeight >= 0) { "weight 不能为负数" }
        require(parsedBillingCycle >= 0) { "billing_cycle 不能为负数" }
        require(parsedTrafficLimit >= 0L) { "traffic_limit 不能为负数" }
        require(currency.trim().isNotEmpty()) { "currency 不能为空" }

        return buildJsonObject {
                put("name", name.trim())
                put("group", group.trim())
                put("tags", tags.trim())
                put("remark", remark.trim())
                put("public_remark", publicRemark.trim())
                put("weight", parsedWeight)
                put("price", parsedPrice)
                put("billing_cycle", parsedBillingCycle)
                put("auto_renewal", autoRenewal)
                put("currency", currency.trim())
                if (expiredAt.trim().isEmpty()) {
                        put("expired_at", JsonNull)
                } else {
                        put("expired_at", expiredAt.trim())
                }
                put("hidden", hidden)
                put("traffic_limit", parsedTrafficLimit)
                put("traffic_limit_type", trafficLimitType.trim())
        }
}

private fun parseManagedClientExpiryInstant(value: String, zoneId: ZoneId): Instant? {
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
