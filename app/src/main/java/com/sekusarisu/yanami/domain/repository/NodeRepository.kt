package com.sekusarisu.yanami.domain.repository

import com.sekusarisu.yanami.data.remote.dto.NodeStatusDto
import com.sekusarisu.yanami.domain.model.LoadRecord
import com.sekusarisu.yanami.domain.model.Node
import com.sekusarisu.yanami.domain.model.PingRecord
import com.sekusarisu.yanami.domain.model.PingTask
import kotlinx.coroutines.flow.Flow

/**
 * 节点数据仓库接口
 *
 * 提供获取节点信息和实时状态的能力。 所有方法使用 session_token 进行认证。
 */
interface NodeRepository {

        /**
         * 获取所有节点基本信息（一次性，HTTP POST）
         *
         * 内部调用 common:getNodes 获取静态节点信息。
         */
        suspend fun getNodeInfos(baseUrl: String, sessionToken: String): List<Node>

        /**
         * 获取节点最近 1 分钟未降采样状态数据（HTTP POST）
         *
         * 用于实时模式切换时作为图表 seed，约 60 条 1 秒粒度数据。
         */
        suspend fun getNodeRecentStatus(
                baseUrl: String,
                sessionToken: String,
                uuid: String
        ): List<LoadRecord>

        /**
         * 通过 WebSocket RPC 实时观测节点状态变化
         *
         * 返回 Flow，持续 emit 合并后的节点列表（基本信息 + 最新状态）。 调用方应传入已获取的节点基本信息列表。
         */
        fun observeNodeStatus(
                baseUrl: String,
                sessionToken: String,
                baseNodes: List<Node>
        ): Flow<List<Node>>

        sealed interface NodeDetailWsEvent {
                data class Status(val statusMap: Map<String, NodeStatusDto>) : NodeDetailWsEvent
                data class LoadRecords(val records: List<LoadRecord>) : NodeDetailWsEvent
                data class PingRecords(val tasks: List<PingTask>, val records: List<PingRecord>) :
                        NodeDetailWsEvent
        }

        /** 通过 WebSocket RPC 多路复用，获取详细信息 (包含 Status, LoadHistory, PingHistory) */
        fun observeNodeDetailWs(
                baseUrl: String,
                sessionToken: String,
                uuid: String,
                loadHours: Int?,
                pingHours: Int
        ): Flow<NodeDetailWsEvent>
}
