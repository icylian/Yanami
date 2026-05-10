package com.sekusarisu.yanami

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.sekusarisu.yanami.data.local.preferences.UserPreferences
import com.sekusarisu.yanami.data.local.preferences.UserPreferencesRepository
import com.sekusarisu.yanami.data.remote.UpdateCheckService
import com.sekusarisu.yanami.domain.repository.ServerRepository
import com.sekusarisu.yanami.ui.screen.rememberAdaptiveLayoutInfo
import com.sekusarisu.yanami.ui.screen.client.ClientCreateScreen
import com.sekusarisu.yanami.ui.screen.client.ClientEditScreen
import com.sekusarisu.yanami.ui.screen.client.ClientManagementScreen
import com.sekusarisu.yanami.ui.screen.nodedetail.NodeDetailScreen
import com.sekusarisu.yanami.ui.screen.nodelist.NodeListScreen
import com.sekusarisu.yanami.ui.screen.server.AddServerScreen
import com.sekusarisu.yanami.ui.screen.server.ServerListScreen
import com.sekusarisu.yanami.ui.screen.server.ServerReLoginScreen
import com.sekusarisu.yanami.ui.screen.settings.AboutScreen
import com.sekusarisu.yanami.ui.screen.settings.SettingsHubScreen
import com.sekusarisu.yanami.ui.screen.settings.SettingsScreen
import com.sekusarisu.yanami.ui.screen.terminal.SshTerminalScreen
import com.sekusarisu.yanami.ui.theme.ThemeColor
import com.sekusarisu.yanami.ui.theme.YanamiTheme
import kotlinx.coroutines.flow.first
import org.koin.android.ext.android.inject

/**
 * 主入口 Activity
 *
 * 从 UserPreferencesRepository 读取主题偏好，传入 YanamiTheme。 使用 AppCompatActivity 以支持
 * AppCompatDelegate.setApplicationLocales 应用内语言切换。
 */
class MainActivity : AppCompatActivity() {

    private val prefsRepo: UserPreferencesRepository by inject()
    private val serverRepo: ServerRepository by inject()
    private val updateCheckService: UpdateCheckService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val prefs by prefsRepo.preferencesFlow.collectAsState(initial = UserPreferences())

            val themeColor = ThemeColor.fromKey(prefs.themeColorKey)
            val darkTheme =
                    when (prefs.darkModeKey) {
                        "light" -> false
                        "dark" -> true
                        else -> isSystemInDarkTheme()
                    }

            // 解析初始导航栈
            var initialScreens by remember { mutableStateOf<List<Screen>?>(null) }
            // 生物识别通过标志（未启用时直接为 true）
            var authReady by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                val initPrefs = prefsRepo.preferencesFlow.first()
                val screens = if (initPrefs.autoEnterNodeList) {
                    val activeServer = serverRepo.getActive()
                    if (activeServer != null) {
                        listOf(ServerListScreen(), NodeListScreen())
                    } else {
                        listOf(ServerListScreen())
                    }
                } else {
                    listOf(ServerListScreen())
                }
                initialScreens = screens

                if (!initPrefs.biometricEnabled) {
                    authReady = true
                } else {
                    val biometricManager = BiometricManager.from(this@MainActivity)
                    val canAuth = biometricManager.canAuthenticate(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
                        showBiometricPrompt(
                            onSuccess = { authReady = true },
                            onError = { finish() }
                        )
                    } else {
                        // 设备不支持或未注册凭据，放行
                        authReady = true
                    }
                }

                // 静默检查更新
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, 0).versionCode
                }
                updateCheckService.checkForUpdateSilent(versionCode)
            }

            YanamiTheme(themeColor = themeColor, darkTheme = darkTheme) {
                val currentDensity = LocalDensity.current
                val adjustedDensity = Density(
                        density = currentDensity.density,
                        fontScale = currentDensity.fontScale * prefs.fontScale
                )
                CompositionLocalProvider(LocalDensity provides adjustedDensity) {
                    val screens = initialScreens
                    if (screens != null && authReady) {
                        Navigator(screens) { navigator ->
                            val adaptiveInfo = rememberAdaptiveLayoutInfo()
                            val currentScreen = navigator.lastItem
                            var hasActiveServer by remember { mutableStateOf(false) }

                            LaunchedEffect(currentScreen) {
                                hasActiveServer = serverRepo.getActive() != null
                            }

                            if (adaptiveInfo.isTabletLandscape) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                ) {
                                    MainNavigationRail(
                                            currentScreen = currentScreen,
                                            hasActiveServer = hasActiveServer,
                                            onServersClick = {
                                                navigator.replaceAll(ServerListScreen())
                                            },
                                            onNodesClick = {
                                                if (hasActiveServer) {
                                                    navigator.replaceAll(
                                                            listOf(
                                                                    ServerListScreen(),
                                                                    NodeListScreen()
                                                            )
                                                    )
                                                }
                                            },
                                            onSettingsClick = {
                                                navigator.replaceAll(
                                                        listOf(
                                                                ServerListScreen(),
                                                                SettingsHubScreen()
                                                        )
                                                )
                                            }
                                    )
                                    Box(
                                            modifier =
                                                    Modifier.weight(1f)
                                                            .fillMaxHeight()
                                    ) {
                                        SlideTransition(navigator)
                                    }
                                }
                            } else {
                                SlideTransition(navigator)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit, onError: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError()
            }
            override fun onAuthenticationFailed() {
                // 生物识别不匹配，弹窗保持打开，不关闭应用
            }
        }
        val biometricPrompt = BiometricPrompt(this, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        biometricPrompt.authenticate(promptInfo)
    }
}

private enum class MainRailDestination {
    SERVERS,
    NODES,
    SETTINGS
}

private fun resolveMainRailDestination(screen: Screen): MainRailDestination =
        when (screen) {
            is NodeListScreen,
            is NodeDetailScreen,
            is SshTerminalScreen,
            is ClientManagementScreen,
            is ClientCreateScreen,
            is ClientEditScreen -> MainRailDestination.NODES
            is SettingsHubScreen, is SettingsScreen, is AboutScreen -> MainRailDestination.SETTINGS
            is ServerListScreen, is AddServerScreen, is ServerReLoginScreen -> MainRailDestination.SERVERS
            else -> MainRailDestination.SERVERS
        }

@Composable
private fun MainNavigationRail(
        currentScreen: Screen,
        hasActiveServer: Boolean,
        onServersClick: () -> Unit,
        onNodesClick: () -> Unit,
        onSettingsClick: () -> Unit
) {
    val selectedDestination = resolveMainRailDestination(currentScreen)
    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.weight(1f))
        NavigationRailItem(
                selected = selectedDestination == MainRailDestination.SERVERS,
                onClick = onServersClick,
                icon = { androidx.compose.material3.Icon(Icons.Default.Dns, contentDescription = null) },
                label = { Text(stringResource(R.string.server_management)) }
        )
        NavigationRailItem(
                selected = selectedDestination == MainRailDestination.NODES,
                onClick = onNodesClick,
                enabled = hasActiveServer,
                icon = { androidx.compose.material3.Icon(Icons.Default.Storage, contentDescription = null) },
                label = { Text(stringResource(R.string.node_list)) }
        )
        NavigationRailItem(
                selected = selectedDestination == MainRailDestination.SETTINGS,
                onClick = onSettingsClick,
                icon = { androidx.compose.material3.Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text(stringResource(R.string.settings_title)) }
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}
