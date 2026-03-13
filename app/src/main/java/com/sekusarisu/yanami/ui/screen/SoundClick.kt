package com.sekusarisu.yanami.ui.screen

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

/**
 * Returns an onClick lambda that plays the system click sound and triggers haptic feedback
 * before executing [block]. Safe to call inside loops as it does not use `remember`.
 */
@Composable
fun soundClick(block: () -> Unit): () -> Unit {
    val view = LocalView.current
    return {
        view.playSoundEffect(SoundEffectConstants.CLICK)
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        block()
    }
}
