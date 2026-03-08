package com.sekusarisu.yanami

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.sekusarisu.yanami.data.local.preferences.UserPreferences
import com.sekusarisu.yanami.data.local.preferences.UserPreferencesRepository
import com.sekusarisu.yanami.data.remote.UpdateCheckService
import com.sekusarisu.yanami.domain.repository.ServerRepository
import com.sekusarisu.yanami.ui.screen.nodelist.NodeListScreen
import com.sekusarisu.yanami.ui.screen.server.ServerListScreen
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
                    if (screens != null) {
                        Navigator(screens) { navigator -> SlideTransition(navigator) }
                    }
                }
            }
        }
    }
}
