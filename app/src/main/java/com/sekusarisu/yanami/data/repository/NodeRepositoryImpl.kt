package com.sekusarisu.yanami.data.repository

import com.sekusarisu.yanami.data.remote.KomariRpcService
import com.sekusarisu.yanami.data.remote.dto.NodeInfoDto
import com.sekusarisu.yanami.data.remote.dto.NodeStatusDto
import com.sekusarisu.yanami.domain.model.LoadRecord
import com.sekusarisu.yanami.domain.model.Node
import com.sekusarisu.yanami.domain.model.PingRecord
import com.sekusarisu.yanami.domain.model.PingTask
import com.sekusarisu.yanami.domain.repository.NodeRepository
import com.sekusarisu.yanami.domain.repository.NodeRepository.NodeDetailWsEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

/**
 * 节点数据仓库实现
 *
 * - 通过 HTTP POST RPC2 获取节点基本信息（一次性）
 * - 通过 WebSocket RPC 实时流式获取节点状态
 * - 通过 HTTP POST RPC2 获取负载/Ping 历史记录
 * - 所有请求使用 session_token 进行认证
 */
class NodeRepositoryImpl(private val rpcService: KomariRpcService) : NodeRepository {

    override suspend fun getNodeInfos(baseUrl: String, sessionToken: String): List<Node> {
        val infoMap = rpcService.getNodes(baseUrl, sessionToken)
        // 首次也尝试获取一次状态（HTTP）
        val statusMap =
                try {
                    rpcService.getNodesLatestStatus(baseUrl, sessionToken)
                } catch (_: Exception) {
                    emptyMap()
                }
        return mergeAndSort(infoMap, statusMap)
    }

    override suspend fun getNodeRecentStatus(
            baseUrl: String,
            sessionToken: String,
            uuid: String
    ): List<LoadRecord> {
        return rpcService.getNodeRecentStatus(baseUrl, sessionToken, uuid).map { dto ->
            val ramPercent =
                    if (dto.ram.total > 0) dto.ram.used.toDouble() / dto.ram.total * 100 else 0.0
            val diskPercent =
                    if (dto.disk.total > 0) dto.disk.used.toDouble() / dto.disk.total * 100 else 0.0
            LoadRecord(
                    time = dto.updatedAt,
                    cpu = dto.cpu.usage,
                    ramPercent = ramPercent,
                    diskPercent = diskPercent,
                    netIn = dto.network.down,
                    netOut = dto.network.up,
                    load = dto.load.load1,
                    process = dto.process,
                    connections = dto.connections.tcp,
                    connectionsUdp = dto.connections.udp
            )
        }
    }

    override fun observeNodeStatus(
            baseUrl: String,
            sessionToken: String,
            baseNodes: List<Node>
    ): Flow<List<Node>> {
        return rpcService.observeNodesLatestStatus(baseUrl, sessionToken).mapNotNull { event ->
            if (event is KomariRpcService.KomariWsEvent.Status) {
                val statusMap = event.statusMap
                val updated =
                        baseNodes.map { node ->
                            val status = statusMap[node.uuid]
                            if (status != null) {
                                node.copy(
                                        isOnline = status.online,
                                        cpuUsage = status.cpu,
                                        memUsed = status.ram,
                                        memTotal =
                                                if (status.ramTotal > 0) status.ramTotal
                                                else node.memTotal,
                                        swapUsed = status.swap,
                                        swapTotal =
                                                if (status.swapTotal > 0) status.swapTotal
                                                else node.swapTotal,
                                        diskUsed = status.disk,
                                        diskTotal =
                                                if (status.diskTotal > 0) status.diskTotal
                                                else node.diskTotal,
                                        netIn = status.netIn,
                                        netOut = status.netOut,
                                        netTotalUp = status.netTotalUp,
                                        netTotalDown = status.netTotalDown,
                                        uptime = status.uptime,
                                        load1 = status.load,
                                        load5 = status.load5,
                                        load15 = status.load15,
                                        process = status.process,
                                        connectionsTcp = status.connections,
                                        connectionsUdp = status.connectionsUdp
                                )
                            } else {
                                node.copy(isOnline = false)
                            }
                        }
                sortNodes(updated)
            } else {
                null
            }
        }
    }

    override fun observeNodeDetailWs(
            baseUrl: String,
            sessionToken: String,
            uuid: String,
            loadHours: Int?,
            pingHours: Int
    ): Flow<NodeRepository.NodeDetailWsEvent> {
        return rpcService.observeNodesLatestStatus(
                        baseUrl,
                        sessionToken,
                        uuid,
                        loadHours,
                        pingHours
                )
                .mapNotNull { event ->
                    when (event) {
                        is KomariRpcService.KomariWsEvent.Status -> {
                            NodeDetailWsEvent.Status(event.statusMap)
                        }
                        is KomariRpcService.KomariWsEvent.LoadRecords -> {
                            val records = event.records.records[uuid] ?: emptyList()
                            val mapped =
                                    records.map { dto ->
                                        val ramPercent =
                                                if (dto.ramTotal > 0)
                                                        (dto.ram.toDouble() / dto.ramTotal * 100)
                                                else 0.0
                                        val diskPercent =
                                                if (dto.diskTotal > 0)
                                                        (dto.disk.toDouble() / dto.diskTotal * 100)
                                                else 0.0
                                        LoadRecord(
                                                time = dto.time,
                                                cpu = dto.cpu,
                                                ramPercent = ramPercent,
                                                diskPercent = diskPercent,
                                                netIn = dto.netIn,
                                                netOut = dto.netOut,
                                                load = dto.load,
                                                process = dto.process,
                                                connections = dto.connections,
                                                connectionsUdp = dto.connectionsUdp
                                        )
                                    }
                            NodeDetailWsEvent.LoadRecords(mapped)
                        }
                        is KomariRpcService.KomariWsEvent.PingRecords -> {
                            val tasks =
                                    event.records.tasks.map { dto ->
                                        PingTask(
                                                id = dto.id,
                                                name = dto.name,
                                                interval = dto.interval,
                                                min = dto.min,
                                                max = dto.max,
                                                avg = dto.avg,
                                                loss =  dto.loss,
                                                latest = dto.latest,
                                                p50 = dto.p50,
                                                p99 = dto.p99
                                        )
                                    }

                            val taskNameMap = tasks.associate { it.id to it.name }

                            val nodeRecords = event.records.records.filter { it.client == uuid }
                            val mapped =
                                    nodeRecords.map { dto ->
                                        PingRecord(
                                                taskId = dto.taskId,
                                                taskName = taskNameMap[dto.taskId]
                                                                ?: "Task ${dto.taskId}",
                                                time = dto.time,
                                                value = dto.value
                                        )
                                    }

                            NodeDetailWsEvent.PingRecords(tasks, mapped)
                        }
                    }
                }
    }

    // ─── 内部方法 ───

    private fun mergeAndSort(
            infoMap: Map<String, NodeInfoDto>,
            statusMap: Map<String, NodeStatusDto>
    ): List<Node> {
        val nodes =
                infoMap.map { (uuid, info) ->
                    val status = statusMap[uuid]
                    Node(
                            uuid = uuid,
                            name = info.name,
                            region = info.region,
                            group = info.group,
                            isOnline = status?.online ?: false,
                            cpuUsage = status?.cpu ?: 0.0,
                            memUsed = status?.ram ?: 0,
                            memTotal = info.memTotal,
                            swapUsed = status?.swap ?: 0,
                            swapTotal = info.swapTotal,
                            diskUsed = status?.disk ?: 0,
                            diskTotal = info.diskTotal,
                            netIn = status?.netIn ?: 0,
                            netOut = status?.netOut ?: 0,
                            netTotalUp = status?.netTotalUp ?: 0,
                            netTotalDown = status?.netTotalDown ?: 0,
                            uptime = status?.uptime ?: 0,
                            os = info.os,
                            cpuName = info.cpuName,
                            cpuCores = info.cpuCores,
                            weight = info.weight,
                            load1 = status?.load ?: 0.0,
                            load5 = status?.load5 ?: 0.0,
                            load15 = status?.load15 ?: 0.0,
                            process = status?.process ?: 0,
                            connectionsTcp = status?.connections ?: 0,
                            connectionsUdp = status?.connectionsUdp ?: 0,
                            // 详情页额外字段
                            kernelVersion = info.kernelVersion,
                            virtualization = info.virtualization,
                            arch = info.arch,
                            gpuName = info.gpuName,
                            trafficLimit = info.trafficLimit,
                            trafficLimitType = info.trafficLimitType,
                            expiredAt = info.expiredAt
                    )
                }
        return sortNodes(nodes)
    }

    /** 按 weight 升序排列，离线节点放到末尾 */
    private fun sortNodes(nodes: List<Node>): List<Node> {
        return nodes.sortedWith(
                compareByDescending<Node> { it.isOnline }.thenBy { it.weight }.thenBy { it.name }
        )
    }
}
