package com.sekusarisu.yanami.ui.screen.nodedetail

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.sekusarisu.yanami.data.local.preferences.UserPreferences
import com.sekusarisu.yanami.data.local.preferences.UserPreferencesRepository
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.AuthType
import com.sekusarisu.yanami.ui.screen.soundClick
import com.sekusarisu.yanami.ui.screen.rememberAdaptiveLayoutInfo
import com.sekusarisu.yanami.ui.screen.server.AddServerScreen
import com.sekusarisu.yanami.ui.screen.server.ServerListScreen
import com.sekusarisu.yanami.ui.screen.server.ServerReLoginScreen
import com.sekusarisu.yanami.ui.screen.terminal.SshTerminalScreen
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

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
        val state by viewModel.state.collectAsStateWithLifecycle()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val prefsRepo = koinInject<UserPreferencesRepository>()
        val prefs by
                prefsRepo.preferencesFlow.collectAsStateWithLifecycle(initialValue = UserPreferences())
        val chartAnimationEnabled = prefs.chartAnimationEnabled
        val adaptiveInfo = rememberAdaptiveLayoutInfo()

        DisposableEffect(lifecycleOwner, viewModel) {
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                viewModel.onScreenStarted()
            }
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> viewModel.onScreenStarted()
                    Lifecycle.Event.ON_STOP -> viewModel.onScreenStopped()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                viewModel.onScreenStopped()
            }
        }

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
