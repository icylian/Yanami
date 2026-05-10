package com.sekusarisu.yanami.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AdaptiveLayoutInfo(
        val widthDp: Int,
        val heightDp: Int,
        val isLandscape: Boolean,
        val isTablet: Boolean,
        val isTabletLandscape: Boolean
)

@Composable
fun rememberAdaptiveLayoutInfo(): AdaptiveLayoutInfo {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isTabletLandscape = isLandscape && (isTablet || configuration.screenWidthDp >= 900)
    return AdaptiveLayoutInfo(
            widthDp = configuration.screenWidthDp,
            heightDp = configuration.screenHeightDp,
            isLandscape = isLandscape,
            isTablet = isTablet,
            isTabletLandscape = isTabletLandscape
    )
}

@Composable
fun AdaptiveContentPane(
        modifier: Modifier = Modifier,
        maxWidth: Dp = 840.dp,
        content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.fillMaxWidth().widthIn(max = maxWidth), content = content)
    }
}
