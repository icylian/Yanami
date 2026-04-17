package com.sekusarisu.yanami.data.repository

import com.sekusarisu.yanami.data.remote.KomariAdminPingService
import com.sekusarisu.yanami.data.remote.dto.AdminPingTaskDto
import com.sekusarisu.yanami.domain.model.AdminPingTask
import com.sekusarisu.yanami.domain.model.AdminPingTaskDraft
import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.PingTaskType
import com.sekusarisu.yanami.domain.model.toCreatePayload
import com.sekusarisu.yanami.domain.model.toEditPayload
import com.sekusarisu.yanami.domain.repository.PingTaskRepository

class PingTaskRepositoryImpl(private val service: KomariAdminPingService) : PingTaskRepository {

    override suspend fun listPingTasks(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType
    ): List<AdminPingTask> {
        return service.listPingTasks(baseUrl, sessionToken, authType).map { it.toDomain() }.sortPingTasks()
    }

    override suspend fun addPingTask(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType,
        draft: AdminPingTaskDraft
    ): Int {
        return service.addPingTask(
            baseUrl = baseUrl,
            sessionToken = sessionToken,
            authType = authType,
            payload = draft.toCreatePayload()
        )
    }

    override suspend fun updatePingTasks(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType,
        tasks: List<AdminPingTask>
    ) {
        service.updatePingTasks(
            baseUrl = baseUrl,
            sessionToken = sessionToken,
            authType = authType,
            tasks = tasks.map { it.toEditPayload() }
        )
    }

    override suspend fun deletePingTasks(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType,
        ids: List<Int>
    ) {
        service.deletePingTasks(baseUrl, sessionToken, authType, ids)
    }

    override suspend fun reorderPingTasks(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType,
        weights: Map<Int, Int>
    ) {
        service.reorderPingTasks(baseUrl, sessionToken, authType, weights)
    }
}

private fun AdminPingTaskDto.toDomain(): AdminPingTask =
    AdminPingTask(
        id = id,
        weight = weight,
        name = name,
        clients = clients,
        type = PingTaskType.fromApiValue(type),
        target = target,
        interval = interval
    )

private fun List<AdminPingTask>.sortPingTasks(): List<AdminPingTask> {
    return sortedWith(compareBy<AdminPingTask> { it.weight }.thenBy { it.name }.thenBy { it.id })
}
