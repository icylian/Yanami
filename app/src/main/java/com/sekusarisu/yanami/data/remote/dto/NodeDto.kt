package com.sekusarisu.yanami.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ─── JSON-RPC 2.0 通用封装 ───

@Serializable
data class RpcRequest(
        val jsonrpc: String = "2.0",
        val method: String,
        val params: JsonElement? = null,
        val id: Int = 1
)

@Serializable
data class RpcResponse<T>(
        val jsonrpc: String = "2.0",
        val id: Int = 1,
        val result: T? = null,
        val error: RpcError? = null
)

@Serializable data class RpcError(val code: Int = 0, val message: String = "")

// ─── common:getNodes 返回的节点基本信息 ───

@Serializable
data class NodeInfoDto(
        val uuid: String = "",
        val name: String = "",
        @SerialName("cpu_name") val cpuName: String = "",
        val virtualization: String = "",
        val arch: String = "",
        @SerialName("cpu_cores") val cpuCores: Int = 0,
        val os: String = "",
        @SerialName("kernel_version") val kernelVersion: String = "",
        @SerialName("gpu_name") val gpuName: String = "",
        val region: String = "",
        @SerialName("mem_total") val memTotal: Long = 0,
        @SerialName("swap_total") val swapTotal: Long = 0,
        @SerialName("disk_total") val diskTotal: Long = 0,
        val weight: Int = 0,
        val price: Double = 0.0,
        @SerialName("billing_cycle") val billingCycle: Int = 0,
        @SerialName("auto_renewal") val autoRenewal: Boolean = false,
        val currency: String = "",
        @SerialName("expired_at") val expiredAt: String? = null,
        val group: String = "",
        val tags: String = "",
        val hidden: Boolean = false,
        @SerialName("traffic_limit") val trafficLimit: Long = 0,
        @SerialName("traffic_limit_type") val trafficLimitType: String = "",
        @SerialName("created_at") val createdAt: String = "",
        @SerialName("updated_at") val updatedAt: String = ""
)

// ─── common:getNodesLatestStatus 返回的扁平化实时状态 ───

@Serializable
data class NodeStatusDto(
        val cpu: Double = 0.0,
        val gpu: Double = 0.0,
        val ram: Long = 0,
        @SerialName("ram_total") val ramTotal: Long = 0,
        val swap: Long = 0,
        @SerialName("swap_total") val swapTotal: Long = 0,
        val load: Double = 0.0,
        val load5: Double = 0.0,
        val load15: Double = 0.0,
        val temp: Double = 0.0,
        val disk: Long = 0,
        @SerialName("disk_total") val diskTotal: Long = 0,
        @SerialName("net_in") val netIn: Long = 0,
        @SerialName("net_out") val netOut: Long = 0,
        @SerialName("net_total_up") val netTotalUp: Long = 0,
        @SerialName("net_total_down") val netTotalDown: Long = 0,
        val process: Int = 0,
        val connections: Int = 0,
        @SerialName("connections_udp") val connectionsUdp: Int = 0,
        val uptime: Long = 0,
        val ping: Map<String, PingDetailDto>? = null,
        val online: Boolean = false,
        @SerialName("updated_at") val updatedAt: String = ""
)

// ─── Ping 详情对象（实际 API 返回的结构） ───

@Serializable
data class PingDetailDto(
        val name: String = "",
        val latest: Double = 0.0,
        val avg: Double = 0.0,
        val tail: Double = 0.0,
        val loss: Double = 0.0,
        val min: Double = 0.0,
        val max: Double = 0.0
)

// ─── common:getRecords (type=load) 返回的负载历史记录 ───

@Serializable
data class LoadRecordDto(
        val client: String = "",
        val time: String = "",
        val cpu: Double = 0.0,
        val gpu: Double = 0.0,
        val ram: Long = 0,
        @SerialName("ram_total") val ramTotal: Long = 0,
        val swap: Long = 0,
        @SerialName("swap_total") val swapTotal: Long = 0,
        val load: Double = 0.0,
        val temp: Double = 0.0,
        val disk: Long = 0,
        @SerialName("disk_total") val diskTotal: Long = 0,
        @SerialName("net_in") val netIn: Long = 0,
        @SerialName("net_out") val netOut: Long = 0,
        @SerialName("net_total_up") val netTotalUp: Long = 0,
        @SerialName("net_total_down") val netTotalDown: Long = 0,
        val process: Int = 0,
        val connections: Int = 0,
        @SerialName("connections_udp") val connectionsUdp: Int = 0
)

@Serializable
data class LoadRecordsResponseDto(
        val count: Int = 0,
        val from: String = "",
        val to: String = "",
        val records: Map<String, List<LoadRecordDto>> = emptyMap()
)

// ─── common:getRecords (type=ping) 返回的 Ping 历史记录 ───

@Serializable
data class PingRecordDto(
        @SerialName("task_id") val taskId: Int = 0,
        val time: String = "",
        val value: Double = 0.0,
        val client: String = ""
)

@Serializable
data class PingTaskDto(
        val id: Int = 0,
        val interval: Int = 0,
        val name: String = "",
        val type: String = "",
        val total: Int = 0,
        val loss: Double = 0.0,
        val latest: Double = 0.0,
        val min: Double = 0.0,
        val max: Double = 0.0,
        val avg: Double = 0.0,
        val p50: Double = 0.0,
        val p99: Double = 0.0,
        @SerialName("p99_p50_ratio") val p99p50Ratio: Double = 0.0
)

@Serializable
data class PingRecordsResponseDto(
        val count: Int = 0,
        val from: String = "",
        val to: String = "",
        val records: List<PingRecordDto> = emptyList(),
        val tasks: List<PingTaskDto> = emptyList()
)

// ─── common:getNodeRecentStatus 返回的嵌套实时状态（1 分钟未降采样数据）───

@Serializable data class RecentCpuDto(val usage: Double = 0.0)

@Serializable data class RecentRamDto(val total: Long = 0, val used: Long = 0)

@Serializable data class RecentSwapDto(val total: Long = 0, val used: Long = 0)

@Serializable
data class RecentLoadDto(
        val load1: Double = 0.0,
        val load5: Double = 0.0,
        val load15: Double = 0.0
)

@Serializable data class RecentDiskDto(val total: Long = 0, val used: Long = 0)

@Serializable
data class RecentNetworkDto(
        val up: Long = 0,
        val down: Long = 0,
        val totalUp: Long = 0,
        val totalDown: Long = 0
)

@Serializable data class RecentConnectionsDto(val tcp: Int = 0, val udp: Int = 0)

@Serializable
data class RecentStatusItemDto(
        val cpu: RecentCpuDto = RecentCpuDto(),
        val ram: RecentRamDto = RecentRamDto(),
        val swap: RecentSwapDto = RecentSwapDto(),
        val load: RecentLoadDto = RecentLoadDto(),
        val disk: RecentDiskDto = RecentDiskDto(),
        val network: RecentNetworkDto = RecentNetworkDto(),
        val connections: RecentConnectionsDto = RecentConnectionsDto(),
        val uptime: Long = 0,
        val process: Int = 0,
        val message: String = "",
        @SerialName("updated_at") val updatedAt: String = ""
)
