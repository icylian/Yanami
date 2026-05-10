package com.sekusarisu.yanami.ui.screen.settings

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import cafe.adriel.voyager.core.model.screenModelScope
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.data.backup.ConfigBackupManager
import com.sekusarisu.yanami.data.local.preferences.UserPreferencesRepository
import com.sekusarisu.yanami.mvi.MviViewModel
import com.sekusarisu.yanami.mvi.UiEffect
import com.sekusarisu.yanami.mvi.UiEvent
import com.sekusarisu.yanami.mvi.UiState
import com.sekusarisu.yanami.ui.theme.ThemeColor
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** 设置页面 State */
data class SettingsState(
        val themeColor: ThemeColor = ThemeColor.DYNAMIC,
        val darkMode: String = "system",
        val language: String = "system",
        val fontScale: Float = 1.0f,
        val autoEnterNodeList: Boolean = false,
        val chartAnimationEnabled: Boolean = true,
        val biometricEnabled: Boolean = false,
        val isBackupInProgress: Boolean = false
) : UiState

/** 设置页面 Events */
sealed interface SettingsEvent : UiEvent {
    data class SetThemeColor(val color: ThemeColor) : SettingsEvent
    data class SetDarkMode(val mode: String) : SettingsEvent
    data class SetLanguage(val lang: String) : SettingsEvent
    data class SetFontScale(val scale: Float) : SettingsEvent
    data class SetAutoEnterNodeList(val enabled: Boolean) : SettingsEvent
    data class SetChartAnimation(val enabled: Boolean) : SettingsEvent
    data class SetBiometricEnabled(val enabled: Boolean) : SettingsEvent
}

/** 设置页面 Effects */
sealed interface SettingsEffect : UiEffect {
    data class ShowToast(val message: String) : SettingsEffect
}

/** 设置 ViewModel */
class SettingsViewModel(
        private val prefsRepo: UserPreferencesRepository,
        private val configBackupManager: ConfigBackupManager,
        private val context: Context
) :
        MviViewModel<SettingsState, SettingsEvent, SettingsEffect>(SettingsState()) {

    init {
        prefsRepo
                .preferencesFlow
                .onEach { prefs ->
                    setState {
                        copy(
                                themeColor = ThemeColor.fromKey(prefs.themeColorKey),
                                darkMode = prefs.darkModeKey,
                                language = prefs.languageKey,
                                fontScale = prefs.fontScale,
                                autoEnterNodeList = prefs.autoEnterNodeList,
                                chartAnimationEnabled = prefs.chartAnimationEnabled,
                                biometricEnabled = prefs.biometricEnabled
                        )
                    }
                }
                .launchIn(screenModelScope)
    }

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetThemeColor -> {
                screenModelScope.launch { prefsRepo.setThemeColor(event.color.key) }
            }
            is SettingsEvent.SetDarkMode -> {
                screenModelScope.launch { prefsRepo.setDarkMode(event.mode) }
            }
            is SettingsEvent.SetLanguage -> {
                screenModelScope.launch {
                    prefsRepo.setLanguage(event.lang)
                    applyLocale(event.lang)
                }
            }
            is SettingsEvent.SetFontScale -> {
                screenModelScope.launch { prefsRepo.setFontScale(event.scale) }
            }
            is SettingsEvent.SetAutoEnterNodeList -> {
                screenModelScope.launch { prefsRepo.setAutoEnterNodeList(event.enabled) }
            }
            is SettingsEvent.SetChartAnimation -> {
                screenModelScope.launch { prefsRepo.setChartAnimation(event.enabled) }
            }
            is SettingsEvent.SetBiometricEnabled -> {
                screenModelScope.launch { prefsRepo.setBiometricEnabled(event.enabled) }
            }
        }
    }

    fun exportConfig(uri: Uri) {
        if (currentState.isBackupInProgress) return
        setState { copy(isBackupInProgress = true) }
        screenModelScope.launch {
            try {
                val summary = configBackupManager.exportToUri(uri)
                sendEffect(
                        SettingsEffect.ShowToast(
                                context.getString(
                                        R.string.settings_export_success,
                                        summary.serverCount,
                                        summary.snippetCount
                                )
                        )
                )
            } catch (e: Exception) {
                sendEffect(
                        SettingsEffect.ShowToast(
                                context.getString(
                                        R.string.settings_export_failed,
                                        e.message ?: context.getString(R.string.settings_backup_unknown_error)
                                )
                        )
                )
            } finally {
                setState { copy(isBackupInProgress = false) }
            }
        }
    }

    fun importConfig(uri: Uri) {
        if (currentState.isBackupInProgress) return
        setState { copy(isBackupInProgress = true) }
        screenModelScope.launch {
            try {
                val summary = configBackupManager.importFromUri(uri)
                sendEffect(
                        SettingsEffect.ShowToast(
                                context.getString(
                                        R.string.settings_import_success,
                                        summary.addedServerCount,
                                        summary.updatedServerCount,
                                        summary.addedSnippetCount,
                                        summary.updatedSnippetCount
                                )
                        )
                )
                if (summary.skippedServerCount > 0 || summary.skippedSnippetCount > 0) {
                    sendEffect(
                            SettingsEffect.ShowToast(
                                    context.getString(
                                            R.string.settings_import_skipped,
                                            summary.skippedServerCount,
                                            summary.skippedSnippetCount
                                    )
                            )
                    )
                }
            } catch (e: Exception) {
                sendEffect(
                        SettingsEffect.ShowToast(
                                context.getString(
                                        R.string.settings_import_failed,
                                        e.message ?: context.getString(R.string.settings_backup_unknown_error)
                                )
                        )
                )
            } finally {
                setState { copy(isBackupInProgress = false) }
            }
        }
    }

    /** 通过 AppCompatDelegate 应用 locale 切换 */
    private fun applyLocale(langKey: String) {
        val localeList =
                if (langKey == "system") {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(langKey)
                }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
