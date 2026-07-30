# Cyrene Mobile

Cyrene 桌面端的 Android 安全控制器。App 负责安全配对、对话、任务、桌面
Agent 设置与项目内远程终端；模型、Agent、工具、文件与命令执行始终发生在
桌面 Cyrene。

## 构建

要求 JDK 17 与 Android SDK 35：

```bash
./gradlew test lint assembleDebug
```

调试 APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

## 使用

1. 在桌面 Cyrene 打开「设置 → 连接」，开启远程访问。
2. 选择授权能力和 Project Scope，生成十位短密钥。
3. Android 输入桌面 LAN 或 Tailscale 数字 IP、端口和短密钥。
4. 核对两端 Fingerprint，确认配对。

客户端通过配对后的加密连接调用桌面 RemoteGateway。App 退出不会停止桌面端
Run；重新进入终端时会在当前授权项目内建立新的交互式 Shell 会话。

## 安全边界

- 只接受 RFC1918、IPv6 本地地址、Loopback 和 Tailscale `100.64.0.0/10`。
- 不接受 URL、域名、Redirect、公网 IP、任意 Route 或任意 Tool。
- Shell 仅能在当前授权项目目录中打开，并按配对设备隔离会话。
- Identity 私钥由 Android Keystore AES-256-GCM Master Key 加密。
- App 数据不参与 Auto Backup 或设备迁移。
- 所有控制命令使用 Ed25519 签名与 X25519/HKDF/ChaCha20-Poly1305 E2EE。
