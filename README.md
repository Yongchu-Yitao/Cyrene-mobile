# Cyrene Mobile

Cyrene 桌面端的 Android 安全控制器。App 负责安全配对、对话、任务、桌面
Agent 设置与项目内远程终端；模型、Agent、工具、文件与命令执行始终发生在
桌面 Cyrene。

## 0.1.1 功能

- 与桌面端一致的对话信息层级、Markdown、工具调用、附件和图片查看体验。
- 无阴影悬浮输入框，可切换自动/默认/Plan 权限模式、处理桌面端提权确认、
  上传多个附件并停止正在运行的回复；首次安装默认启用自动模式。
- 左侧菜单统一显示设备、对话、任务、终端与所有会话；全部授权项目的任务和对话
  标注所属项目后按时间混排。新对话可从中央胶囊切换项目，任务可在创建卡片内切换。
- 会话长按菜单支持重命名和删除；新对话标题由第一条消息自动生成。
- 向左滑动打开桌面式右侧栏，提供概览、上下文，以及按数据动态出现的子 Agent、
  变更、查看器、地图、计划、产物和分支页签。
- 任务详情复用对话输入框，支持附件派发、暂停、恢复、取消和产物下载。
- 支持多台桌面设备、安全切换、浅色/深色/跟随系统主题和中英文界面。
- 设置页可从 GitHub Release 检查、下载并安装后续 APK 更新。

## 构建

要求 JDK 17 与 Android SDK 35：

```bash
./gradlew test lint assembleDebug
```

调试 APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

正式发布使用 `v0.1.1` 标签，APK 及完整更新说明见仓库的 GitHub Releases。

## 使用

1. 在桌面 Cyrene 打开「设置 → 连接」，开启远程访问。
2. 选择授权能力和 Project Scope，生成十位短密钥。
3. Android 输入桌面 LAN 或 Tailscale 数字 IP、端口和短密钥。
4. 核对两端 Fingerprint，确认配对。

客户端通过配对后的加密连接调用桌面 RemoteGateway。App 退出不会停止桌面端
Run；重新进入终端时会在当前授权项目内建立新的交互式 Shell 会话。

> 完整使用 0.1.1 的移动端权限确认和 OpenAI OAuth 管理，需要同步更新支持对应
> 远程命令的 Cyrene Desktop。

## 安全边界

- 只接受 RFC1918、IPv6 本地地址、Loopback 和 Tailscale `100.64.0.0/10`。
- 不接受 URL、域名、Redirect、公网 IP、任意 Route 或任意 Tool。
- Shell 仅能在当前授权项目目录中打开，并按配对设备隔离会话。
- Identity 私钥由 Android Keystore AES-256-GCM Master Key 加密。
- App 数据不参与 Auto Backup 或设备迁移。
- 所有控制命令使用 Ed25519 签名与 X25519/HKDF/ChaCha20-Poly1305 E2EE。

## 更新记录

参见 [CHANGELOG.md](CHANGELOG.md)。
