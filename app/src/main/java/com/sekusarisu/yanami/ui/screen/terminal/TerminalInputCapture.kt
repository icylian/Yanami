package com.sekusarisu.yanami.ui.screen.terminal

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager

/**
 * 透明终端输入捕获层
 *
 * 覆盖在 [com.termux.view.TerminalView] 上方，拦截所有来自软键盘和硬件键盘的输入，
 * 通过 [onInput] 回调将字节序列路由到 WebSocket，而不写入任何本地进程。
 *
 * 技术原理：
 * - 重写 [onCreateInputConnection] 提供自定义 [InputConnection]，拦截软键盘文字输入和退格
 * - 重写 [onKeyDown] 拦截硬件键盘特殊键（方向键、Enter、Tab 等）
 * - 将所有输入转换为对应的 ANSI/VT100 字节序列
 */
class TerminalInputCapture
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    /** 字节输入回调：所有来自键盘的输入最终都通过此回调路由到 WebSocket */
    var onInput: (ByteArray) -> Unit = {}

    /** 音量键字号调整回调：delta = +1 增大 / -1 减小 */
    var onFontSizeChange: (delta: Int) -> Unit = {}

    /** 查询 TerminalEmulator 当前是否处于 Application Cursor Keys 模式 (DECCKM) */
    var isCursorAppMode: () -> Boolean = { false }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onCheckIsTextEditor(): Boolean = true

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        // TYPE_NULL：禁止自动补全、预测文字、标点替换，保证原始字符输入
        outAttrs.inputType = InputType.TYPE_NULL
        return TerminalInputConnection(this)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            requestFocus()
            val imm = context.getSystemService(InputMethodManager::class.java)
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // 音量键：调整字号，消费事件防止系统调节音量
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> { onFontSizeChange(+1); return true }
            KeyEvent.KEYCODE_VOLUME_DOWN -> { onFontSizeChange(-1); return true }
        }
        val bytes = keyCodeToBytes(keyCode, event, isCursorAppMode()) ?: return super.onKeyDown(keyCode, event)
        onInput(bytes)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        // 仅消费 onKeyDown 也会消费的按键，保持一致性
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> true
            else -> keyCodeToBytes(keyCode, event, isCursorAppMode()) != null
        }
    }

    // ─── 内部 InputConnection 实现 ───

    private inner class TerminalInputConnection(view: View) : BaseInputConnection(view, false) {

        /** 软键盘提交文本（包括正常字符和中文输入法上屏） */
        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
            text?.toString()?.toByteArray(Charsets.UTF_8)?.let { onInput(it) }
            return true
        }

        /** 软键盘退格：发送 DEL (0x7F) */
        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            if (beforeLength > 0) {
                onInput(ByteArray(beforeLength) { 127.toByte() })
            }
            return true
        }

        /** 将软键盘触发的 KeyEvent 转交给 View.onKeyDown 统一处理 */
        override fun sendKeyEvent(event: KeyEvent): Boolean {
            if (event.action == KeyEvent.ACTION_DOWN) {
                return this@TerminalInputCapture.onKeyDown(event.keyCode, event)
            }
            return super.sendKeyEvent(event)
        }
    }

    companion object {

        /**
         * 将 Android KeyCode 转换为对应的 ANSI/VT100 终端字节序列。
         * 返回 null 表示此 keyCode 不需要特殊处理（由 InputConnection 或父类处理）。
         *
         * @param cursorApp 当 TerminalEmulator 处于 Application Cursor Keys 模式 (DECCKM) 时为 true，
         *                  方向键将发送 ESC O A/B/C/D 而非 ESC [ A/B/C/D。
         */
        fun keyCodeToBytes(keyCode: Int, event: KeyEvent, cursorApp: Boolean = false): ByteArray? {
            return when (keyCode) {
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER -> byteArrayOf(13) // CR
                KeyEvent.KEYCODE_DEL -> byteArrayOf(127) // DEL (backspace)
                KeyEvent.KEYCODE_FORWARD_DEL -> byteArrayOf(27, 91, 51, 126) // ESC [ 3 ~
                KeyEvent.KEYCODE_ESCAPE -> byteArrayOf(27)
                KeyEvent.KEYCODE_TAB -> byteArrayOf(9) // HT
                // 方向键：Application Cursor Keys 模式下 ESC O x，Normal 模式下 ESC [ x
                KeyEvent.KEYCODE_DPAD_UP    -> if (cursorApp) byteArrayOf(27, 79, 65) else byteArrayOf(27, 91, 65)
                KeyEvent.KEYCODE_DPAD_DOWN  -> if (cursorApp) byteArrayOf(27, 79, 66) else byteArrayOf(27, 91, 66)
                KeyEvent.KEYCODE_DPAD_RIGHT -> if (cursorApp) byteArrayOf(27, 79, 67) else byteArrayOf(27, 91, 67)
                KeyEvent.KEYCODE_DPAD_LEFT  -> if (cursorApp) byteArrayOf(27, 79, 68) else byteArrayOf(27, 91, 68)
                KeyEvent.KEYCODE_MOVE_HOME -> byteArrayOf(27, 91, 72) // ESC [ H
                KeyEvent.KEYCODE_MOVE_END -> byteArrayOf(27, 91, 70) // ESC [ F
                KeyEvent.KEYCODE_PAGE_UP -> byteArrayOf(27, 91, 53, 126) // ESC [ 5 ~
                KeyEvent.KEYCODE_PAGE_DOWN -> byteArrayOf(27, 91, 54, 126) // ESC [ 6 ~
                KeyEvent.KEYCODE_INSERT -> byteArrayOf(27, 91, 50, 126) // ESC [ 2 ~
                // F1–F12（蓝牙键盘通过 Fn 组合键发送）
                KeyEvent.KEYCODE_F1 -> byteArrayOf(27, 79, 80)           // ESC O P
                KeyEvent.KEYCODE_F2 -> byteArrayOf(27, 79, 81)           // ESC O Q
                KeyEvent.KEYCODE_F3 -> byteArrayOf(27, 79, 82)           // ESC O R
                KeyEvent.KEYCODE_F4 -> byteArrayOf(27, 79, 83)           // ESC O S
                KeyEvent.KEYCODE_F5 -> byteArrayOf(27, 91, 49, 53, 126) // ESC [ 1 5 ~
                KeyEvent.KEYCODE_F6 -> byteArrayOf(27, 91, 49, 55, 126) // ESC [ 1 7 ~
                KeyEvent.KEYCODE_F7 -> byteArrayOf(27, 91, 49, 56, 126) // ESC [ 1 8 ~
                KeyEvent.KEYCODE_F8 -> byteArrayOf(27, 91, 49, 57, 126) // ESC [ 1 9 ~
                KeyEvent.KEYCODE_F9 -> byteArrayOf(27, 91, 50, 48, 126) // ESC [ 2 0 ~
                KeyEvent.KEYCODE_F10 -> byteArrayOf(27, 91, 50, 49, 126) // ESC [ 2 1 ~
                KeyEvent.KEYCODE_F11 -> byteArrayOf(27, 91, 50, 51, 126) // ESC [ 2 3 ~
                KeyEvent.KEYCODE_F12 -> byteArrayOf(27, 91, 50, 52, 126) // ESC [ 2 4 ~
                else -> {
                    // 处理 Ctrl/Alt 组合键和可打印字符
                    var unicodeChar = event.getUnicodeChar(event.metaState)
                    if (unicodeChar == 0 && (event.isCtrlPressed || event.isAltPressed)) {
                        // 部分蓝牙键盘的 KeyCharacterMap 未定义修饰键映射，
                        // getUnicodeChar(metaState) 返回 0，回退到无修饰的基础字符
                        unicodeChar = event.getUnicodeChar(0)
                    }
                    if (unicodeChar > 0) {
                        val baseBytes = if (event.isCtrlPressed) {
                            byteArrayOf((unicodeChar and 0x1f).toByte())
                        } else {
                            String(Character.toChars(unicodeChar)).toByteArray(Charsets.UTF_8)
                        }
                        if (event.isAltPressed) byteArrayOf(27) + baseBytes else baseBytes
                    } else {
                        null
                    }
                }
            }
        }
    }
}
