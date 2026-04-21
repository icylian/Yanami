package com.sekusarisu.yanami.data.repository

import com.sekusarisu.yanami.data.remote.KomariAdminNotificationService
import com.sekusarisu.yanami.data.remote.dto.LoadNotificationDto
import com.sekusarisu.yanami.data.remote.dto.OfflineNotificationDto
import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.LoadNotificationDraft
import com.sekusarisu.yanami.domain.model.LoadNotificationTask
import com.sekusarisu.yanami.domain.model.OfflineNotificationConfig
import com.sekusarisu.yanami.domain.model.toCreatePayload
import com.sekusarisu.yanami.domain.model.toEditPayload
import com.sekusarisu.yanami.domain.repository.NotificationRepository

class NotificationRepositoryImpl(private val service: KomariAdminNotificationService) :
    NotificationRepository {

    override suspend fun listOfflineNotifications(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType
    ): List<OfflineNotificationConfig> {
        return service.listOfflineNotifications(baseUrl, sessionToken, authType).map { it.toDomain() }
    }

    override suspend fun updateOfflineNotifications(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType,
        configs: List<OfflineNotificationConfig>
    ) {
        service.updateOfflineNotifications(
            baseUrl = baseUrl,
            sessionToken = sessionToken,
            authType = authType,
            payload = configs.toEditPayload()
        )
    }

    override suspend fun setOfflineNotificationsEnabled(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType,
        uuids: List<String>,
        enabled: Boolean
    ) {
        service.setOfflineNotificationsEnabled(baseUrl, sessionToken, authType, uuids, enabled)
    }

    override suspend fun listLoadNotifications(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType
    ): List<LoadNotificationTask> {
        return service.listLoadNotifications(baseUrl, sessionToken, authType).map { it.toDomain() }
            .sortedWith(compareBy<LoadNotificationTask> { it.name }.thenBy { it.id })
    }

    override suspend fun addLoadNotification(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType,
        draft: LoadNotificationDraft
    ): Int {
        return service.addLoadNotification(
            baseUrl = baseUrl,
            sessionToken = sessionToken,
            authType = authType,
            payload = draft.toCreatePayload()
        )
    }

    override suspend fun updateLoadNotifications(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType,
        tasks: List<LoadNotificationTask>
    ) {
        service.updateLoadNotifications(
            baseUrl = baseUrl,
            sessionToken = sessionToken,
            authType = authType,
            tasks = tasks.map { it.toEditPayload() }
        )
    }

    override suspend fun deleteLoadNotifications(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType,
        ids: List<Int>
    ) {
        service.deleteLoadNotifications(baseUrl, sessionToken, authType, ids)
    }
}

private fun OfflineNotificationDto.toDomain(): OfflineNotificationConfig =
    OfflineNotificationConfig(
        clientUuid = clientUuid,
        enabled = enabled,
        gracePeriod = gracePeriod,
        lastNotified = lastNotified
    )

private fun LoadNotificationDto.toDomain(): LoadNotificationTask =
    LoadNotificationTask(
        id = id,
        name = name,
        clients = clients,
        metric = metric,
        threshold = threshold,
        ratio = ratio,
        interval = interval,
        lastNotified = lastNotified
    )
