package com.sekusarisu.yanami.mvi

/**
 * MVI 架构 — 单次副作用标记接口
 *
 * 用于表示一次性的副作用事件，如：
 * - 显示 Toast / Snackbar
 * - 导航跳转
 * - 弹出对话框
 *
 * Effect 通过 Channel 发送，确保只被消费一次。
 */
interface UiEffect
