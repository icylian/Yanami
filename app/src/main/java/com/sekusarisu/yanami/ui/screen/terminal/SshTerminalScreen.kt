package com.sekusarisu.yanami.ui.screen.terminal

import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.sekusarisu.yanami.R
import com.sekusarisu.yanami.ui.screen.soundClick
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import org.koin.core.parameter.parametersOf

/**
 * SSH 终端页面
 *
 * 布局：
 * - TopAppBar：节点名 + 连接状态副标题 + 返回按钮
 * - TerminalView（仅渲染）+ TerminalInputCapture（透明覆盖，拦截所有键盘输入）
 * - 底部特殊按键工具栏（Ctrl+C / Tab / Esc / 方向键等）
 */
class SshTerminalScreen(private val uuid: String, private val nodeName: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SshTerminalViewModel> { parametersOf(uuid) }
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current

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
                            },
                            colors =
                                    TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                    )
                    )
                }
        ) { innerPadding ->
            Column(
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(innerPadding)
                                    .imePadding()
                                    .background(Color.Black)
            ) {
                // ─── 终端区域 ───
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    TerminalContent(viewModel = viewModel, fontSize = state.fontSize)

                    // 连接中遮罩
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

                    // 错误遮罩
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

                // ─── 特殊按键工具栏 ───
                SpecialKeysToolbar(
                        ctrlActive = state.ctrlActive,
                        altActive = state.altActive,
                        fnMode = state.fnMode,
                        onKey = { bytes -> viewModel.sendInput(bytes) },
                        onToggleCtrl = { viewModel.onEvent(SshTerminalContract.Event.ToggleCtrl) },
                        onToggleAlt = { viewModel.onEvent(SshTerminalContract.Event.ToggleAlt) },
                        onToggleFn = { viewModel.onEvent(SshTerminalContract.Event.ToggleFn) },
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer)
                )
            }
        }

        // ─── 确认对话框 ───

        if (showDisconnectDialog) {
            AlertDialog(
                    onDismissRequest = { showDisconnectDialog = false },
                    title = { Text(stringResource(R.string.ssh_disconnect_title)) },
                    text = { Text(stringResource(R.string.ssh_disconnect_confirm)) },
                    confirmButton = {
                        TextButton(onClick = soundClick {
                            showDisconnectDialog = false
                            // Ctrl+D (EOT) 断开 SSH 连接
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
                            // 发送 reboot 命令重启服务器
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
    }
}

// ─── 终端渲染 + 输入捕获 ───

@Composable
private fun TerminalContent(viewModel: SshTerminalViewModel, fontSize: Int) {
    val terminalViewRef = remember { mutableStateOf<TerminalView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clientBridge.onTextChanged = {}
            terminalViewRef.value = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // TerminalView：仅负责渲染，不处理输入
        AndroidView(
                factory = { ctx ->
                    TerminalView(ctx, null).apply {
                        setTextSize(fontSize)
                        attachSession(viewModel.terminalBridge.session)
                        setTerminalViewClient(buildDisplayOnlyClient())
                        viewModel.clientBridge.onTextChanged = { post { invalidate() } }
                        terminalViewRef.value = this
                        // 通过布局监听读取 TerminalEmulator 实际列/行数，精确同步给服务端
                        addOnLayoutChangeListener { _, l, t, r, b, _, _, _, _ ->
                            if (r - l > 0 && b - t > 0) {
                                post { readAndSendEmulatorSize(viewModel) }
                            }
                        }
                    }
                },
                update = { view ->
                    view.setTextSize(fontSize)
                    // 字号变化后 TerminalView 会重新 layout，layout 监听器会再次同步尺寸
                },
                modifier = Modifier.fillMaxSize()
        )

        // TerminalInputCapture：透明覆盖层，拦截所有键盘输入路由到 WebSocket
        AndroidView(
                factory = { ctx ->
                    TerminalInputCapture(ctx).apply {
                        onInput = viewModel::sendInput
                        onFontSizeChange = { delta ->
                            viewModel.onEvent(SshTerminalContract.Event.FontSizeChanged(delta))
                        }
                        requestFocus()
                    }
                },
                modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * 通过反射读取 TerminalEmulator 实际渲染使用的列/行数，确保发送给服务端的尺寸与渲染完全一致。
 */
private fun readAndSendEmulatorSize(viewModel: SshTerminalViewModel) {
    val emulator = viewModel.terminalBridge.session.getEmulator() ?: return
    val cls = emulator.javaClass
    val cols = try {
        cls.getDeclaredField("mColumns").also { it.isAccessible = true }.getInt(emulator)
    } catch (_: Exception) { return }
    val rows = try {
        cls.getDeclaredField("mRows").also { it.isAccessible = true }.getInt(emulator)
    } catch (_: Exception) { return }
    if (cols > 0 && rows > 0) viewModel.sendResize(cols, rows)
}

/**
 * 构建一个纯展示用的 [TerminalViewClient]（不处理任何输入事件）。
 * 所有用户输入由 [TerminalInputCapture] 覆盖层负责拦截，TerminalView 仅做渲染。
 */
private fun buildDisplayOnlyClient(): TerminalViewClient =
        object : TerminalViewClient {

            override fun onScale(scale: Float): Float = scale

            override fun onSingleTapUp(e: android.view.MotionEvent) {}

            override fun shouldBackButtonBeMappedToEscape(): Boolean = false

            override fun shouldEnforceCharBasedInput(): Boolean = false

            override fun shouldUseCtrlSpaceWorkaround(): Boolean = false

            override fun isTerminalViewSelected(): Boolean = true

            override fun copyModeChanged(copyMode: Boolean) {}

            override fun onKeyDown(
                    keyCode: Int,
                    e: KeyEvent,
                    session: TerminalSession
            ): Boolean = false

            override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false

            override fun onLongPress(e: android.view.MotionEvent): Boolean = false

            override fun readControlKey(): Boolean = false

            override fun readAltKey(): Boolean = false

            override fun readFnKey(): Boolean = false

            override fun readShiftKey(): Boolean = false

            override fun onCodePoint(
                    codePoint: Int,
                    ctrlDown: Boolean,
                    session: TerminalSession
            ): Boolean = false

            override fun onEmulatorSet() {}

            override fun logError(tag: String?, message: String?) {}

            override fun logWarn(tag: String?, message: String?) {}

            override fun logInfo(tag: String?, message: String?) {}

            override fun logDebug(tag: String?, message: String?) {}

            override fun logVerbose(tag: String?, message: String?) {}

            override fun logStackTraceWithMessage(
                    tag: String?,
                    message: String?,
                    e: Exception?
            ) {}

            override fun logStackTrace(tag: String?, e: Exception?) {}
        }

// ─── 特殊按键工具栏 ───

private data class ToolbarKey(val label: String, val bytes: ByteArray)

/** 普通模式第一行：ESC / | - HOME ↑ END PGUP */
private val ROW1_KEYS = listOf(
        ToolbarKey("ESC", byteArrayOf(27)),
        ToolbarKey("/", byteArrayOf('/'.code.toByte())),
        ToolbarKey("|", byteArrayOf('|'.code.toByte())),
        ToolbarKey("-", byteArrayOf('-'.code.toByte())),
        ToolbarKey("HOME", byteArrayOf(27, 91, 72)),
        ToolbarKey("↑", byteArrayOf(27, 91, 65)),
        ToolbarKey("END", byteArrayOf(27, 91, 70)),
        ToolbarKey("PGUP", byteArrayOf(27, 91, 53, 126)),
)

/** 普通模式第二行箭头区：← ↓ → PGDN */
private val ROW2_ARROW_KEYS = listOf(
        ToolbarKey("←", byteArrayOf(27, 91, 68)),
        ToolbarKey("↓", byteArrayOf(27, 91, 66)),
        ToolbarKey("→", byteArrayOf(27, 91, 67)),
        ToolbarKey("PGDN", byteArrayOf(27, 91, 54, 126)),
)

/** FN 模式第一行：F1–F7 */
private val FN_ROW1_KEYS = listOf(
        ToolbarKey("F1", byteArrayOf(27, 79, 80)),
        ToolbarKey("F2", byteArrayOf(27, 79, 81)),
        ToolbarKey("F3", byteArrayOf(27, 79, 82)),
        ToolbarKey("F4", byteArrayOf(27, 79, 83)),
        ToolbarKey("F5", byteArrayOf(27, 91, 49, 53, 126)),
        ToolbarKey("F6", byteArrayOf(27, 91, 49, 55, 126)),
)

/** FN 模式第二行：F8–F12 */
private val FN_ROW2_KEYS = listOf(
        ToolbarKey("F7", byteArrayOf(27, 91, 49, 56, 126)),
        ToolbarKey("F8", byteArrayOf(27, 91, 49, 57, 126)),
        ToolbarKey("F9", byteArrayOf(27, 91, 50, 48, 126)),
        ToolbarKey("F10", byteArrayOf(27, 91, 50, 49, 126)),
        ToolbarKey("F11", byteArrayOf(27, 91, 50, 51, 126)),
        ToolbarKey("F12", byteArrayOf(27, 91, 50, 52, 126)),
)

private val BTN_PADDING = PaddingValues(horizontal = 1.dp)
private val BTN_HEIGHT = 36.dp

@Composable
private fun SpecialKeysToolbar(
        ctrlActive: Boolean,
        altActive: Boolean,
        fnMode: Boolean,
        onKey: (ByteArray) -> Unit,
        onToggleCtrl: () -> Unit,
        onToggleAlt: () -> Unit,
        onToggleFn: () -> Unit,
        modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeColor = MaterialTheme.colorScheme.primary
    val normalColor = MaterialTheme.colorScheme.onPrimaryContainer

    @Composable
    fun KeyBtn(key: ToolbarKey, modifier: Modifier) {
        TextButton(
                onClick = soundClick { onKey(key.bytes) },
                modifier = modifier.height(BTN_HEIGHT),
                contentPadding = BTN_PADDING
        ) {
            Text(
                    key.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = normalColor,
                    maxLines = 1
            )
        }
    }

    @Composable
    fun ToggleBtn(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
        TextButton(
                onClick = soundClick { onClick() },
                modifier = modifier.height(BTN_HEIGHT),
                contentPadding = BTN_PADDING,
                border = if (active) BorderStroke(1.dp,  activeColor) else null
        ) {
            Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) activeColor else normalColor,
                    maxLines = 1
            )
        }
    }

    @Composable
    fun KbdBtn(modifier: Modifier) {
        IconButton(
                onClick = soundClick {
                    val imm = context.getSystemService(InputMethodManager::class.java)
                    @Suppress("DEPRECATION")
                    imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0)
                },
                modifier = modifier.height(BTN_HEIGHT)
        ) {
            Icon(
                    imageVector = Icons.Filled.Keyboard,
                    contentDescription = null,
                    tint = normalColor,
                    modifier = Modifier.size(18.dp)
            )
        }
    }

    Column(modifier = modifier.padding(horizontal = 2.dp, vertical = 2.dp)) {
        if (fnMode) {
            // FN 模式第一行：F1–F6 + FN 切换
            Row(Modifier.fillMaxWidth()) {
                FN_ROW1_KEYS.forEach { KeyBtn(it, Modifier.weight(1f)) }
                ToggleBtn("FN", active = true, onClick = onToggleFn, modifier = Modifier.weight(1f))
            }
            // FN 模式第二行：F7–F12 + 键盘图标
            Row(Modifier.fillMaxWidth()) {
                FN_ROW2_KEYS.forEach { KeyBtn(it, Modifier.weight(1f)) }
                KbdBtn(Modifier.weight(1f))
            }
        } else {
            // 普通模式第一行：ESC / | - HOME ↑ END PGUP FN
            Row(Modifier.fillMaxWidth()) {
                // 对齐方向键
                ROW1_KEYS.take(5).forEach { KeyBtn(it, Modifier.weight(0.8f)) }
                ROW1_KEYS.drop(5).forEach { KeyBtn(it, Modifier.weight(1f)) }
                ToggleBtn("FN", active = false, onClick = onToggleFn, modifier = Modifier.weight(1f))
            }
            // 普通模式第二行：TAB CTRL ALT ← ↓ → PGDN ⌨
            Row(Modifier.fillMaxWidth()) {
                KeyBtn(ToolbarKey("TAB", byteArrayOf(9)), Modifier.weight(1f))
                ToggleBtn("CTRL", ctrlActive, onToggleCtrl, Modifier.weight(1f))
                ToggleBtn("ALT", altActive, onToggleAlt, Modifier.weight(1f))
                ROW2_ARROW_KEYS.forEach { KeyBtn(it, Modifier.weight(1f)) }
                KbdBtn(Modifier.weight(1f))
            }
        }
    }
}
