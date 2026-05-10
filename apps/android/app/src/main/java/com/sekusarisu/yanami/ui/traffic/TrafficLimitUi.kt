package com.sekusarisu.yanami.ui.traffic

import com.sekusarisu.yanami.domain.model.TrafficLimitUsage
import com.sekusarisu.yanami.ui.screen.nodelist.formatBytes

fun formatTrafficLimitTypeLabel(type: String): String = type.uppercase()

fun formatTrafficLimitPercent(percent: Double): String = String.format("%.0f%%", percent)

fun formatTrafficLimitDetail(usage: TrafficLimitUsage): String {
        return "${formatBytes(usage.currentUsage)} / ${formatBytes(usage.limit)} (${formatTrafficLimitPercent(usage.usagePercent)})"
}
