> **PLANNED / 功能与实现边界已确定，尚未开始编码 — 2026-07-30：**
> 本文定义 Cyrene Android 移动端的完整产品范围、协议边界、源码结构、
> 实施顺序和验收基线。移动端是纯控制器；模型、Agent、Tool、Skill、
> Browser、文件和任务执行始终发生在桌面端 Cyrene。
>
> **连接范围：** 正式支持同一局域网直连和 Tailscale Tailnet 直连。
> 不建设公网 API、云 Relay、账号服务、NAT 穿透或移动端 Agent Runtime。
>
> **实现前置：** 桌面端现有 Signed + E2EE Envelope、Device Identity、
> Pairing、Grant、Project Scope、Idempotency 和固定 Remote Command
> 全部复用；为了满足“移动端只主动连接桌面端”，桌面端需要新增
> Controller-only Request/Response Transport。不得要求 Android 开放监听
> 端口作为正式产品依赖。

# Cyrene Mobile Controller 功能实现 Handoff

[桌面端 Cyrene-to-Cyrene 远程控制 Handoff](../../Cyrene/project-notes/remote-cyrene-control-design.zh-CN.md) ·
[桌面端架构说明](../../Cyrene/docs/architecture.zh-CN.md)

更新时间：2026-07-30

仓库：`Cyrene-mobile`

桌面端依赖仓库：`../Cyrene`

当前状态：移动端仓库为空；本文是首个正式实现基线。

## 1. 产品目标

Cyrene Mobile 是桌面端 Cyrene 的轻量控制器。用户在 Android 手机上通过
局域网或 Tailscale 连接一台已运行 Cyrene 的电脑，查看已授权项目、创建和
管理对话、向 Agent 下达任务、观察公开运行事件、补充指导、回答确认问题、
控制任务并下载结果文件。

移动端不运行：

- Python；
- LLM Provider；
- Agent Harness；
- Tool、Skill、MCP 或 Integration；
- Browser / Computer Use；
- Workspace 文件系统；
- 桌面端 Credential；
- Cyrene 主 Runtime 数据库。

正式数据流是：

```text
Android Compose UI
        │
        ▼
Mobile Repository / Command Client
        │ typed command
        ▼
Signed + E2EE Envelope
        │ HTTP over LAN or Tailscale
        ▼
Desktop Controller-only Endpoint
        │
        ▼
RemoteGateway
        │ identity → replay → grant → scope → schema → idempotency
        ▼
Desktop Workbench / Agent / Tooling
```

用户关闭 App、手机断网或 Android 杀死进程，不得终止桌面端已经启动的
Chat Run 或 Task。重新打开 App 后通过 `run_id + cursor` 恢复公开事件。

## 2. 产品边界

### 2.1 第一版必须实现

| 领域 | 功能 |
|---|---|
| 设备 | IP + 短密钥配对、指纹确认、设备列表、撤销本机信任 |
| 连接 | 局域网地址、Tailscale 地址、在线检测、手动切换、自动回退 |
| Project | 查看桌面端明确授权的项目摘要 |
| Chat | 列表、创建、详情、发送消息 |
| Run | 状态、增量事件、等待、指导、停止 |
| Approval | 回答 Chat / Task 的澄清和受控确认 |
| Task | 列表、创建、详情、派发、计划批准、执行步骤、暂停/恢复/取消 |
| 文件 | Chat Attachment 和 Task Artifact 分块下载 |
| 本地体验 | 最近状态缓存、错误恢复、下载进度、中文/英文、深色模式 |
| 安全 | Device Identity、E2EE、Grant、Scope、Replay、Idempotency |

### 2.2 第二阶段可实现

- Android 平板和折叠屏双栏布局；
- 用户显式开启的后台 Run 监控；
- 完成、等待回答和失败通知；
- QR Code 承载桌面地址、短密钥和 Fingerprint；
- Tailscale MagicDNS 名称；
- 多台桌面端快速切换；
- 生物识别保护 App 内已配对设备；
- 下载文件的系统分享和 Android Photo Picker / DocumentsProvider 集成。

### 2.3 明确不做

第一版和本协议不得：

- 在移动端运行 Cyrene Agent；
- 将桌面端 FastAPI `/api/*` 或 `/v1/control/*` 直接暴露到 LAN/Tailnet；
- 使用桌面本机 `X-Cyrene-Token` 作为跨设备凭据；
- 接受任意 HTTP Method、URL、Route、Shell、SQL、Python 或 Tool Name；
- 远程读取未被 Chat/Task 明确引用的任意文件；
- 远程修改 Model Credential、SOUL、全局 Memory 或 Permission Mode；
- 远程 Backup、Restore、Reset、Update、Restart 或 Shutdown；
- 提供桌面视频、鼠标、键盘或登录界面接管；
- 内置或托管 Tailscale 登录凭据；
- 自动扫描局域网全部主机或任意端口；
- 建设 Cyrene 云账号、Push Relay 或公网 Command Server。

## 3. 已审计的桌面端能力

桌面端已有以下可复用能力：

- Ed25519 签名身份和从公钥派生的稳定 `device_id`；
- X25519 Key Agreement；
- HKDF-SHA256；
- ChaCha20-Poly1305 Envelope；
- Base64URL 无 Padding 编码；
- Canonical JSON：UTF-8、Key 排序、无多余空白；
- 五分钟 Timestamp Window 和持久 Nonce Replay Protection；
- 两分钟一次性十位短密钥；
- Capability Grant 和 Project Scope；
- 固定 Remote Command Allowlist；
- Side-effect Command Idempotency；
- Public Event Allowlist；
- Durable Run/Event 和 Cursor 恢复；
- Attachment/Artifact 分块传输；
- 独立 Remote Control SQLite Sidecar 和 Audit Log。

关键桌面端源码：

```text
../Cyrene/src/cyrene/runtime/
├── remote_control.py       Identity、Pairing、Grant、Envelope、Gateway
├── remote_pairing.py       LAN Listener、地址限制、HTTP Transport
└── remote_commands.py      固定 Command 到 Workbench Service 的映射

../Cyrene/src/route/
├── control.py              Desktop-local v1 Control API
├── control_schemas.py      严格 DTO
├── remote.py               Desktop-local 远程设置和配对管理
└── remote_schemas.py       远程设置 DTO
```

桌面端现有 LAN Listener 只接受：

```text
POST /v1/pairing/claim
POST /v1/pairing/complete
POST /v1/control/envelope
```

现有 `/v1/control/envelope` 快速返回 `202 Accepted`，Command Response 通过
桌面端反向连接控制端监听端口交付。这个模式适合 Cyrene-to-Cyrene，但不适合
Android 纯控制器，因此移动端正式实现需要第 7 节的 Transport Extension。

## 4. 目标移动端源码结构

移动端使用 Kotlin、Jetpack Compose、Coroutines、Flow、Room 和
Android Keystore。采用单 Activity、单向数据流和按 Feature 拆分的模块结构。

目标目录：

```text
Cyrene-mobile/
├── README.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   └── libs.versions.toml
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── kotlin/.../mobile/
│       │   │   ├── CyreneMobileApp.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── navigation/
│       │   │   └── theme/
│       │   └── res/
│       ├── test/
│       └── androidTest/
├── core/
│   ├── common/             Result、Clock、Dispatcher、日志净化
│   ├── model/              共享领域模型
│   ├── protocol/           Canonical JSON、Bundle、Command、Envelope
│   ├── crypto/             Identity、Sign、Verify、KDF、AEAD
│   ├── network/            Pairing Client、Control Client、Endpoint Policy
│   ├── database/           Room Entity、DAO、Migration
│   ├── datastore/          非敏感 Preference
│   ├── designsystem/       Theme、Component、Icon、Typography
│   └── testing/            Fake Clock、Fixture、Protocol Vector
├── data/
│   ├── devices/
│   ├── projects/
│   ├── chats/
│   ├── runs/
│   ├── tasks/
│   └── downloads/
├── feature/
│   ├── onboarding/
│   ├── pairing/
│   ├── devices/
│   ├── projects/
│   ├── chats/
│   ├── runs/
│   ├── approvals/
│   ├── tasks/
│   ├── artifacts/
│   └── settings/
├── protocol-fixtures/
│   ├── identity-v1.json
│   ├── pairing-v1.json
│   ├── envelope-v1.json
│   └── command-v1.json
└── project-notes/
    └── cyrene-mobile-controller-handoff.zh-CN.md
```

业务依赖方向必须保持：

```text
feature UI
    │
    ▼
ViewModel / Use Case
    │
    ▼
Repository Interface
    │
    ▼
data implementation
    ├── Room
    └── core/network → core/protocol → core/crypto
```

`feature` 不得直接操作 Socket、Crypto Provider、Room DAO 或序列化细节。
`core/protocol` 不得依赖 Android UI。

## 5. Android 平台基线

实现时采用当前稳定 Android Gradle Plugin、Kotlin 和 Compose BOM，并在
`libs.versions.toml` 中锁定版本；不得在多个模块分散写死依赖版本。

基线要求：

- Kotlin；
- Jetpack Compose；
- Material 3；
- Navigation Compose；
- ViewModel + `StateFlow`；
- `collectAsStateWithLifecycle`；
- Coroutines；
- Room；
- Android Keystore；
- Gradle Kotlin DSL；
- JUnit 和 Compose UI Test；
- 最低系统建议 Android 9 / API 28；
- 编译和 Target SDK 跟随实现时的稳定 SDK；
- 提前兼容 Android 17 / API 37 的 `ACCESS_LOCAL_NETWORK`。

必需权限按功能最小化：

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

当 Target SDK 进入 API 37 时，局域网访问前声明并运行时请求：

```xml
<uses-permission android:name="android.permission.ACCESS_LOCAL_NETWORK" />
```

只有用户显式开启后台 Run 监控时，才增加符合当期 Android 政策的前台服务
声明和通知权限。第一阶段前台使用不依赖常驻 Service。

由于 LAN Listener 使用 HTTP、应用层使用 Signed + E2EE Envelope，Android
Network Security Config 可以为受控连接允许 Cleartext Transport，但所有
调用必须先经过 Endpoint Policy；不得提供接受任意公网 URL 的通用 HTTP
Client，也不得让 WebView 继承该能力。

## 6. 设备身份和本地密钥

### 6.1 Identity

移动端首次启动生成：

- Ed25519 Signing Key Pair；
- X25519 Exchange Key Pair；
- `device_id = "dev_" + base64url(SHA256(signing_public_key)[0:18])`；
- Fingerprint 为 Signing Public Key SHA-256 的分组十六进制摘要；
- 展示名默认使用 Android 设备的用户可识别名称，不参与身份验证。

密钥算法和 Raw Key 编码必须与桌面端 `remote_control.py` 一致。Crypto
Provider 需要通过 Protocol Fixture 验证，不能只依赖“算法名称相同”。

### 6.2 Keystore

若目标 Android API / Provider 能稳定使用并保留协议要求的 Ed25519/X25519
Key，应优先使用不可导出的 Android Keystore Key。

如果跨版本兼容需要保存 Raw Private Key，则：

1. 在 Android Keystore 中生成不可导出的 AES-256-GCM Master Key；
2. 用 Master Key 加密 Ed25519/X25519 Private Key Bundle；
3. Room 只保存 Ciphertext、IV 和 Version；
4. 解密后的 Raw Key 只在最短生命周期内存在内存；
5. 日志、Crash Report 和 Analytics 永远不记录 Key Bundle。

Identity、Peer Database 和 Grant 必须排除 Android Auto Backup。不同手机
不得因系统恢复而克隆同一个 `device_id`。Keystore 丢失或恢复失败时生成新
Identity，并要求用户重新配对。

### 6.3 本地解锁

第一版 App 解锁跟随系统设备锁。可选的生物识别只保护打开已配对设备和敏感
操作，不参与远程协议，也不能替代桌面端 Pairing/Grant。

## 7. Controller-only Transport

### 7.1 为什么需要新 Transport

Android 仅作为控制器时，正式连接必须满足：

- 所有连接由 Android 主动发起；
- 桌面端不需要知道 Android 可回连地址；
- Android 不开放 TCP Listener；
- Tailscale Grant 只需允许 Android → Desktop；
- App 进程停止不会留下监听服务；
- LAN 和 Tailscale 切换不要求重新配对；
- 领域 Command 和加密格式保持不变。

因此桌面端新增：

```text
POST /v1/control/request
```

它是 RemoteGateway 的新 Transport Adapter，不是新的业务 API。

### 7.2 Request

```json
{
  "envelope": {
    "version": 1,
    "message_id": "msg_...",
    "sender_device_id": "dev_android_...",
    "recipient_device_id": "dev_desktop_...",
    "kind": "command",
    "timestamp": 1785400000,
    "nonce": "...",
    "ciphertext": "...",
    "signature": "..."
  }
}
```

Command Plaintext 继续使用：

```json
{
  "request_id": "request_...",
  "command": "chats.send",
  "project_id": "project_...",
  "idempotency_key": "idem_...",
  "payload": {
    "chat_id": "chat_...",
    "message": "继续检查并修复当前问题",
    "permission_mode": "default",
    "language": "zh"
  }
}
```

### 7.3 Response

验证、执行和加密完成后返回：

```json
{
  "accepted": true,
  "envelope": {
    "version": 1,
    "message_id": "msg_...",
    "sender_device_id": "dev_desktop_...",
    "recipient_device_id": "dev_android_...",
    "kind": "response",
    "timestamp": 1785400001,
    "nonce": "...",
    "ciphertext": "...",
    "signature": "..."
  }
}
```

Response Plaintext 保持现有格式：

```json
{
  "request_id": "request_...",
  "result": {
    "ok": true
  },
  "grant": {
    "capabilities": [],
    "project_scopes": []
  }
}
```

规则：

- HTTP `200` 表示收到加密 Response；
- HTTP `400` 只用于无法解析的请求；
- HTTP `403` 用于未知或已撤销设备；
- HTTP `409` 用于协议级冲突；
- HTTP `413` 用于超限；
- HTTP `429` 用于 Rate Limit；
- HTTP `503` 用于 RemoteGateway 尚未启动；
- Command 的领域错误放在加密 `result.code/error` 中；
- 未验证请求不得得到包含设备状态、Grant 或内部错误的详细明文响应；
- 请求和响应设置 `Cache-Control: no-store`；
- Endpoint 不接受 Query 中的 Command、Token 或 Secret。

### 7.4 Long Poll

`runs.wait` 继续使用加密 Command：

```json
{
  "run_id": "run_...",
  "cursor": 42,
  "limit": 200,
  "timeout_seconds": 25
}
```

桌面 HTTP 请求最多等待 25～30 秒，返回新事件、完成状态或空事件结果。
Android 收到后：

1. 持久化 Event；
2. 原子更新 Cursor；
3. 若 Run 未完成且页面仍观察该 Run，立即发起下一次 `runs.wait`；
4. 网络失败使用有上限的指数退避；
5. App 退出时取消等待，不影响桌面 Run。

### 7.5 兼容现有 Cyrene-to-Cyrene

现有 `/v1/control/envelope` 和反向投递行为继续保留。新增 Transport 不得
修改已有 Envelope、Command、Grant、Dedupe 和 Audit 语义。

RemoteGateway 应把“解密并执行 Command、构造 Response”提取成可复用方法：

```text
receive reverse-delivery envelope ─┐
                                   ├─ validate_and_execute()
inline mobile request ─────────────┘
```

不得复制两份授权或命令执行逻辑。

## 8. 地址和连接策略

### 8.1 支持的 Endpoint

第一版只允许：

- RFC1918 IPv4；
- IPv6 Private / Link-local；
- Loopback，仅用于开发和测试；
- Tailscale CGNAT `100.64.0.0/10`；
- 用户明确输入的端口 `1024..65535`；
- 默认端口 `37841`；
- Cyrene 受限备用端口 `37841..37940`。

明确拒绝：

- `http://`、`https://` 等由用户输入的 Scheme；
- Public Internet IP；
- 任意 Domain；
- URL Path、Query 或 Fragment；
- 自动跟随 Redirect；
- 系统代理和环境代理；
- 自动扫描其他主机；
- 超出 Cyrene 备用范围的端口扫描。

第一版使用数字 IP。MagicDNS 是后续功能；实现时只能接受明确的 Tailnet
Hostname，解析后仍需验证地址和目标设备签名，避免 DNS Rebinding。

### 8.2 Endpoint 数据

同一个 Peer 可以保存多个移动端 Endpoint：

```text
Device
└── Endpoints
    ├── LAN        192.168.1.23:37841
    └── TAILSCALE  100.88.12.34:37841
```

连接顺序：

1. 最近一次成功且当前网络类型相符的 Endpoint；
2. LAN Endpoint；
3. Tailscale Endpoint；
4. 用户手动选择的 Endpoint。

每个 Endpoint 保存：

- `kind`；
- `host`；
- `port`；
- `last_success_at`；
- `last_failure_at`；
- `consecutive_failures`；
- `last_latency_ms`；
- `enabled`。

自动回退只在连接失败、超时或明确不可达时发生。已收到可信 Cyrene 响应但
Command 被拒绝时，不得把它误判为 Endpoint 故障。

### 8.3 来源 IP 和身份

短密钥领取和完成仍绑定 Pairing Source，防止同一短密钥被其他来源抢用。

配对完成后的 Controller-only Command 不再把单个 Source IP 当成长久身份：

- 先按 `sender_device_id` 查找可信 Peer；
- 验证 Signature、Recipient、Timestamp、Nonce；
- 解密 Envelope；
- 再执行 Grant、Scope、Policy 和 Schema 校验；
- 不因手机从 LAN 切换到 Tailscale 而拒绝合法设备。

Controller-only 模式不保存用于反向投递的 Android 地址。Transport Source
可以写入不含敏感信息的 Audit，但不能自动成为新信任凭据。

## 9. Pairing

### 9.1 桌面端入口

用户在桌面 Cyrene：

1. 打开“设置 → 连接”；
2. 启用远程访问；
3. 选择授予的 Capability；
4. 选择 Project Scope；
5. 生成两分钟有效的十位短密钥；
6. 查看 LAN IP、Tailscale IP（如可用）、端口和 Fingerprint。

桌面端不得默认授予全部 Project 或新增 Capability。

### 9.2 Android 入口

Pairing Screen 字段：

- 设备地址；
- 端口，默认 `37841`；
- 短密钥，接受 `ABCDE-23456` 和无连字符形式；
- 连接类型：自动 / LAN / Tailscale；
- 可选设备备注。

流程：

1. 验证地址策略；
2. `POST /v1/pairing/claim`；
3. Base64URL 解码 Invitation；
4. 验证 Version、Kind、TTL、Device ID、公钥和 Ed25519 Signature；
5. 显示桌面端 Device Name 和 Fingerprint；
6. 用户确认 Fingerprint；
7. 保存 Pending Peer；
8. 生成带移动端 Identity 的 Pairing Response；
9. `POST /v1/pairing/complete`；
10. 请求声明 `transport_mode = "request_response"`；
11. 桌面返回可信 Peer 和协商结果；
12. Android 原子提交 Peer、Grant、Scope 和 Endpoint；
13. 调用 `capabilities.read`；
14. 进入设备首页。

建议扩展 Complete Request：

```json
{
  "response": "...",
  "transport_mode": "request_response",
  "client_features": [
    "inline_response_v1",
    "durable_run_events",
    "chunked_files"
  ]
}
```

旧客户端未提供 `transport_mode` 时继续走原有 `listener_port` 行为。

### 9.3 Pairing 失败清理

- Claim 失败不写 Peer；
- Invitation 验证失败立即丢弃 Secret；
- Complete 失败撤销 Pending Peer；
- App 被杀后 Pending Pairing 不自动恢复；
- 短密钥过期提示用户在桌面重新生成；
- Fingerprint 改变时不得静默覆盖旧设备；
- 同一个 `device_id` 新增 Endpoint 时复用现有 Peer；
- 相同地址返回不同 `device_id` 时必须显示身份变更警告。

## 10. Envelope 和 Canonical Encoding

Kotlin 必须逐字节兼容桌面端协议：

```text
Canonical JSON:
- UTF-8
- Object Key 按 Unicode Code Point 顺序排序
- separators = "," 和 ":"
- 不添加空格
- 不转义非 ASCII 文本，必要 JSON 控制字符除外
- Boolean / Null 使用 JSON 标准小写

Base64:
- URL Safe Alphabet
- 无换行
- 删除尾部 "=" Padding
```

Envelope Header：

```json
{
  "version": 1,
  "message_id": "msg_...",
  "sender_device_id": "dev_...",
  "recipient_device_id": "dev_...",
  "kind": "command",
  "timestamp": 1785400000,
  "nonce": "..."
}
```

加密：

1. X25519 得到 Shared Secret；
2. 对排序后的 `sender_id|recipient_id` 做 SHA-256 作为 HKDF Salt；
3. HKDF Info 固定为 `cyrene-remote-envelope-v1`；
4. 得到 32 字节 Key；
5. Canonical Header 作为 AAD；
6. 12 字节 Nonce；
7. ChaCha20-Poly1305 加密 Canonical Plaintext；
8. Header + Ciphertext 的 Canonical JSON 由 Ed25519 签名。

解密顺序：

1. 检查 Version；
2. 检查 Sender/Recipient；
3. 检查 Timestamp；
4. 检查 Nonce 格式；
5. 验证 Signature；
6. 原子记录 Nonce；
7. 派生 Key；
8. AEAD 解密；
9. 验证 Plaintext 是 Object；
10. 匹配 `request_id`。

如果为了避免无效签名污染 Nonce Store 而调整桌面端现有顺序，桌面和 Android
必须统一，并用测试向量锁定；不得出现一端先记 Nonce、另一端后验签的隐式
差异。

## 11. Remote Command 契约

移动端按 `capabilities.read` 的结果动态启用功能，不能只通过 App Version
或桌面 Cyrene Version 推断。

### 11.1 Command Matrix

| Command | Project Scope | Side Effect | 主要 Payload |
|---|---:|---:|---|
| `capabilities.read` | 否 | 否 | `{}` |
| `projects.list` | 否 | 否 | `{}` |
| `chats.list` | 是 | 否 | `{}` |
| `chats.create` | 是 | 是 | `title` |
| `chats.read` | 是 | 否 | `chat_id` |
| `chats.send` | 是 | 是 | `chat_id,message,permission_mode,language` |
| `runs.read` | 是 | 否 | `run_id` |
| `runs.events` | 是 | 否 | `run_id,cursor,limit` |
| `runs.wait` | 是 | 否 | `run_id,cursor,limit,timeout_seconds` |
| `runs.guide` | 是 | 是 | `chat_id,message,request_id` |
| `runs.interrupt` | 是 | 是 | `chat_id` |
| `tasks.list` | 是 | 否 | `{}` |
| `tasks.create` | 是 | 是 | `goal,title,priority` |
| `tasks.read` | 是 | 否 | `task_id` |
| `tasks.dispatch` | 是 | 是 | `task_id,message,permission_mode` |
| `tasks.approve_plan` | 是 | 是 | `task_id` |
| `tasks.run_step` | 是 | 是 | `task_id,step_id,message` |
| `tasks.pause` | 是 | 是 | `task_id` |
| `tasks.resume` | 是 | 是 | `task_id` |
| `tasks.cancel` | 是 | 是 | `task_id` |
| `approvals.respond` | 是 | 是 | Chat/Task ID、`question_id,answer` |
| `artifacts.list` | 是 | 否 | `task_id` |
| `artifacts.read` | 是 | 否 | `task_id,artifact_id,offset,limit` |
| `attachments.read` | 是 | 否 | `chat_id,attachment_id,offset,limit` |

`harness.discover/describe/invoke` 不进入移动端第一版。移动端首版只控制桌面端
Chat/Task Agent，不直接把 Harness Tool Catalog 暴露给用户。

### 11.2 Idempotency

所有 Side-effect Command 生成稳定 `idempotency_key`：

```text
idem_<device-id-fragment>_<uuid>
```

规则：

- Command 发出前先持久化 Pending Command；
- 网络超时后使用原 Key 和完全相同 Payload 重试；
- 不得为自动重试生成新 Key；
- 相同 Key 不得配不同 Payload；
- 收到成功或确定的非重试错误后结束 Pending；
- `remote_command_in_progress` 使用退避后以相同 Key 重试；
- App 重启后只恢复用户仍能理解和确认的 Pending Action；
- `chats.send`、`tasks.dispatch` 等不得因页面重组重复提交。

### 11.3 Permission Mode

移动端不得提供任意字符串：

- Chat Send：只提供协议允许的 `default` / `plan`；
- Task/Approval：遵守桌面端 RemoteGateway 当前限制；
- UI 不把 `full_access` 做成移动端快捷开关；
- 最终工具权限仍由桌面端 Harness 决定；
- 远端 Grant 不能绕过桌面端审批。

## 12. 数据模型和持久化

Room 至少包含：

```text
mobile_identity
paired_devices
device_endpoints
device_grants
projects
chats
messages
runs
run_events
tasks
artifacts
pending_commands
seen_nonces
downloads
```

### 12.1 Paired Device

```text
device_id                  PK
display_name
signing_public_key
exchange_public_key
fingerprint
protocol_version
granted_capabilities_json
granted_project_scopes_json
created_at
last_seen_at
revoked_at
```

### 12.2 Run/Event

`run_events` 使用 `(run_id, cursor)` 唯一键。写入事件和更新 Run Cursor 必须
在同一事务中完成。重复 Event 不重复展示；Cursor Gap 显示恢复状态并请求
Snapshot/Events，不静默跳过。

默认只保留必要的移动端缓存：

- Run Event 7 天；
- 已完成 Run 摘要 30 天；
- Project/Chat/Task 以桌面端为权威，可按设备清理；
- 下载由用户控制；
- Nonce 至少覆盖协议 Replay Window；
- Audit 不复制桌面端敏感详情。

### 12.3 Source of Truth

- Device Identity 和 Peer Trust：本地安全存储；
- Capability/Scope：桌面端 Response 中的权威 Grant Snapshot；
- Project/Chat/Task：桌面端；
- UI 页面状态：ViewModel；
- 长期缓存：Room；
- 非敏感偏好：DataStore；
- 文件内容：App Private Storage 或用户选择的 Documents 位置。

## 13. UI 和交互

### 13.1 导航

```text
首次启动
└── Welcome
    └── Pair Device

主界面
├── Devices
│   └── Device Overview
│       └── Projects
├── Chats
│   ├── Chat List
│   └── Chat Detail / Active Run
├── Tasks
│   ├── Task List
│   └── Task Detail
├── Downloads
└── Settings
```

进入 Project 后，Chat 和 Task 默认限定当前 `device_id + project_id`，避免
跨设备或跨 Project 混淆。

### 13.2 Device Screen

显示：

- Device Name；
- Fingerprint；
- 当前 Endpoint 类型；
- LAN/Tailscale 地址；
- 延迟和最后在线时间；
- 已授权 Capability；
- 已授权 Project；
- 协议版本；
- 重新连接；
- 编辑 Endpoint；
- 撤销本机信任。

“撤销本机信任”只删除移动端 Peer 仍不等于桌面撤销。若设备在线，应先发送
受支持的撤销流程；否则清理本地并提示用户在桌面端也删除该设备。

### 13.3 Chat

Chat Detail：

- 稳定 Message Timeline；
- Markdown 安全渲染；
- Attachment；
- Pending Question；
- Active Run 状态；
- Composer；
- `default` / `plan` 模式；
- Send、Guide、Interrupt；
- 当前 Device/Project 明确标签。

公开 `reply_delta` 可以流式拼接，但只在收到 `reply_done` 或 Chat Read 后
形成稳定消息。App 重启后以桌面 Chat Detail 为权威修复未完成的本地 Delta。

### 13.4 Task

Task Detail：

- Goal；
- Status 和 Priority；
- Plan Steps；
- Pending Question；
- Goal Loop 摘要；
- Artifact；
- Dispatch；
- Approve Plan；
- Run Step；
- Pause/Resume/Cancel。

非法状态转换应在按钮层禁用，但仍以桌面端校验结果为准。

### 13.5 Error

错误至少分为：

- 地址或本地权限错误；
- Transport Timeout；
- Tailscale/LAN 不可达；
- 未配对/已撤销；
- Protocol Version 不兼容；
- Signature/Decrypt/Replay；
- Capability Denied；
- Project Scope Denied；
- Idempotency Conflict；
- Desktop Busy/Offline；
- Command Validation；
- Run/Chat/Task 不存在；
- 文件 Offset/Integrity。

不得把所有错误显示为“网络错误”。错误文案不得暴露 Ciphertext、Private
Key、Pairing Secret、完整 Envelope、绝对路径或桌面内部 Traceback。

## 14. Tailscale

### 14.1 使用方式

第一版依赖用户在 Android 和桌面安装并登录官方 Tailscale 客户端。Cyrene
Mobile 不调用 Tailscale Admin API，不保存 Auth Key，也不管理 Tailnet。

用户可输入：

```text
100.x.y.z:37841
```

当前桌面端地址验证已允许 `100.64.0.0/10`。MagicDNS 留到第二阶段。

### 14.2 最小 Grant

建议桌面设备使用 `tag:cyrene-desktop`，Tailnet 只允许指定用户访问 Cyrene
端口：

```json
{
  "tagOwners": {
    "tag:cyrene-desktop": ["autogroup:admin"]
  },
  "grants": [
    {
      "src": ["user@example.com"],
      "dst": ["tag:cyrene-desktop"],
      "ip": ["tcp:37841-37940"]
    }
  ]
}
```

如果桌面固定使用 `37841`，可进一步收窄为单端口。

Tailscale 只提供网络可达性和额外传输保护。Cyrene 仍必须独立执行：

- Pairing；
- Device Signature；
- E2EE；
- Replay Protection；
- Capability；
- Project Scope；
- Idempotency；
- Audit。

不得因为来源是 Tailnet 地址就跳过应用层认证。

## 15. 局域网

局域网流程与 Tailscale 使用同一 Pairing 和 Envelope，不存在“弱安全 LAN
模式”。

桌面端：

- Remote Access 开启后监听 `0.0.0.0:37841`；
- 端口冲突时只在 `37841..37940` 受限回退；
- 防火墙允许 Private Network/Tailscale Interface；
- 不监听公网 Interface 的路由器端口映射；
- 不建议用户配置公网 Port Forwarding。

Android：

- 只连接用户输入或已配对保存的地址；
- 不执行全网段扫描；
- 不跟随 Redirect；
- Wi-Fi 切换时取消旧请求；
- 请求绑定当前有效 Network；
- Android 17+ 在 LAN 前请求 Local Network Permission；
- 权限拒绝时仍允许用户尝试由系统 VPN 路由的 Tailscale Endpoint，但必须
  以真实设备测试验证平台行为。

## 16. 文件传输

复用桌面现有分块字段：

```json
{
  "size": 12345678,
  "offset": 0,
  "chunk_size": 524288,
  "next_offset": 524288,
  "eof": false,
  "progress": 0.042,
  "content_base64": "..."
}
```

规则：

- 默认块 512 KiB；
- 单块最大 1 MiB；
- 完整文件不设协议级总大小上限；
- 每块验证 `offset` 连续；
- Base64 解码长度必须等于 `chunk_size`；
- `next_offset = offset + chunk_size`；
- `next_offset <= size`；
- `eof` 时 `next_offset == size`；
- 下载写入临时 `.part`；
- 完成后原子 Rename；
- 中断后可从已验证 Offset 继续；
- 文件名经过 Android 文件名净化；
- MIME 只用于展示，不作为可信执行依据；
- 不自动打开可执行文件；
- 删除设备时不自动删除用户已明确保存到外部位置的文件。

如果后续协议加入完整文件 Digest，Android 必须在发布文件前验证；在此之前
AEAD 只保证每个 Response Envelope 的完整性，分块顺序由 Offset 规则保证。

## 17. 前台、后台和通知

### 17.1 第一阶段

- App 在前台时使用 `runs.wait`；
- 进入后台后允许短暂完成当前请求；
- 不启动永久后台 Listener；
- 进程停止不影响桌面 Run；
- 用户重新进入页面时从持久 Cursor 恢复。

### 17.2 用户显式监控

第二阶段可提供“在后台监控此 Run”：

- 必须由用户在 Run Screen 明确开启；
- 使用符合当前 Target SDK 的 Foreground Service Type；
- 显示不可隐藏的持续通知；
- Notification 提供“停止监控”，不是“停止桌面任务”；
- 只有用户点击独立操作才发送 `runs.interrupt`；
- Run 完成、失败或等待回答后停止 Foreground Service；
- 系统撤销通知或网络权限时安全降级；
- 不使用 WorkManager 冒充实时长轮询。

云 Push 不在当前范围。App 进程完全停止时，无法保证即时提醒。

## 18. 安全和隐私

### 18.1 Threat Model

必须覆盖：

- 同一 Wi-Fi 的被动监听者；
- LAN/Tailnet 中的主动中间人；
- 窃取短密钥后抢先领取；
- 重放 Envelope；
- 篡改 Header/Ciphertext/Signature；
- 恶意或已撤销 Peer；
- 手机时钟偏差；
- DNS Rebinding；
- DHCP/Tailscale 地址改变；
- Android 本地数据库被复制；
- Screenshot/Log/Crash 泄密；
- 重复点击或重试导致副作用重复执行；
- 超大请求、Base64 和 JSON 内存耗尽；
- 恶意文件名和文件内容；
- 桌面端返回超大或未知字段。

### 18.2 日志

Release 日志允许：

- Event Category；
- Stable Error Code；
- Endpoint Kind；
- 延迟；
- Payload Size；
- 截断的 Device/Request ID。

禁止记录：

- Private Key；
- Pairing Secret/Short Key；
- Full Public Key Bundle；
- Ciphertext；
- Signature；
- 完整 Message/Guidance/Answer；
- Attachment Base64；
- Credential；
- 桌面绝对路径；
- 完整 IP，除非用户导出明确诊断包并再次确认。

### 18.3 Screenshot 和剪贴板

Pairing Key、Fingerprint Confirm 和安全设置页面可以使用 `FLAG_SECURE`。
若允许复制 Pairing Key，Clipboard 内容应设置敏感标记并尽量缩短保留时间。

### 18.4 Dependency

- Crypto Dependency 必须固定版本；
- 禁止自行实现 Ed25519/X25519/ChaCha20 算法；
- Canonical JSON 和协议编排可以自行实现，但必须由 Cross-language Fixture
  锁定；
- Release 构建开启依赖审计和 R8；
- 第三方 Analytics 默认不接收协议或业务 Payload。

## 19. 分阶段实施顺序

### Phase 0：契约和测试向量

状态：待实现。

桌面仓库：

- 新增语言无关 `remote-protocol-v1` 说明；
- 生成固定 Ed25519/X25519 Identity Fixture；
- 生成 Pairing Invitation/Response Fixture；
- 生成 Envelope Encode/Decode Fixture；
- 生成 Tamper/Replay/Clock Error Fixture；
- 锁定 Canonical JSON 和 Base64URL。

移动仓库：

- 初始化 Gradle Multi-module；
- 建立 `core/protocol` 和 `core/crypto`；
- Kotlin Unit Test 消费同一 Fixture；
- Python Encode → Kotlin Decode；
- Kotlin Encode → Python Decode。

Phase Gate：所有 Cross-language Vector 逐字节一致。

### Phase 1：桌面 Controller-only Transport

状态：待实现。

- 新增 `/v1/control/request`；
- 提取 RemoteGateway 共用 Validate/Execute/Response；
- Pairing 协商 `request_response`；
- Controller-only Peer 不要求 Listener Port；
- 已配对身份不绑定单个 Source IP；
- 保留 Pairing Source Binding；
- 保留旧 `/v1/control/envelope`；
- 加入 Rate Limit、Size Limit、Timeout 和 Audit；
- Tailscale 与 LAN 行为测试。

Phase Gate：一个无监听端口的测试 Client 可完成 Pairing、
`projects.list → chats.create → chats.send → runs.wait`。

### Phase 2：Android Pairing 和 Device

状态：待实现。

- Keystore Identity；
- Room；
- Endpoint Policy；
- Pairing Claim/Complete；
- Fingerprint Confirm；
- LAN/Tailscale Endpoint；
- Capability/Scope；
- Device Overview；
- Revoke/Cleanup；
- Error Model。

Phase Gate：真实 Android 设备可分别通过 LAN 和 Tailscale 配对并读取共享
Project；切换地址不重新配对。

### Phase 3：Chat 和 Run

状态：待实现。

- Project List；
- Chat List/Create/Read；
- Composer；
- Send；
- Run Read/Wait/Event；
- Cursor Recovery；
- Guide/Interrupt；
- Pending Question；
- Approval Respond；
- Markdown 和 Attachment Metadata。

Phase Gate：手机创建桌面对话、启动 Agent、观察回复、补充指导并在 App
重启后恢复同一 Run。

### Phase 4：Task 和文件

状态：待实现。

- Task List/Create/Read；
- Dispatch；
- Plan Approval；
- Run Step；
- Pause/Resume/Cancel；
- Task Approval；
- Artifact List；
- Artifact/Attachment Chunk Download；
- Resume、Progress、Share。

Phase Gate：完成长任务控制和超过 10 MiB 文件的中断续传。

### Phase 5：后台监控和产品化

状态：待实现。

- 用户显式 Foreground Monitoring；
- Notification；
- Adaptive Layout；
- Accessibility；
- Localization；
- Offline/Empty/Error State；
- Performance；
- Release Signing；
- Privacy Policy；
- Play Policy Review；
- Crash-safe Secret Redaction。

Phase Gate：通过完整验收、真实网络切换、进程杀死和安全回归。

## 20. 测试基线

### 20.1 Android Unit

必须覆盖：

- Canonical JSON；
- Base64URL Padding；
- Device ID/Fingerprint；
- Ed25519 Sign/Verify；
- X25519 Shared Key；
- HKDF Context；
- ChaCha20-Poly1305 AAD；
- Envelope Encode/Decode；
- Timestamp Window；
- Nonce Replay；
- Pairing Signature/Proof/TTL；
- Endpoint Allow/Deny；
- Idempotency Persistence；
- Event Cursor/Gaps；
- File Chunk Offset；
- Error Mapping；
- Secret Redaction。

建议命令：

```bash
./gradlew test
./gradlew lint
```

### 20.2 Android Instrumentation

- Compose Navigation；
- Pairing Form；
- Fingerprint Confirm；
- Device/Project/Chat/Task State；
- Process Recreation；
- Dark Mode；
- Font Scale 200%；
- TalkBack Semantics；
- Tablet/Foldable；
- Download Storage；
- Runtime Permission；
- Foreground Monitoring。

建议命令：

```bash
./gradlew connectedCheck
```

### 20.3 桌面回归

桌面端改动必须按桌面仓库要求使用 `uv run pytest`：

```bash
cd ../Cyrene
uv run pytest -q \
  tests/test_control_api.py \
  tests/test_remote_control.py \
  tests/test_route_structure.py \
  tests/test_architecture_boundaries.py
```

完整发布前：

```bash
cd ../Cyrene
uv run pytest -q
```

### 20.4 跨仓库集成

至少覆盖：

- Android Fixture Client ↔ Desktop Test Server；
- LAN Pairing；
- Tailscale Pairing；
- LAN → Tailscale 切换；
- Tailscale → LAN 切换；
- Desktop Port Fallback；
- Android 进程重启；
- Desktop 进程重启；
- Run 跨重启终态；
- Grant 更新；
- Peer 撤销；
- Project Scope 删除；
- Duplicate Command；
- Tamper/Replay；
- 10 MiB+ 文件；
- 网络中断续传；
- 时钟偏差；
- Android Local Network Permission 拒绝。

## 21. 验收场景

正式发布至少通过：

1. 用户在桌面只共享一个 Project，手机只看到该 Project；
2. 手机通过 LAN 配对，创建 Chat 并收到 Agent 回复；
3. 同一 Peer 切到 Tailscale 地址，不重新配对；
4. Tailnet Grant 只允许 Android → Desktop 时完整工作；
5. Android 不开放任何监听端口；
6. App 退出后桌面 Run 继续；
7. App 重启后从最后 Cursor 恢复；
8. Guidance 使用 Request ID 去重；
9. 重复点击 Send 不产生两条远程消息；
10. Pending Approval 只能回答当前 Question；
11. 撤销 Peer 后下一条 Command 立即失败；
12. 删除 Project Scope 后对应页面立即失权；
13. 篡改 Ciphertext/Signature 被拒绝；
14. 重放 Nonce 被拒绝；
15. Public Event 不包含 Reasoning、绝对路径和 Tool 参数；
16. 超过 10 MiB Artifact 可断点下载；
17. LAN 不可用时回退到已保存 Tailscale Endpoint；
18. Tailscale 不可用时可以手动使用 LAN；
19. 错误 UI 区分网络、权限、协议和业务错误；
20. Release 日志不包含 Secret、Prompt、Message 或文件 Base64。

## 22. 后续修改检查清单

修改协议时：

- 是否增加 Protocol Version 或 Feature Negotiation；
- Python/Kotlin Fixture 是否同步；
- Canonical Encoding 是否变化；
- 旧 Client 是否兼容；
- 新字段是否限长并拒绝未知危险内容；
- Desktop Reverse Transport 是否保持；
- Nonce、Timestamp、Signature、E2EE 是否仍覆盖完整 Header/Payload；
- 是否新增 Side Effect，是否加入 Idempotency；
- 是否新增 Capability 和 Project Scope；
- Audit 是否净化。

修改 Android 功能时：

- 是否仍由 Repository 访问网络；
- 是否错误地把 Room Cache 当桌面权威状态；
- 是否会因 Compose 重组重复发命令；
- 是否处理 Process Death；
- 是否泄漏 Context 到 ViewModel；
- 是否增加不必要权限；
- 是否影响 Auto Backup 排除；
- 是否记录敏感 Payload；
- 是否在 LAN/Tailscale 两种连接验证；
- 是否保持无监听端口。

修改文件传输时：

- 是否验证 Offset/Size/EOF；
- 是否限制单块；
- 是否可恢复；
- 是否防 Path Traversal；
- 是否在完成前留在 Private Temp；
- 是否自动执行不可信文件；
- 是否新增完整 Digest。

## 23. 禁止回归

后续实现不得：

- 把 Android 变成 Agent Runtime；
- 把桌面 FastAPI 绑定到公网或 Tailnet；
- 要求 Android 为正式模式开放 Listener；
- 让桌面端主动连接手机成为基础依赖；
- 用 Tailscale 身份替代 Cyrene Device Identity；
- 用 Source IP 替代 Signature；
- 在配对后继续硬绑定单个 LAN IP；
- 接受任意公网 Endpoint、URL 或 Redirect；
- 扫描局域网主机；
- 让移动端自授 Capability 或 Project Scope；
- 让 Remote Command 接受任意 Tool/Shell/Route；
- 让远程审批绕过桌面 Harness；
- 把 Reasoning、Credential、绝对路径或内部 Store JSON 发给手机；
- 用新的 Idempotency Key 自动重试同一副作用；
- 把内存状态冒充 Durable Cursor；
- 在 App 退出时取消桌面 Run；
- 将 Identity 纳入 Auto Backup；
- 在日志、Crash、Analytics 或 Clipboard 长期保存 Secret；
- 为了通过测试盲目更新 Protocol Fixture；
- 在没有 Cross-language Test 的情况下更换 Crypto Provider；
- 将 MagicDNS、Cloud Relay、Push 或远程桌面偷偷加入第一版范围。

## 24. 实现开始条件

开始编码前必须确认：

- 桌面和移动仓库负责人接受 Controller-only Request/Response Transport；
- `/v1/control/request` 命名和 Feature Negotiation 已冻结；
- Android 第一版只做 Chat/Task 控制，不开放 Harness Direct Invoke；
- LAN/Tailscale 都使用相同 Pairing/E2EE；
- 最低 Android API 和 Release Target 已确定；
- Crypto Provider 已选定并能通过 Raw Key Cross-language Fixture；
- Tailscale 测试设备和真实 LAN 测试环境可用；
- 桌面端现有远程测试保持绿色；
- 本文仍是唯一正式移动端范围基线。

