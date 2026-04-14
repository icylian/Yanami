package com.sekusarisu.yanami.ui.screen.terminal

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.viewinterop.AndroidView
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.domain.model.TerminalSnippet
import com.sekusarisu.yanami.ui.screen.rememberAdaptiveLayoutInfo
import com.sekusarisu.yanami.ui.screen.soundClick
import com.sekusarisu.yanami.ui.theme.ThemeColor
import com.sekusarisu.yanami.ui.theme.YanamiTheme
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import org.koin.core.parameter.parametersOf

class SshTerminalScreen(private val uuid: String, private val nodeName: String) : Screen {

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SshTerminalViewModel> { parametersOf(uuid) }
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val snippetNameEmptyMessage = stringResource(R.string.terminal_snippet_name_empty)
        val snippetContentEmptyMessage = stringResource(R.string.terminal_snippet_content_empty)

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is SshTerminalContract.Effect.NavigateBack -> navigator.pop()
                    is SshTerminalContract.Effect.ShowToast ->
                            Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        var showDisconnectDialog by remember { mutableStateOf(false) }
        var showRebootDialog by remember { mutableStateOf(false) }
        var showSnippetEditorDialog by rememberSaveable { mutableStateOf(false) }
        var editingSnippetId by rememberSaveable { mutableStateOf<String?>(null) }
        var snippetTitle by rememberSaveable { mutableStateOf("") }
        var snippetContent by rememberSaveable { mutableStateOf("") }
        var snippetAppendEnter by rememberSaveable { mutableStateOf(false) }
        var snippetPendingDelete by remember { mutableStateOf<TerminalSnippet?>(null) }

        fun resetSnippetEditor(snippet: TerminalSnippet? = null) {
            editingSnippetId = snippet?.id
            snippetTitle = snippet?.title.orEmpty()
            snippetContent = snippet?.content.orEmpty()
            snippetAppendEnter = snippet?.appendEnter ?: false
        }

        Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    TopAppBar(
                            title = {
                                Column {
                                    Text(
                                            text = nodeName,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                            text =
                                                    when {
                                                        state.isConnecting -> "正在连接…"
                                                        state.isConnected -> "已连接"
                                                        state.error != null -> "连接失败"
                                                        else -> "SSH 终端"
                                                    },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = soundClick { navigator.pop() }) {
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.action_back)
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                        onClick = soundClick { showDisconnectDialog = true },
                                        enabled = state.isConnected
                                ) {
                                    Icon(
                                            imageVector = Icons.Filled.LinkOff,
                                            contentDescription = stringResource(R.string.ssh_disconnect_title)
                                    )
                                }
                                IconButton(
                                        onClick = soundClick { showRebootDialog = true },
                                        enabled = state.isConnected
                                ) {
                                    Icon(
                                            imageVector = Icons.Filled.RestartAlt,
                                            contentDescription = stringResource(R.string.ssh_reboot_title)
                                    )
                                }
                                IconButton(
                                        onClick = soundClick {
                                            viewModel.onEvent(SshTerminalContract.Event.ToggleSnippetsPanel)
                                        }
                                ) {
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
                                    .imePadding()
                                    .background(Color.Black)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        TerminalContent(viewModel = viewModel, fontSize = state.fontSize)

                        if (state.isConnecting) {
                            Box(
                                    modifier =
                                            Modifier.fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.8f)),
                                    contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color.White)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                            "正在连接 SSH…",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        if (!state.isConnecting && state.error != null) {
                            Box(
                                    modifier =
                                            Modifier.fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.85f)),
                                    contentAlignment = Alignment.Center
                            ) {
                                Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(24.dp)
                                ) {
                                    Text(
                                            "连接失败",
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                            state.error!!,
                                            color = Color.LightGray,
                                            style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TextButton(onClick = soundClick { navigator.pop() }) {
                                        Text("返回", color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)

                    SpecialKeysToolbar(
                            ctrlActive = state.ctrlActive,
                            altActive = state.altActive,
                            fnMode = state.fnMode,
                            onKey = { bytes -> viewModel.sendInput(bytes) },
                            onToggleCtrl = { viewModel.onEvent(SshTerminalContract.Event.ToggleCtrl) },
                            onToggleAlt = { viewModel.onEvent(SshTerminalContract.Event.ToggleAlt) },
                            onToggleFn = { viewModel.onEvent(SshTerminalContract.Event.ToggleFn) },
                            isCursorAppMode = {
                                viewModel.terminalBridge.session.emulator
                                    ?.isCursorKeysApplicationMode ?: false
                            },
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                }

                SnippetsSidebar(
                        visible = state.isSnippetsPanelOpen,
                        snippets = state.snippets,
                        isConnected = state.isConnected,
                        onDismiss = {
                            viewModel.onEvent(SshTerminalContract.Event.SetSnippetsPanelOpen(false))
                        },
                        onStartCreate = {
                            resetSnippetEditor()
                            showSnippetEditorDialog = true
                        },
                        onStartEdit = {
                            resetSnippetEditor(it)
                            showSnippetEditorDialog = true
                        },
                        onDeleteRequest = { snippetPendingDelete = it },
                        onSend = viewModel::sendSnippet
                )
            }
        }

        if (showSnippetEditorDialog) {
            SnippetEditorDialog(
                    editingSnippetId = editingSnippetId,
                    snippetTitle = snippetTitle,
                    snippetContent = snippetContent,
                    snippetAppendEnter = snippetAppendEnter,
                    onDismiss = { showSnippetEditorDialog = false },
                    onTitleChange = { snippetTitle = it },
                    onContentChange = { snippetContent = it },
                    onAppendEnterChange = { snippetAppendEnter = it },
                    onSave = {
                        val normalizedTitle = snippetTitle.trim()
                        val normalizedContent =
                                snippetContent.replace("\r\n", "\n").replace('\r', '\n')
                        if (normalizedTitle.isEmpty()) {
                            Toast.makeText(context, snippetNameEmptyMessage, Toast.LENGTH_SHORT).show()
                            return@SnippetEditorDialog
                        }
                        if (normalizedContent.isBlank()) {
                            Toast.makeText(context, snippetContentEmptyMessage, Toast.LENGTH_SHORT)
                                    .show()
                            return@SnippetEditorDialog
                        }
                        val saved =
                                viewModel.saveSnippet(
                                        snippetId = editingSnippetId,
                                        title = normalizedTitle,
                                        content = normalizedContent,
                                        appendEnter = snippetAppendEnter
                                )
                        if (saved) {
                            showSnippetEditorDialog = false
                            resetSnippetEditor()
                        }
                    }
            )
        }

        if (showDisconnectDialog) {
            AlertDialog(
                    onDismissRequest = { showDisconnectDialog = false },
                    title = { Text(stringResource(R.string.ssh_disconnect_title)) },
                    text = { Text(stringResource(R.string.ssh_disconnect_confirm)) },
                    confirmButton = {
                        TextButton(onClick = soundClick {
                            showDisconnectDialog = false
                            viewModel.sendInput(byteArrayOf(4))
                        }) {
                            Text(stringResource(R.string.action_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = soundClick { showDisconnectDialog = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
            )
        }

        if (showRebootDialog) {
            AlertDialog(
                    onDismissRequest = { showRebootDialog = false },
                    title = { Text(stringResource(R.string.ssh_reboot_title)) },
                    text = { Text(stringResource(R.string.ssh_reboot_confirm)) },
                    confirmButton = {
                        TextButton(onClick = soundClick {
                            showRebootDialog = false
                            viewModel.sendInput("reboot\n".toByteArray())
                        }) {
                            Text(stringResource(R.string.action_confirm))
                        }
                    },
                dismissButton = {
                    TextButton(onClick = soundClick { showRebootDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                        }
                }
            )
        }

        snippetPendingDelete?.let { snippet ->
            AlertDialog(
                    onDismissRequest = { snippetPendingDelete = null },
                    title = { Text(stringResource(R.string.terminal_snippet_delete_title)) },
                    text = {
                        Text(
                                stringResource(
                                        R.string.terminal_snippet_delete_confirm,
                                        snippet.title
                                )
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = soundClick {
                            viewModel.deleteSnippet(snippet.id)
                            if (editingSnippetId == snippet.id) resetSnippetEditor()
                            snippetPendingDelete = null
                        }) {
                            Text(stringResource(R.string.action_delete))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = soundClick { snippetPendingDelete = null }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
            )
        }
    }
}
