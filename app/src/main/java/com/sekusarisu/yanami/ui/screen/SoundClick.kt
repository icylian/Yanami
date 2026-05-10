package com.sekusarisu.yanami.ui.screen

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView

/**
 * Returns an onClick lambda that plays the system click sound and triggers haptic feedback
 * before executing [block]. The returned lambda stays stable for the same view.
 */
@Composable
fun soundClick(block: () -> Unit): () -> Unit {
    val view = LocalView.current
    val latestBlock by rememberUpdatedState(block)
    return remember(view) {
        {
            view.playSoundEffect(SoundEffectConstants.CLICK)
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            latestBlock()
        }
    }
}
