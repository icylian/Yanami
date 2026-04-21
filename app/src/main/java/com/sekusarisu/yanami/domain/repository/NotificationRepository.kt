package com.sekusarisu.yanami.domain.repository

import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.domain.model.LoadNotificationDraft
import com.sekusarisu.yanami.domain.model.LoadNotificationTask
import com.sekusarisu.yanami.domain.model.OfflineNotificationConfig

interface NotificationRepository {
    suspend fun listOfflineNotifications(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType
    ): List<OfflineNotificationConfig>

    suspend fun updateOfflineNotifications(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType,
        configs: List<OfflineNotificationConfig>
    )

    suspend fun setOfflineNotificationsEnabled(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType,
        uuids: List<String>,
        enabled: Boolean
    )

    suspend fun listLoadNotifications(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType
    ): List<LoadNotificationTask>

    suspend fun addLoadNotification(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType,
        draft: LoadNotificationDraft
    ): Int

    suspend fun updateLoadNotifications(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType,
        tasks: List<LoadNotificationTask>
    )

    suspend fun deleteLoadNotifications(
        baseUrl: String,
        sessionToken: String,
        authType: AuthType,
        ids: List<Int>
    )
}
