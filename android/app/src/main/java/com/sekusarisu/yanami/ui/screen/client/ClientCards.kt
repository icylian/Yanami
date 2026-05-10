package com.sekusarisu.yanami.ui.screen.client

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.InstallDesktop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.ManagedClient
import com.sekusarisu.yanami.domain.model.calculateExpiryStatus
import com.sekusarisu.yanami.ui.screen.ExpiryBadge

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ClientCard(
        client: ManagedClient,
        maskIpAddress: Boolean,
        canMoveUp: Boolean,
        canMoveDown: Boolean,
        onShowInstallCommand: () -> Unit,
        onOpenTerminal: () -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        onMoveUp: () -> Unit,
        onMoveDown: () -> Unit,
        modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    OutlinedCard(
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
            modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            val expiryStatus = client.calculateExpiryStatus()
            val secondaryInfo =
                    listOf(client.group, client.os, client.version)
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
            ) {
                Text(
                        text = client.region + " " + client.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    if (expiryStatus != null) {
                        ExpiryBadge(expiryStatus = expiryStatus)
                    }
                    if (client.hidden) {
                        HiddenBadge()
                    }
                }
            }

            if (secondaryInfo.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = secondaryInfo, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState(), true),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (client.ipv4.isNotBlank()) {
                    AssistChip(
                            colors =
                                    AssistChipDefaults.assistChipColors(
                                            MaterialTheme.colorScheme.primaryContainer
                                    ),
                            onClick = {
                                clipboard.setText(AnnotatedString(client.ipv4))
                                Toast.makeText(
                                                context,
                                                context.getString(R.string.client_management_ip_copied),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            },
                            label = {
                                Text(
                                        stringResource(
                                                R.string.client_management_ipv4,
                                                client.ipv4.maskedIp(maskIpAddress)
                                        )
                                )
                            }
                    )
                }
                if (client.ipv6.isNotBlank()) {
                    AssistChip(
                            colors =
                                    AssistChipDefaults.assistChipColors(
                                            MaterialTheme.colorScheme.primaryContainer
                                    ),
                            onClick = {
                                clipboard.setText(AnnotatedString(client.ipv6))
                                Toast.makeText(
                                                context,
                                                context.getString(R.string.client_management_ip_copied),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            },
                            label = {
                                Text(
                                        stringResource(
                                                R.string.client_management_ipv6,
                                                client.ipv6.maskedIp(maskIpAddress)
                                        )
                                )
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text =
                            if (client.price < 0) {
                                stringResource(
                                        R.string.client_management_billing_free,
                                        client.billingCycle,
                                        if (client.autoRenewal) {
                                            stringResource(R.string.client_management_auto_renewal_on)
                                        } else {
                                            stringResource(
                                                    R.string.client_management_auto_renewal_off
                                            )
                                        }
                                )
                            } else {
                                stringResource(
                                        R.string.client_management_billing_summary,
                                        client.currency,
                                        client.price.billingText(),
                                        client.billingCycle,
                                        if (client.autoRenewal) {
                                            stringResource(R.string.client_management_auto_renewal_on)
                                        } else {
                                            stringResource(
                                                    R.string.client_management_auto_renewal_off
                                            )
                                        }
                                )
                            },
                    style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                    modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = onShowInstallCommand) {
                    Icon(Icons.Default.InstallDesktop, contentDescription = null)
                }
                OutlinedButton(onClick = onOpenTerminal) {
                    Icon(Icons.Default.Terminal, contentDescription = null)
                }
                OutlinedButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
                OutlinedButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        }
    }
}

@Composable
internal fun SortModeClientCard(
        client: ManagedClient,
        isDragging: Boolean,
        modifier: Modifier = Modifier
) {
    val secondaryInfo =
            listOf(client.group, client.os, client.version)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")

    OutlinedCard(
            modifier =
                    modifier
                            .fillMaxWidth()
                            .then(
                                    if (isDragging) {
                                        Modifier.background(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(12.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                            )
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = client.region + " " + client.name, style = MaterialTheme.typography.titleMedium)
                if (secondaryInfo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                            text = secondaryInfo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

internal fun List<ManagedClient>.moveItem(fromIndex: Int, toIndex: Int): List<ManagedClient> {
    if (fromIndex == toIndex || fromIndex !in indices || toIndex !in indices) return this
    return toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
}

@Composable
private fun HiddenBadge(modifier: Modifier = Modifier) {
    Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = modifier
    ) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                    Icons.Default.VisibilityOff,
                    contentDescription = stringResource(R.string.node_expiry_badge_remaining),
                    modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                    text = stringResource(R.string.client_management_hidden_badge),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun String.maskedIp(mask: Boolean): String {
    if (!mask || isBlank()) return this
    return if (contains('.')) {
        val parts = split('.')
        if (parts.size == 4) "${parts[0]}.${parts[1]}.*.*" else this
    } else if (contains(':')) {
        maskIpv6Address()
    } else {
        this
    }
}

private fun String.maskIpv6Address(): String {
    val normalized = substringBefore('%')
    val ipv4Tail = normalized.substringAfterLast(':', "")
    if (ipv4Tail.contains('.')) return this

    val expanded = normalized.expandIpv6Segments() ?: return this
    return expanded.take(2).joinToString(":") { it.trimLeadingIpv6Zeros() } + "::*"
}

private fun String.expandIpv6Segments(): List<String>? {
    val value = trim().lowercase()
    if (value.isEmpty()) return null
    if (value.split("::").size > 2) return null

    val segments =
            if (value.contains("::")) {
                val halves = value.split("::", limit = 2)
                val left = halves[0].split(':').filter { it.isNotEmpty() }
                val right = halves[1].split(':').filter { it.isNotEmpty() }
                val missing = 8 - (left.size + right.size)
                if (missing < 1) return null
                left + List(missing) { "0" } + right
            } else {
                value.split(':')
            }

    if (segments.size != 8) return null
    if (segments.any { it.length > 4 || it.any { ch -> !ch.isDigit() && ch !in 'a'..'f' } }) {
        return null
    }

    return segments.map { it.padStart(4, '0') }
}

private fun String.trimLeadingIpv6Zeros(): String {
    val trimmed = trimStart('0')
    return if (trimmed.isEmpty()) "0" else trimmed
}

private fun Double.billingText(): String {
    val asLong = toLong()
    return if (asLong.toDouble() == this) asLong.toString() else toString()
}
