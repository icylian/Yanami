package com.sekusarisu.yanami.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.sekusarisu.yanami.ui.screen.nodelist.formatBytes
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Int.dp get() = androidx.compose.ui.unit.Dp(this.toFloat())

val WIDGET_STATE_KEY = stringPreferencesKey("widget_state_json")

class OverviewWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val stateJson = prefs[WIDGET_STATE_KEY]
            val state = if (stateJson != null) {
                try {
                    Json.decodeFromString<WidgetState>(stateJson)
                } catch (_: Exception) {
                    WidgetState()
                }
            } else {
                WidgetState()
            }
            GlanceTheme {
                WidgetContent(state)
            }
        }
    }
}

@Composable
private fun WidgetContent(state: WidgetState) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(12.dp)
    ) {
        when {
            state.isLoading -> {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "更新中…",
                        style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer)
                    )
                }
            }
            state.error != null -> {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Text(
                        text = state.error,
                        style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    androidx.glance.Button(
                        text = "刷新",
                        onClick = actionRunCallback<RefreshWidgetAction>()
                    )
                }
            }
            else -> {
                // Title row
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp, horizontal =  4.dp),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            text = state.serverName.ifBlank { "节点总览" },
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        androidx.glance.Button(
                            text = "↻",
                            modifier = GlanceModifier
                                .padding(horizontal = 8.dp)
                                .height(16.dp),
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            onClick = actionRunCallback<RefreshWidgetAction>()
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    // Stats row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        StatItem(
                            label = "总数",
                            value = state.totalCount.toString(),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        StatItem(
                            label = "在线",
                            value = state.onlineCount.toString(),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        StatItem(
                            label = "离线",
                            value = state.offlineCount.toString(),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = "↑ ${formatBytes(state.totalTrafficUp)}",
                                style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer)
                            )
                            Text(
                                text = "↓ ${formatBytes(state.totalTrafficDown)}",
                                style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer)
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    // Traffic + last updated row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.Bottom,
                        horizontalAlignment = Alignment.Horizontal.End
                    ) {
                        if (state.lastUpdated > 0) {
                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault())
                                .format(Date(state.lastUpdated))
                            Text(
                                text = "更新时间: $timeStr",
                                style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: GlanceModifier = GlanceModifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Text(
            text = value,
            style = TextStyle(
                color = GlanceTheme.colors.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        )
        Text(
            text = label,
            style = TextStyle(
                color = GlanceTheme.colors.onPrimaryContainer,
                fontSize = 10.sp
            )
        )
    }
}
