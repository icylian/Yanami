package com.sekusarisu.yanami.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Yanami MD3 主题
 *
 * - 支持多种预设颜色方案 (ThemeColor 枚举)
 * - ThemeColor.DYNAMIC + Android 12+: 壁纸动态取色
 * - ThemeColor.DYNAMIC + 低版本: 回退到 TEAL 配色
 * - 其他: 使用对应预设颜色的 light/dark ColorScheme
 */
@Composable
fun YanamiTheme(
        themeColor: ThemeColor = ThemeColor.DYNAMIC,
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit
) {
        val colorScheme =
                when {
                        themeColor == ThemeColor.DYNAMIC &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                                val context = LocalContext.current
                                if (darkTheme) dynamicDarkColorScheme(context)
                                else dynamicLightColorScheme(context)
                        }
                        themeColor == ThemeColor.DYNAMIC -> {
                                // 低版本不支持动态取色，回退到 Teal
                                if (darkTheme) ThemeColor.TEAL.darkScheme!!
                                else ThemeColor.TEAL.lightScheme!!
                        }
                        else -> {
                                if (darkTheme) themeColor.darkScheme!! else themeColor.lightScheme!!
                        }
                }

        MaterialTheme(colorScheme = colorScheme, typography = YanamiTypography, content = content)
}
