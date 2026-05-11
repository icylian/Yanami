package com.sekusarisu.yanami.mvi

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * MVI 架构 — 基础 ViewModel (基于 Voyager ScreenModel)
 *
 * 泛型参数：
 * - [S] : UI 状态 (UiState)，通过 StateFlow 暴露，驱动 UI 重组
 * - [E] : 用户事件 (UiEvent)，从 UI 层发送到 ViewModel
 * - [F] : 副作用 (UiEffect)，通过 Channel 发送一次性事件
 *
 * 使用示例：
 * ```
 * data class HomeState(...) : UiState
 * sealed interface HomeEvent : UiEvent { ... }
 * sealed interface HomeEffect : UiEffect { ... }
 *
 * class HomeViewModel : MviViewModel<HomeState, HomeEvent, HomeEffect>(HomeState()) {
 *     override fun onEvent(event: HomeEvent) { ... }
 * }
 * ```
 */
abstract class MviViewModel<S : UiState, E : UiEvent, F : UiEffect>(initialState: S) : ScreenModel {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    /** 当前 UI 状态的快捷访问 */
    protected val currentState: S
        get() = _state.value

    private val _effect = Channel<F>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    /** 处理来自 UI 层的事件（Intent）。 子类需重写此方法实现具体的业务逻辑。 */
    abstract fun onEvent(event: E)

    /**
     * 更新 UI 状态。 通过 reduce lambda 基于当前状态生成新状态。
     *
     * @param reduce 接收当前状态，返回新状态的 lambda
     */
    protected fun setState(reduce: S.() -> S) {
        _state.value = currentState.reduce()
    }

    /**
     * 发送一次性副作用事件。
     *
     * @param effect 要发送的副作用
     */
    protected fun sendEffect(effect: F) {
        screenModelScope.launch { _effect.send(effect) }
    }
}
