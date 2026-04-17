package com.sekusarisu.yanami.ui.screen.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.TerminalSnippet
import com.sekusarisu.yanami.ui.theme.ThemeColor
import com.sekusarisu.yanami.ui.theme.YanamiTheme

private val previewSnippets =
        listOf(
                TerminalSnippet(
                        id = "check-disk",
                        title = "检查磁盘占用",
                        content = "df -h",
                        appendEnter = true
                ),
                TerminalSnippet(
                        id = "restart-nginx",
                        title = "重启 Nginx",
                        content = "systemctl restart nginx",
                        appendEnter = true
                ),
                TerminalSnippet(
                        id = "tail-log",
                        title = "查看应用日志",
                        content = "tail -n 200 /var/log/yanami/app.log"
                )
        )

@Preview(name = "SSH Terminal Screen", showBackground = true, widthDp = 412, heightDp = 917)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SshTerminalScreenPreview() {
    YanamiTheme(themeColor = ThemeColor.TEAL, darkTheme = true) {
        Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    TopAppBar(
                            title = {
                                Column {
                                    Text(
                                            text = "Tokyo Edge 01",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                            text = "已连接",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = {}) {
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.action_back)
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = {}, enabled = true) {
                                    Icon(
                                            imageVector = Icons.Filled.LinkOff,
                                            contentDescription =
                                                    stringResource(R.string.ssh_disconnect_title)
                                    )
                                }
                                IconButton(onClick = {}, enabled = true) {
                                    Icon(
                                            imageVector = Icons.Filled.RestartAlt,
                                            contentDescription = stringResource(R.string.ssh_reboot_title)
                                    )
                                }
                                IconButton(onClick = {}) {
                                    Icon(
                                            imageVector = Icons.Filled.Code,
                                            contentDescription =
                                                    stringResource(R.string.terminal_snippets_title)
                                    )
                                }
                            },
                            colors =
                                    TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                    )
                    )
                }
        ) { innerPadding ->
            Box(
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(innerPadding)
                                    .background(Color.Black)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            androidx.compose.foundation.layout.Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Notes,
                                        contentDescription = null,
                                        tint = Color(0xFF54C27A)
                                )
                                Text(
                                        text = "ssh root@tokyo-edge-01",
                                        color = Color(0xFF54C27A),
                                        style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                    text = "Last login: 2026-03-21 09:42:13 from 192.168.10.5",
                                    color = Color(0xFFBDBDBD),
                                    style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                    text = "root@tokyo-edge-01:~# uname -a",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                    text =
                                            "Linux tokyo-edge-01 6.6.12-amd64 #1 SMP PREEMPT_DYNAMIC x86_64 GNU/Linux",
                                    color = Color(0xFFBDBDBD),
                                    style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                    text = "root@tokyo-edge-01:~# systemctl status nginx --no-pager",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                    text = "active (running) since Fri 2026-03-20 18:04:08 JST; 15h ago",
                                    color = Color(0xFF54C27A),
                                    style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                    text = "root@tokyo-edge-01:~#",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)

                    SpecialKeysToolbar(
                            ctrlActive = true,
                            altActive = false,
                            fnMode = false,
                            onKey = {},
                            onToggleCtrl = {},
                            onToggleAlt = {},
                            onToggleFn = {},
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                }

                SnippetsSidebar(
                        visible = true,
                        snippets = previewSnippets,
                        isConnected = true,
                        onDismiss = {},
                        onStartCreate = {},
                        onStartEdit = {},
                        onDeleteRequest = {},
                        onSend = {}
                )
            }
        }
    }
}

@Preview(name = "Snippet Editor", showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun SshTerminalSnippetEditorPreview() {
    YanamiTheme(themeColor = ThemeColor.TEAL, darkTheme = true) {
        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
        ) {
            SnippetEditorDialog(
                    editingSnippetId = "tail-log",
                    snippetTitle = "查看应用日志",
                    snippetContent = "tail -n 200 /var/log/yanami/app.log",
                    snippetAppendEnter = false,
                    onDismiss = {},
                    onTitleChange = {},
                    onContentChange = {},
                    onAppendEnterChange = {},
                    onSave = {}
            )
        }
    }
}
