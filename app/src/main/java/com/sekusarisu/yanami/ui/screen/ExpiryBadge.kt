package com.sekusarisu.yanami.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.NodeExpiryStatus

@Composable
fun ExpiryBadge(expiryStatus: NodeExpiryStatus, modifier: Modifier = Modifier) {
        val (bgColor, textColor) =
                when {
                        expiryStatus.isExpired ->
                                MaterialTheme.colorScheme.errorContainer to
                                        MaterialTheme.colorScheme.onErrorContainer
                        expiryStatus.remainingSeconds <= 3 * 86_400 ->
                                MaterialTheme.colorScheme.errorContainer to
                                        MaterialTheme.colorScheme.onErrorContainer
                        expiryStatus.remainingSeconds <= 14 * 86_400 ->
                                MaterialTheme.colorScheme.tertiaryContainer to
                                        MaterialTheme.colorScheme.onTertiaryContainer
                        else ->
                                MaterialTheme.colorScheme.secondaryContainer to
                                        MaterialTheme.colorScheme.onSecondaryContainer
                }

        Surface(shape = RoundedCornerShape(12.dp), color = bgColor, modifier = modifier) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp)
                ) {
                        Icon(
                                Icons.Default.Timelapse,
                                contentDescription =
                                        stringResource(R.string.node_expiry_badge_remaining),
                                modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                                text = formatExpiryBadgeText(expiryStatus),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                        )
                }
        }
}

@Composable
private fun formatExpiryBadgeText(expiryStatus: NodeExpiryStatus): String {
        if (expiryStatus.isExpired) {
                return stringResource(R.string.node_expiry_badge_expired)
        }
        if (expiryStatus.remainingSeconds > 3L * 365 * 86_400) {
                return stringResource(R.string.node_expiry_badge_long_term)
        }
        return stringResource(
                R.string.node_expiry_badge_remaining,
                formatRemainingDurationShort(expiryStatus.remainingSeconds)
        )
}

private fun formatRemainingDurationShort(remainingSeconds: Long): String {
        val days = remainingSeconds / 86_400
        val hours = (remainingSeconds % 86_400) / 3_600
        val minutes = (remainingSeconds % 3_600) / 60

        return when {
                days > 0 -> "${days}d"
                hours > 0 -> "${hours}h"
                minutes > 0 -> "${minutes}m"
                else -> "<1m"
        }
}
