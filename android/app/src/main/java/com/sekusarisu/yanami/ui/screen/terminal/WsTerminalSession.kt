package com.sekusarisu.yanami.ui.screen.terminal

import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient

/**
 * WebSocket 与 TerminalSession/TerminalEmulator 的桥接器
 *
 * 由于 [TerminalSession] 被声明为 final，无法继承，本类改用组合模式：
 * - 内部持有一个使用空壳进程的 [TerminalSession]，仅用于初始化 PTY/Emulator
 * - [feedOutput] 将从 WebSocket 收到的原始字节写入 TerminalEmulator
 * - 用户输入改由 [TerminalInputCapture] 透明覆盖层捕获，直接通过回调路由到 WebSocket
 */
class WsTerminalBridge(private val sessionClient: TerminalSessionClient) {

    /**
     * 内部 TerminalSession：
     * - 空壳 Shell 进程（重定向 stdin/stdout/stderr 到 /dev/null，随后无限 sleep）
     * - 不参与任何实际 I/O，仅为 TerminalEmulator 的初始化提供 PTY 环境
     * - transcriptRows=3000 表示回滚缓冲区行数
     */
    val session: TerminalSession =
            TerminalSession(
                    "/system/bin/sh",
                    "/",
//                    arrayOf("-c", "exec </dev/null >/dev/null 2>&1; while :; do sleep 86400; done"),
                    emptyArray<String>(),
                    emptyArray<String>(),
                    3000,
                    sessionClient
            )

    /**
     * 将从 WebSocket 收到的 Binary Frame 写入 TerminalEmulator，并通知 client 重绘。
     *
     * 应在主线程调用（TerminalEmulator 非线程安全）。
     */
    fun feedOutput(data: ByteArray) {
        val emulator = session.emulator ?: return
        emulator.append(data, data.size)
        sessionClient.onTextChanged(session)
    }
}
