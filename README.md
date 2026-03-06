# Yanami

**Yanami** 是 [Komari](https://github.com/komari-monitor/komari) 服务器监控工具的 Android 客户端，采用 Material Design 3 设计语言构建。

> A Material Design 3 Android client for the Komari server monitoring tool.

---

## 功能特性

- **多实例管理** — 添加、编辑、切换多个 Komari 服务端实例
- **实时节点列表** — WebSocket 实时推送节点状态（CPU / RAM / 磁盘 / 网络 IO）
- **节点详情看板** — 负载历史折线图、Ping 延迟趋势、服务器基础信息
- **SSH 终端** — 基于 Terminal-view + WebSocket 的全功能 ANSI/VT100 终端，支持特殊按键工具栏与字号调整
- **多语言** — 中文（默认）、English、日本語
- **主题系统** — Material You 动态取色（Android 12+）+ 6 种预设配色，支持深色/浅色/跟随系统

## 截图

### 实例管理

<p style="text-align: center;">
    <img alt="desktop" src="assets/addserver.png" width="360"> <img alt="desktop" src="assets/serverlist.png" width="360">
</p>

### 日间/浅色模式

<p style="text-align: center;">
    <img alt="desktop" src="assets/nodelist.png" width="360"> <img alt="desktop" src="assets/nodedetail1.png" width="360">
</p>

### 夜间/深色模式

<p style="text-align: center;">
    <img alt="desktop" src="assets/nodelistdark.png" width="360"> <img alt="desktop" src="assets/nodedetaildark.png" width="360">
</p>

### 延迟监测/SSH终端

<p style="text-align: center;">
    <img alt="desktop" src="assets/nodedetail2.png" width="360"> <img alt="desktop" src="assets/nodeterminal.png" width="360">
</p>

## 系统要求

| 项目 | 要求 |
|---|---|
| Android | 9.0（API 28）及以上 |
| 服务端 | Komari |

## 构建

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease

# 清理后构建
./gradlew clean assembleDebug
```

构建产物位于 `app/build/outputs/apk/`。

## 技术栈

| 库 | 版本 | 用途 |
|---|---|---|
| Kotlin | 2.3.10 | 主语言 |
| Jetpack Compose BOM | 2026.02.00 | UI 框架 |
| MD3 | — | 设计系统 |
| Voyager | 1.1.0-beta03 | 导航 + ScreenModel |
| Koin | 4.1.1 | 依赖注入 |
| Ktor | 3.4.0 | HTTP 客户端 + WebSocket |
| Room | 2.8.4 | 本地数据库（加密凭据存储） |
| Vico | 2.2.0 | 图表（Compose M3） |
| termux terminal-view | 0.119.0-beta.3 | 终端 ANSI/VT100 渲染 |
| DataStore Preferences | 1.2.0 | 用户偏好持久化 |

## 架构

采用 **MVI（Model-View-Intent）** 模式，分三层：

```
UI Layer      Voyager Screen + Compose UI + MviViewModel<State, Event, Effect>
Domain Layer  Repository 接口 + 领域模型（Node, ServerInstance …）
Data Layer    Repository 实现、Ktor、Room、DataStore
```

每个页面遵循 **Contract 模式**，以嵌套的 `State` / `Event` / `Effect` 描述该页面的完整 MVI 契约。

### 导航流

```
ServerListScreen → AddServerScreen
                 → NodeListScreen → NodeDetailScreen → SshTerminalScreen
                 → SettingsScreen
```

### 认证与网络

- 通过 `POST /api/login` 获取 `session_token`（支持 2FA）
- Token 以 AES/GCM 加密后存入 Room，启动时自动恢复
- WebSocket (`wss://host/api/rpc2`) 需携带 `Cookie: session_token` 及 `Origin` 头
- `SessionCookieInterceptor`（OkHttp）自动注入 Cookie

## 许可证

本项目遵循 [MIT License](LICENSE)。
