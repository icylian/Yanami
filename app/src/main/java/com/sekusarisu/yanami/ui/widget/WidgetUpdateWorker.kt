package com.sekusarisu.yanami.ui.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sekusarisu.yanami.domain.repository.NodeRepository
import com.sekusarisu.yanami.domain.repository.ServerRepository
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val serverRepository: ServerRepository by inject()
    private val nodeRepository: NodeRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            val server = serverRepository.getActive()
            if (server == null) {
                updateAllWidgets(WidgetState(isLoading = false, error = "无活跃服务器"))
                return Result.success()
            }

            updateAllWidgets(WidgetState(isLoading = true, serverName = server.name))

            val sessionToken = serverRepository.ensureSessionToken(server)
            val nodes = nodeRepository.getNodeInfos(server.baseUrl, sessionToken)
            val onlineNodes = nodes.filter { it.isOnline }

            updateAllWidgets(
                WidgetState(
                    isLoading = false,
                    serverName = server.name,
                    totalCount = nodes.size,
                    onlineCount = onlineNodes.size,
                    offlineCount = nodes.size - onlineNodes.size,
                    totalTrafficUp = onlineNodes.sumOf { it.netTotalUp },
                    totalTrafficDown = onlineNodes.sumOf { it.netTotalDown },
                    lastUpdated = System.currentTimeMillis()
                )
            )
            Result.success()
        } catch (e: Exception) {
            updateAllWidgets(WidgetState(isLoading = false, error = e.message ?: "更新失败"))
            Result.failure()
        }
    }

    private suspend fun updateAllWidgets(state: WidgetState) {
        val stateJson = Json.encodeToString(WidgetState.serializer(), state)
        val glanceIds = GlanceAppWidgetManager(applicationContext)
            .getGlanceIds(OverviewWidget::class.java)
        for (glanceId in glanceIds) {
            updateAppWidgetState(applicationContext, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[WIDGET_STATE_KEY] = stateJson
                }
            }
            OverviewWidget().update(applicationContext, glanceId)
        }
    }

    companion object {
        private const val WORK_NAME = "yanami_widget_update_periodic"

        fun enqueue(context: Context, intervalMinutes: Int = 30, immediate: Boolean = false) {
            val wm = WorkManager.getInstance(context)
            if (immediate) {
                wm.enqueue(OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build())
            }
            val periodic = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                intervalMinutes.toLong(), TimeUnit.MINUTES
            ).build()
            wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, periodic)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
