package com.sekusarisu.yanami.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ManagedClientDto(
        val uuid: String = "",
        val token: String = "",
        val name: String = "",
        @SerialName("cpu_name") val cpuName: String = "",
        val virtualization: String = "",
        val arch: String = "",
        @SerialName("cpu_cores") val cpuCores: Int = 0,
        val os: String = "",
        @SerialName("kernel_version") val kernelVersion: String = "",
        @SerialName("gpu_name") val gpuName: String = "",
        val ipv4: String = "",
        val ipv6: String = "",
        val region: String = "",
        val remark: String = "",
        @SerialName("public_remark") val publicRemark: String = "",
        @SerialName("mem_total") val memTotal: Long = 0,
        @SerialName("swap_total") val swapTotal: Long = 0,
        @SerialName("disk_total") val diskTotal: Long = 0,
        val version: String = "",
        val weight: Int = 0,
        val price: Double = 0.0,
        @SerialName("billing_cycle") val billingCycle: Int = 0,
        @SerialName("auto_renewal") val autoRenewal: Boolean = false,
        val currency: String = "$",
        @SerialName("expired_at") val expiredAt: String? = null,
        val group: String = "",
        val tags: String = "",
        val hidden: Boolean = false,
        @SerialName("traffic_limit") val trafficLimit: Long = 0,
        @SerialName("traffic_limit_type") val trafficLimitType: String = "max",
        @SerialName("created_at") val createdAt: String = "",
        @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable data class ClientTokenDto(val token: String = "")

@Serializable
data class ClientCreateResultDto(val uuid: String = "", val token: String = "")
