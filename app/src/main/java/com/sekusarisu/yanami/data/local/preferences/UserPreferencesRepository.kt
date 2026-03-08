package com.sekusarisu.yanami.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
/** DataStore 扩展属性 */
private val Context.dataStore: DataStore<Preferences> by
        preferencesDataStore(name = "user_preferences")

/**
 * 用户偏好仓库
 *
 * 使用 DataStore Preferences 持久化：
 * - 主题颜色 (dynamic / teal / blue / purple / pink / orange / green)
 * - 深色模式 (system / light / dark)
 * - 语言 (system / zh / en / ja)
 */
class UserPreferencesRepository(private val context: Context) {

    companion object {
        private val THEME_COLOR_KEY = stringPreferencesKey("theme_color")
        private val DARK_MODE_KEY = stringPreferencesKey("dark_mode")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val TERMINAL_FONT_SIZE_KEY = intPreferencesKey("terminal_font_size")
        private val FONT_SCALE_KEY = floatPreferencesKey("font_scale")
        private val AUTO_ENTER_NODELIST_KEY = booleanPreferencesKey("auto_enter_nodelist")
        const val DEFAULT_TERMINAL_FONT_SIZE = 20
    }

    /** 偏好数据流 */
    val preferencesFlow: Flow<UserPreferences> =
            context.dataStore.data.map { prefs ->
                UserPreferences(
                        themeColorKey = prefs[THEME_COLOR_KEY] ?: "dynamic",
                        darkModeKey = prefs[DARK_MODE_KEY] ?: "system",
                        languageKey = prefs[LANGUAGE_KEY] ?: "system",
                        fontScale = prefs[FONT_SCALE_KEY] ?: 1.0f,
                        autoEnterNodeList = prefs[AUTO_ENTER_NODELIST_KEY] ?: false
                )
            }

    /** 设置主题颜色 */
    suspend fun setThemeColor(key: String) {
        context.dataStore.edit { it[THEME_COLOR_KEY] = key }
    }

    /** 设置深色模式 */
    suspend fun setDarkMode(key: String) {
        context.dataStore.edit { it[DARK_MODE_KEY] = key }
    }

    /** 设置语言 */
    suspend fun setLanguage(key: String) {
        context.dataStore.edit { it[LANGUAGE_KEY] = key }
    }

    /** 终端字号 Flow */
    val terminalFontSize: Flow<Int> =
            context.dataStore.data.map { prefs ->
                prefs[TERMINAL_FONT_SIZE_KEY] ?: DEFAULT_TERMINAL_FONT_SIZE
            }

    /** 保存终端字号 */
    suspend fun setTerminalFontSize(size: Int) {
        context.dataStore.edit { it[TERMINAL_FONT_SIZE_KEY] = size }
    }

    /** 设置字体缩放比例 */
    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { it[FONT_SCALE_KEY] = scale }
    }

    /** 设置自动进入节点列表 */
    suspend fun setAutoEnterNodeList(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_ENTER_NODELIST_KEY] = enabled }
    }
}

/** 用户偏好数据类 */
data class UserPreferences(
        val themeColorKey: String = "dynamic",
        val darkModeKey: String = "system",
        val languageKey: String = "system",
        val fontScale: Float = 1.0f,
        val autoEnterNodeList: Boolean = false
)
