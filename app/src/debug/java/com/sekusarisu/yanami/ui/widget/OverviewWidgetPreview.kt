package com.sekusarisu.yanami.ui.widget

import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme

@androidx.glance.preview.ExperimentalGlancePreviewApi
@androidx.glance.preview.Preview(widthDp = 360, heightDp = 120)
@Composable
private fun OverviewWidgetPreview() {
    GlanceTheme {
        WidgetContent(
            state = WidgetState(
                isLoading = false,
                serverName = "Preview Server",
                totalCount = 10,
                onlineCount = 8,
                offlineCount = 2,
                netSpeedUp = 1024L * 1024L * 5L, // 5 MB/s
                netSpeedDown = 1024L * 1024L * 15L, // 15 MB/s
                totalTrafficUp = 1024L * 1024L * 1024L * 50L, // 50 GB
                totalTrafficDown = 1024L * 1024L * 1024L * 150L, // 150 GB
                lastUpdated = System.currentTimeMillis()
            )
        )
    }
}
