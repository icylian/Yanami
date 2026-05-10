package com.sekusarisu.yanami.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.ui.screen.nodelist.formatBytes
import com.sekusarisu.yanami.ui.screen.nodelist.formatSpeed
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.sekusarisu.yanami.MainActivity

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
internal fun WidgetContent(state: WidgetState) {
    val ctx = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        when {
            state.isLoading -> {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ctx.getString(R.string.widget_loading),
                        style = TextStyle(
                            color = GlanceTheme.colors.secondary,
                            fontSize = 13.sp
                        )
                    )
                }
            }
            state.error != null -> {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Text(
                        text = state.error,
                        style = TextStyle(
                            color = GlanceTheme.colors.error,
                            fontSize = 12.sp
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    androidx.glance.Button(
                        text = ctx.getString(R.string.action_retry),
                        onClick = actionRunCallback<RefreshWidgetAction>()
                    )
                }
            }
            else -> {
                // Header: server name | last updated | refresh icon
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = state.serverName.ifBlank { ctx.getString(R.string.widget_title) },
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    if (state.lastUpdated > 0) {
                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            .format(Date(state.lastUpdated))
                        Text(
                            text = ctx.getString(R.string.widget_last_updated, timeStr),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 10.sp
                            ),
                            modifier = GlanceModifier.padding(end = 6.dp)
                        )
                    }
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_refresh),
                        contentDescription = "refresh",
                        contentScale = ContentScale.Fit,
                        modifier = GlanceModifier
                            .size(18.dp)
                            .clickable(actionRunCallback<RefreshWidgetAction>())
                    )
                }

//                Spacer(modifier = GlanceModifier.height(16.dp))

                // Stats row: 总数 / 在线 / 离线
                Row(
                    modifier = GlanceModifier
                        .fillMaxHeight()
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    StatItem(
                        label = ctx.getString(R.string.widget_total),
                        value = state.totalCount.toString(),
                        valueColor = GlanceTheme.colors.onSurface,
                        modifier = GlanceModifier.defaultWeight()
                    )
                    StatItem(
                        label = ctx.getString(R.string.widget_online),
                        value = state.onlineCount.toString(),
                        valueColor = GlanceTheme.colors.primary,
                        modifier = GlanceModifier.defaultWeight()
                    )
                    StatItem(
                        label = ctx.getString(R.string.widget_offline),
                        value = state.offlineCount.toString(),
                        valueColor = GlanceTheme.colors.error,
                        modifier = GlanceModifier.defaultWeight()
                    )
                    NetStatColumn(
                        label = ctx.getString(R.string.node_net_speed),
                        upText = formatSpeed(state.netSpeedUp),
                        downText = formatSpeed(state.netSpeedDown),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    NetStatColumn(
                        label = ctx.getString(R.string.node_net_traffic),
                        upText = formatBytes(state.totalTrafficUp),
                        downText = formatBytes(state.totalTrafficDown),
                        modifier = GlanceModifier.defaultWeight()
                    )
                }

//                Spacer(modifier = GlanceModifier.height(8.dp))
//
//                // Divider
//                Box(
//                    modifier = GlanceModifier
//                        .fillMaxWidth()
//                        .height(1.dp)
//                        .background(GlanceTheme.colors.onTertiaryContainer)
//                ) {}
//
//                Spacer(modifier = GlanceModifier.height(2.dp))
//
//                // Speed + Traffic row
//                Row(
//                    modifier = GlanceModifier.fillMaxWidth(),
//                    verticalAlignment = Alignment.Vertical.CenterVertically
//                ) {
//                    NetStatColumn(
//                        label = ctx.getString(R.string.node_net_speed),
//                        upText = formatSpeed(state.netSpeedUp),
//                        downText = formatSpeed(state.netSpeedDown),
//                        modifier = GlanceModifier.defaultWeight()
//                    )
//                    NetStatColumn(
//                        label = ctx.getString(R.string.node_net_traffic),
//                        upText = formatBytes(state.totalTrafficUp),
//                        downText = formatBytes(state.totalTrafficDown),
//                        modifier = GlanceModifier.defaultWeight()
//                    )
//                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    valueColor: androidx.glance.unit.ColorProvider,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Text(
            text = value,
            style = TextStyle(
                color = valueColor,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            )
        )
        Text(
            text = label,
            style = TextStyle(
                color = valueColor,
                fontSize = 10.sp
            )
        )
    }
}

@Composable
private fun NetStatColumn(
    label: String,
    upText: String,
    downText: String,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "↑ $upText",
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
        )
        Text(
            text = "↓ $downText",
            style = TextStyle(
                color = GlanceTheme.colors.secondary,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
        )
        Text(
            text = label,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp
            )
        )
    }
}
