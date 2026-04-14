package com.sekusarisu.yanami.ui.screen.nodedetail

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.sekusarisu.yanami.data.local.preferences.UserPreferences
import com.sekusarisu.yanami.data.local.preferences.UserPreferencesRepository
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.ui.screen.soundClick
import com.sekusarisu.yanami.domain.model.LoadRecord
import com.sekusarisu.yanami.domain.model.Node
import com.sekusarisu.yanami.domain.model.PingRecord
import com.sekusarisu.yanami.domain.model.PingTask
import com.sekusarisu.yanami.domain.model.calculateTrafficLimitUsage
import com.sekusarisu.yanami.ui.screen.AdaptiveContentPane
import com.sekusarisu.yanami.ui.screen.rememberAdaptiveLayoutInfo
import com.sekusarisu.yanami.ui.screen.nodelist.formatBytes
import com.sekusarisu.yanami.ui.screen.nodelist.formatUptime
import com.sekusarisu.yanami.ui.screen.server.AddServerScreen
import com.sekusarisu.yanami.ui.screen.server.ServerListScreen
import com.sekusarisu.yanami.ui.screen.server.ServerReLoginScreen
import com.sekusarisu.yanami.ui.screen.terminal.SshTerminalScreen
import com.sekusarisu.yanami.ui.traffic.formatTrafficLimitDetail
import com.sekusarisu.yanami.ui.traffic.formatTrafficLimitTypeLabel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import java.time.Instant
import java.time.ZoneId

/**
 * 节点详情页面
 *
 * 展示服务器信息总览、负载历史图表、Ping 延迟监控。
 */
class NodeDetailScreen(private val uuid: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<NodeDetailViewModel> { parametersOf(uuid) }
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val prefsRepo = koinInject<UserPreferencesRepository>()
        val prefs by prefsRepo.preferencesFlow.collectAsState(initial = UserPreferences())
        val chartAnimationEnabled = prefs.chartAnimationEnabled
        val adaptiveInfo = rememberAdaptiveLayoutInfo()

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is NodeDetailContract.Effect.ShowToast -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                    is NodeDetailContract.Effect.NavigateToServerRelogin -> {
                        navigator.replaceAll(ServerListScreen())
                        navigator.push(
                                ServerReLoginScreen(
                                        serverId = effect.serverId,
                                        forceTwoFa = effect.forceTwoFa
                                )
                        )
                    }
                    is NodeDetailContract.Effect.NavigateToServerEdit -> {
                        navigator.replaceAll(ServerListScreen())
                        navigator.push(AddServerScreen(editServerId = effect.serverId))
                    }
                }
            }
        }

        Scaffold(
                topBar = {
                    TopAppBar(
                            title = {
                                Text(
                                        text = state.node?.name
                                                        ?: stringResource(
                                                                R.string.node_detail_title
                                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = soundClick { navigator.pop() }) {
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription =
                                                    stringResource(R.string.action_back)
                                    )
                                }
                            },
                            actions = {
                                // SSH 终端入口：仅节点在线且非游客模式时可用
                                val isOnline = state.node?.isOnline ?: false
                                IconButton(
                                        onClick = soundClick {
                                            navigator.push(
                                                    SshTerminalScreen(
                                                            uuid = uuid,
                                                            nodeName = state.node?.name ?: uuid
                                                    )
                                            )
                                        },
                                        enabled = isOnline && state.authType != AuthType.GUEST
                                ) {
                                    Icon(
                                            imageVector = Icons.Filled.Terminal,
                                            contentDescription =
                                                    stringResource(R.string.action_ssh_terminal)
                                    )
                                }
                            },
                            colors =
                                    TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                    )
                    )
                }
        ) { innerPadding ->
            when {
                state.isLoading -> {
                    LoadingContent(modifier = Modifier.fillMaxSize().padding(innerPadding))
                }
                state.error != null -> {
                    ErrorContent(
                            error = state.error!!,
                            onRetry = { viewModel.onEvent(NodeDetailContract.Event.Retry) },
                            modifier = Modifier.fillMaxSize().padding(innerPadding)
                    )
                }
                state.node != null -> {
                    PullToRefreshBox(
                            isRefreshing = false,
                            onRefresh = { viewModel.onEvent(NodeDetailContract.Event.Refresh) },
                            modifier = Modifier.fillMaxSize().padding(innerPadding)
                    ) {
                        NodeDetailContent(
                                state = state,
                                chartAnimationEnabled = chartAnimationEnabled,
                                isTabletLandscape = adaptiveInfo.isTabletLandscape,
                                onLoadHoursChanged = {
                                    viewModel.onEvent(NodeDetailContract.Event.LoadHoursChanged(it))
                                },
                                onPingHoursChanged = {
                                    viewModel.onEvent(NodeDetailContract.Event.PingHoursChanged(it))
                                }
                        )
                    }
                }
            }
        }
    }
}
