# EdgeCOS APP 端 API 完整操作文档

> **版本**：v1.0（2026-07-26）
> **适用范围**：Android / iOS 客户端开发
> **后端版本**：`EdgeCOS/web/Backend-master`
> **前端参考**：`EdgeCOS/web/Frontend-main`
> **Android 已实现接口**：见各章节末尾的「Android 状态」标注

---

## 目录

- [0. 通用约定](#0-通用约定)
  - [0.1 BaseURL](#01-baseurl)
  - [0.2 鉴权机制](#02-鉴权机制)
  - [0.3 通用请求头](#03-通用请求头)
  - [0.4 通用响应结构](#04-通用响应结构)
  - [0.5 通用错误码](#05-通用错误码)
  - [0.6 限流](#06-限流)
  - [0.7 人机验证（Captcha）](#07-人机验证captcha)
  - [0.8 实名认证前置](#08-实名认证前置)
  - [0.9 流量前置](#09-流量前置)
- [1. 鉴权与注册](#1-鉴权与注册)
- [2. 用户中心](#2-用户中心)
- [3. 文件管理](#3-文件管理)
- [4. 上传](#4-上传)
- [5. 下载与预览](#5-下载与预览)
- [6. 分享](#6-分享)
- [7. 直链](#7-直链)
- [8. 套餐与订单](#8-套餐与订单)
- [9. 钱包](#9-钱包)
- [10. 流量](#10-流量)
- [11. 工单](#11-工单)
- [12. 公告](#12-公告)
- [13. 实名认证](#13-实名认证)
- [14. Passkey](#14-passkey)
- [15. 第三方 OAuth 登录](#15-第三方-oauth-登录)
- [16. 离线下载](#16-离线下载)
- [17. 安全（违规举报）](#17-安全违规举报)
- [18. 管理员公告](#18-管理员公告)
- [附录 A. 环境变量速查](#附录-a-环境变量速查)
- [附录 B. 客户端实现状态总览](#附录-b-客户端实现状态总览)

---

## 0. 通用约定

### 0.1 BaseURL

| 用途 | 值 |
|---|---|
| 生产 API BaseURL | `https://user.edgecos.com/api/` |
| 生产 Web BaseURL | `https://user.edgecos.com` |
| 付费直链域名 | `https://edgecos.com` |
| 免费直链域名 | `https://dl2.edgecos.com` |
| 主 CDN 域名 | `https://upos-ksgtm-cos.edgecos.com`（金山云） |
| 免费 CDN 域名 | 由 `TENCENT_EO_FREE_CDN_URL` 配置 |

> **Retrofit 约定**：`BASE_URL` 末尾必须带 `/`，子路径无需前导 `/`。
> **Android 已配置**：`app/build.gradle.kts` → `BuildConfig.BASE_URL = "https://user.edgecos.com/api/"`

### 0.2 鉴权机制

- **方式**：JWT 写入 **httpOnly Cookie**，Cookie 名 `token`
- **Cookie 配置**：`httpOnly=true`、`secure` 由环境决定、`sameSite=Lax`（默认）
- **有效期**：`AUTH_COOKIE_DAYS` 天（默认 15）
- **滑动续期**：距过期 ≤ 24h 时，后端在响应中自动 `Set-Cookie` 续签
- **兼容方案**：同时支持 `Authorization: Bearer <token>` 头（不推荐，移动端用 Cookie 即可）
- **状态校验**：每次请求后端都会查 `User.status`，非 `active` 直接 403

**Android 实现**：
- 使用 `EncryptedCookieJar`（基于 `EncryptedSharedPreferences`）自动持久化 Cookie
- Cookie 域名过滤：仅持久化 `user.edgecos.com` 及其子域
- 启动时若本地有 User 缓存即视为已登录态（乐观登录）
- 不实现 `okhttp3.Authenticator` 自动续签，401 直接踢登录

### 0.3 通用请求头

| Header | 必填 | 说明 |
|---|---|---|
| `Cookie: token=<jwt>` | 自动 | 由 CookieJar 注入，无需手动设置 |
| `User-Agent` | 推荐 | Android 端格式：`EdgeCOS-Android/<versionName> (<Build.MODEL>; Android <sdk>)` |
| `X-Trace-Id` | 推荐 | 32 位无连字符 UUID，便于日志追踪 |
| `Accept: application/json` | 推荐 | 默认 JSON 响应 |
| `Content-Type: application/json` | 自动 | POST/PUT/PATCH 默认 JSON |
| `X-Idempotency-Key` | 可选 | 支付/充值场景幂等键（也可通过 body `clientRequestId`） |

### 0.4 通用响应结构

#### 成功响应
直接返回业务数据 JSON，无统一外层包装。例：
```json
{ "message": "操作成功", "id": "abc-123" }
```

#### 失败响应
统一结构：
```json
{
  "code": "ERROR_CODE",
  "message": "用户可读的错误描述（中文）"
}
```
- HTTP 状态码与业务码配合判断
- `code` 字段为业务错误码（大写下划线），见 [0.5](#05-通用错误码)
- 部分错误响应可能无 `code` 字段，仅有 `message`，需兼容

### 0.5 通用错误码

#### 鉴权相关（401）
| code | 含义 | 客户端处理 |
|---|---|---|
| `AUTH_REQUIRED` | 未登录或登录已过期 | 跳登录页 |
| `AUTH_EXPIRED` | Token 已过期 | 跳登录页 |
| `AUTH_INVALID` | Token 无效或用户不存在 | 跳登录页 |

> **特例**：`POST /login` 返回的 401 不会跳转（避免循环），仅提示密码错误。

#### 账号状态（403）
| code/message | 含义 |
|---|---|
| `suspended_overquota` | 套餐过期且容量超限，账号被封禁 |
| （其他非 active 状态） | 账号已被封禁或状态异常 |

#### 业务前置（403）
| code | 含义 | 触发位置 |
|---|---|---|
| `REAL_NAME_REQUIRED` | 需先完成实名认证 | `POST /share`、`POST /generate-direct-link` |
| `TRAFFIC_EXHAUSTED` | 流量已耗尽 | 下载/预览/缩略图/直链生成 |

#### 限流（429）
- 响应头：`Retry-After: <秒>`、`X-RateLimit-Limit`、`X-RateLimit-Remaining`、`X-RateLimit-Reset`
- 响应体：`{ "message": "请求过于频繁，请稍后重试", "retryAfter": <秒> }`

#### 文件操作（409）
| code | 含义 |
|---|---|
| `INSTANT_MISS` | 秒传未命中（hash 不匹配） |
| `FILE_EXISTS` | 同名文件已存在 |
| `NAME_CONFLICT_FOLDER` | 与现有文件夹同名冲突 |
| `TARGET_NOT_FOLDER` | 移动/复制目标不是文件夹 |

#### 支付相关
| code | HTTP | 含义 |
|---|---|---|
| `MISSING_PLAN_ID` | 400 | 缺少 planId |
| `INVALID_PLAN_ID` | 400 | planId 无效 |
| `INVALID_PAYMENT_METHOD` | 400 | paymentMethod 不合法 |
| `INVALID_BILLING_MONTHS` | 400 | billingMonths 不在 1/3/12 |
| `FREE_PLAN_NOT_PAYABLE` | 400 | 免费套餐不可购买 |
| `DOWNGRADE_NOT_ALLOWED` | 400 | 不允许降级套餐 |
| `CREATE_TOO_FREQUENT` | 429 | 创建订单过于频繁 |
| `EXCEED_MAX_PAYMENT_AMOUNT` | 400 | 单笔金额超过 3000 |
| `INVALID_PAY_AMOUNT` | 400 | 应付金额无效 |
| `INSUFFICIENT_BALANCE` | 402 | 余额不足 |
| `BALANCE_PAYMENT_FAILED` | 409 | 余额支付失败 |
| `IDEMPOTENCY_METHOD_CONFLICT` | 409 | 同一幂等键用了不同支付方式 |
| `ORDER_ALREADY_PAID` | 400 | 订单已支付 |
| `ORDER_EXPIRED` | 400 | 订单已过期 |
| `ORDER_NOT_PENDING` | 400 | 订单非待支付状态 |

#### 工单
| code | HTTP | 含义 |
|---|---|---|
| `ADMIN_REPLY_ONLY` | 403 | 管理员只能回复不能提交工单 |
| `ROOT_ADMIN_ONLY` | 403 | 仅超级管理员可操作 |

### 0.6 限流

所有限流均为后端内存计数（单进程），按 `keyPrefix:rawKey` 分桶。触发后返回 429 + `Retry-After`。

| 限流器 | 窗口 | max | key 维度 | 触发场景 |
|---|---|---|---|---|
| `global` | 60s | 600 | IP | 所有端点（兜底） |
| `auth` | 60s | 20 | IP | 登录/注册/邮箱码 |
| `login` | 60s | 8 | `IP:username` | `/login` |
| `register-email-code` | 60s | 3 | `IP:email` | `/send-email-code` |
| `user-action` | 60s | 240 | `userId:IP` | 已登录用户操作 |
| `planRead` | 60s | 120 | `userId:IP` | 套餐查询 |
| `paymentApi` | 60s | 12 | `userId:IP:UA` | 支付下单 |
| `paymentNotify` | 60s | 180 | IP | 网关回调（客户端不调） |
| `wallet` | 60s | 90 | `userId:IP` | 钱包操作 |
| `captcha-precheck` | 10s | 1 | IP | `/captcha/precheck` |
| `docs-ai` | 60s | 30 | IP | AI 文档（客户端不调） |
| `shareAccess` | 60s | 20 | `shareCode:IP` | 分享访问 |
| `shareDownload` | 60s | 40 | `shareCode:IP` | 分享下载 |
| `directLinkFile` | 5min | 10 | `userId:fileId/filePath` | 直链生成 |
| `directLinkAccess` | 60s | 40 | `linkCode:IP` | 直链访问 |
| `offlineSubmit` | 10s | 10 | `userId:IP` | 离线下载提交 |
| `report` | 60s | 15 | `IP:targetType` | 违规举报 |
| `adminAction` | 60s | 60 | `userId:IP` | 管理员操作 |
| `cdn-auth` | 60s | 2000 | IP | CDN 远程鉴权（客户端不调） |
| `cdn-log` | 60s | 600 | IP | CDN 日志回调（客户端不调） |

### 0.7 人机验证（Captcha）

**触发位置**：`POST /send-email-code`、`POST /realname/start`、`POST /safety/report`

**两步流程**：

```
┌────────────────────┐         ┌────────────────────┐         ┌────────────────────┐
│ 1. POST /captcha/  │         │ 2. 腾讯 TCaptcha    │         │ 3. 业务接口        │
│    precheck        │ ──────► │    SDK 弹窗         │ ──────► │    (带 captcha*    │
│  返回: token        │         │  返回: ticket+randstr│         │     字段)          │
└────────────────────┘         └────────────────────┘         └────────────────────┘
```

#### Step 1：预检
```http
POST /api/captcha/precheck
Content-Type: application/json

{ "scene": "register-send-code" }
```
- `scene` 取值：
  - `register-send-code`：注册发送邮箱验证码
  - `register`：注册（前端实际未传，仅邮箱码场景用）
  - `realname-start`：实名认证发起
  - `safety-report`：违规举报
  - `default`：兜底
- 限流：10s/1次/IP
- 返回：`{ "token": "<24位hex>" }`
- Token TTL：5 分钟，IP 绑定，一次性

#### Step 2：腾讯云 Captcha SDK
- AppID：`197733334`（前端 .env `VITE_TENCENT_CAPTCHA_APP_ID`，Android `BuildConfig.TENCENT_CAPTCHA_APP_ID`）
- SDK：Web 端用 `https://turing.captcha.qcloud.com/TCaptcha.js`
- 用户完成滑动后回调：`{ ticket: "...", randstr: "..." }`
- **Android 端**：需用腾讯云 Captcha Android SDK 或在 WebView 中加载 `captcha.html`（项目已内置 `app/src/main/assets/captcha.html`）

#### Step 3：调用业务接口
请求体追加两个字段：
```json
{
  "email": "user@example.com",
  "captchaPrecheckToken": "<step1 返回的 token>",
  "captchaData": {
    "ticket": "<step2 返回的 ticket>",
    "randstr": "<step2 返回的 randstr>"
  }
}
```

### 0.8 实名认证前置

`requireRealName` 中间件在以下端点强制要求 `realNameStatus === 'verified'`：
- `POST /share`
- `POST /generate-direct-link`

未认证返回 403 `{ code: "REAL_NAME_REQUIRED" }`。

**客户端处理**：调用分享/直链前先调 `GET /realname/status` 检查；未认证则引导跳实名认证流程。

### 0.9 流量前置

`getTrafficState` 在以下端点检查流量是否耗尽：
- `GET /download`、`GET /preview`、`GET /preview/pdf-url`、`GET /thumbnail`
- `POST /generate-direct-link`

耗尽返回 403 `{ code: "TRAFFIC_EXHAUSTED" }`。

---

## 1. 鉴权与注册

### 1.1 POST /api/login

账号密码登录。

| 项 | 值 |
|---|---|
| 鉴权 | 否 |
| Captcha | 否 |
| 限流 | `auth` 20/min/IP + `login` 8/min/`IP:username` |

**请求体**：
```json
{
  "username": "alice",
  "password": "Pass1234"
}
```
- `username`：支持用户名 / 手机号 / 邮箱
- `password`：明文（HTTPS 传输）

**成功响应**（200）：
```json
{
  "user": {
    "id": 1,
    "username": "alice",
    "role": "user",
    "email": "alice@example.com",
    "qqNumber": null,
    "qqOpenId": null,
    "qqNickname": null,
    "qqAvatar": null,
    "oauthAccounts": {},
    "realNameStatus": "unverified",
    "realNameMaskedName": null,
    "realNameMaskedIdNo": null,
    "realNameVerifiedAt": null
  }
}
```
- 同时通过 `Set-Cookie: token=<jwt>; HttpOnly; ...` 写入 Cookie

**错误**：
- 400：参数缺失
- 401：用户名或密码错误（**此 401 不会触发跳转**）
- 500：登录失败

**Android Retrofit**：
```kotlin
@POST("login")
suspend fun login(@Body req: LoginRequest): LoginResponse
```

**Android 状态**：✅ 已实现（`AuthRepository.login`）

---

### 1.2 POST /api/register

用户注册。

| 项 | 值 |
|---|---|
| 鉴权 | 否 |
| Captcha | 否（但需先调 `/send-email-code`，那一步需要 Captcha） |
| 限流 | `auth` 20/min/IP |

**请求体**：
```json
{
  "username": "alice",
  "email": "alice@example.com",
  "verifyCode": "123456",
  "password": "Pass1234",
  "captchaData": { "ticket": "...", "randstr": "..." },
  "captchaPrecheckToken": "<from /captcha/precheck scene=register>"
}
```
- `username`：3-15 位中文/字母/数字，正则 `^[\u4e00-\u9fa5A-Za-z0-9]{3,15}$`
- `email`：合法邮箱
- `verifyCode`：6 位数字（来自邮箱）
- `password`：≥8 位，必须含数字 + 字母
- `captchaData` + `captchaPrecheckToken`：见 [0.7](#07-人机验证captcha)（前端实际注册时也会传，后端校验可选）

**成功响应**（200）：
```json
{ "message": "注册成功" }
```

**错误**：400（多项校验）、500（注册失败）

**Android Retrofit**：
```kotlin
@POST("register")
suspend fun register(@Body req: RegisterRequest): LoginResponse
```

**Android 状态**：✅ 已实现

---

### 1.3 POST /api/send-email-code

发送注册邮箱验证码。

| 项 | 值 |
|---|---|
| 鉴权 | 否 |
| Captcha | **是** |
| 限流 | `auth` 20/min/IP + `register-email-code` 3/min/`IP:email` |

**请求体**：
```json
{
  "email": "alice@example.com",
  "captchaPrecheckToken": "<token>",
  "captchaData": { "ticket": "...", "randstr": "..." }
}
```

**成功响应**（200）：
```json
{ "message": "验证码已发送" }
```

**错误**：400（邮箱为空/格式错/已注册）、500（发送失败）

**Android Retrofit**：
```kotlin
@POST("send-email-code")
suspend fun sendEmailCode(@Body req: SendEmailCodeRequest): Map<String, Any?>
```

**Android 状态**：✅ 已实现

---

### 1.4 POST /api/captcha/precheck

人机验证预检，详见 [0.7](#07-人机验证captcha)。

**Android Retrofit**：
```kotlin
@POST("captcha/precheck")
suspend fun captchaPrecheck(@Body req: CaptchaPrecheckRequest): CaptchaPrecheckResponse
```

**Android 状态**：✅ 已实现

---

## 2. 用户中心

> 本组接口挂载在 `/api/user`，全局应用 `authenticateToken` + `userActionRateLimiter`（240/min/userId+IP）。

### 2.1 GET /api/user/profile

获取当前用户完整资料。

**响应**：
```json
{
  "id": 1,
  "username": "alice",
  "phoneNumber": null,
  "email": "alice@example.com",
  "qqNumber": null,
  "qqOpenId": null,
  "qqNickname": null,
  "qqAvatar": null,
  "oauthAccounts": {
    "wx": { "provider": "wx", "uid": "...", "nickname": "...", "avatar": "..." }
  },
  "role": "user",
  "createdAt": "2026-01-01T00:00:00.000Z",
  "realNameStatus": "verified",
  "realNameMaskedName": "张*三",
  "realNameMaskedIdNo": "510***********1234",
  "realNameVerifiedAt": "2026-02-01T00:00:00.000Z"
}
```

**Android 状态**：⚠️ 已声明 `getUserProfile()` 但 `AuthRepository` 未调用（建议启动时调一次同步本地缓存）

---

### 2.2 PUT /api/user/profile

修改用户名。

**请求体**：
```json
{ "username": "newname" }
```

**响应**：
```json
{
  "message": "Profile updated",
  "user": { "id": 1, "username": "newname", "email": "...", "..." : "..." }
}
```

**Android 状态**：❌ 未实现

---

### 2.3 POST /api/user/change-password

修改密码。

**请求体**：
```json
{
  "currentPassword": "OldPass1234",
  "newPassword": "NewPass1234"
}
```
- `newPassword`：≥8 位含数字 + 字母

**响应**：`{ "message": "Password changed successfully" }`

**Android 状态**：❌ 未实现

---

### 2.4 POST /api/user/bind-email/code

发送绑定邮箱验证码（已登录用户绑定新邮箱）。

**请求体**：
```json
{ "email": "new@example.com" }
```

**响应**：`{ "message": "Verification code sent to email" }`

**Android 状态**：❌ 未实现

---

### 2.5 POST /api/user/bind-email

绑定邮箱。

**请求体**：
```json
{ "email": "new@example.com", "code": "123456" }
```

**响应**：`{ "message": "Email bound successfully", "email": "new@example.com" }`

**Android 状态**：❌ 未实现

---

### 2.6 GET /api/user/quota

获取存储配额（注意：此接口挂载在 `routes/files.js`，路径前缀是 `/`，不是 `/user`）。

**响应**：
```json
{
  "quota": "10737418240",
  "used": "5368709120"
}
```
- BigInt 字符串，需用 `BigInteger` 解析

**Android Retrofit**：
```kotlin
@GET("user/quota")
suspend fun getUserQuota(): QuotaResponse
```

**Android 状态**：✅ 已实现

---

## 3. 文件管理

> 本组接口挂载在 `/api/`（前缀为空），每个端点单独应用 `authenticateToken` + `userActionRateLimiter`。
> **路径约定**：根目录为 `/`，子目录如 `/docs/work`，分隔符为 `/`。

### 3.1 GET /api/files

列出目录下的文件。

**Query 参数**：
| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `path` | string | 否 | `/` | 目录路径 |

**响应**：
```json
[
  {
    "id": "uuid-1234",
    "filename": "report.pdf",
    "basename": "report",
    "lastmod": "2026-07-26T10:00:00.000Z",
    "size": "1048576",
    "type": "file",
    "thumbnail": "https://cdn.../thumb-xxx.jpg?sign=...",
    "hasThumbnail": true,
    "activeShare": null
  },
  {
    "id": "uuid-5678",
    "filename": "photos",
    "basename": "photos",
    "lastmod": "2026-07-26T09:00:00.000Z",
    "size": "0",
    "type": "directory",
    "thumbnail": null,
    "hasThumbnail": false,
    "activeShare": {
      "shareId": "abc1234567",
      "expiration": "2026-08-26T00:00:00.000Z",
      "extractionCode": "ABCD"
    }
  }
]
```
- `size` 为 BigInt 字符串
- `type`：`file` / `directory`
- `activeShare`：若该文件已有未过期的分享则填充

**Android Retrofit**：
```kotlin
@GET("files")
suspend fun listFiles(@Query("path") path: String = "/"): List<FileNodeDto>
```

**Android 状态**：✅ 已实现

---

### 3.2 GET /api/files/summary

获取首页资源概览。

**响应**：
```json
{
  "fileCount": 152,
  "folderCount": 18,
  "recentCount": 23,
  "sharedCount": 5
}
```

**Android Retrofit**：
```kotlin
@GET("files/summary")
suspend fun getFilesSummary(): FileSummaryDto
```

**Android 状态**：✅ 已实现

---

### 3.3 GET /api/trash

列出回收站文件。

**响应**：
```json
[
  {
    "id": "uuid-xxx",
    "parentId": "uuid-parent",
    "basename": "deleted-file.pdf",
    "size": "1048576",
    "type": "file",
    "trashedAt": "2026-07-25T10:00:00.000Z"
  }
]
```

**Android Retrofit**：
```kotlin
@GET("trash")
suspend fun listTrash(): List<FileNodeDto>
```

**Android 状态**：✅ 已实现

---

### 3.4 POST /api/mkdir

新建文件夹。

**请求体**：
```json
{ "path": "/docs/new-folder" }
```

**响应**：`{ "message": "Directory created" }`

**Android Retrofit**：
```kotlin
@POST("mkdir")
suspend fun mkdir(@Body req: MkdirRequest): Map<String, Any?>
```

**Android 状态**：✅ 已实现

---

### 3.5 POST /api/rename

重命名文件或文件夹。

**请求体**（二选一）：
```json
{ "oldPath": "/docs/old-name.pdf", "newName": "new-name.pdf" }
```
或
```json
{ "id": "uuid-xxx", "newName": "new-name.pdf" }
```

**响应**：`{ "message": "重命名成功" }`

**Android Retrofit**：
```kotlin
@POST("rename")
suspend fun rename(@Body req: RenameRequest): Map<String, Any?>
```

**Android 状态**：✅ 已实现

---

### 3.6 POST /api/move

移动单个文件（到目标目录）。

**请求体**（sourcePath/sourceId 二选一）：
```json
{ "sourcePath": "/docs/file.pdf", "targetDir": "/archive" }
```
或
```json
{ "sourceId": "uuid-xxx", "targetDir": "/archive" }
```

**响应**：`{ "message": "移动成功" }`

**Android 状态**：✅ 已实现

---

### 3.7 POST /api/copy

复制单个文件。

**请求体**：同 [3.6](#36-post-apimove)

**响应**：`{ "message": "复制成功" }`

**Android 状态**：✅ 已实现

---

### 3.8 POST /api/batch/move

批量移动。

**请求体**：
```json
{
  "sourceIds": ["uuid-1", "uuid-2"],
  "targetDir": "/archive"
}
```
或
```json
{
  "sourcePaths": ["/docs/a.pdf", "/docs/b.pdf"],
  "targetDir": "/archive"
}
```
- 最多 100 个

**响应**：`{ "message": "批量移动成功", "count": 2 }`

**Android 状态**：✅ 已实现

---

### 3.9 POST /api/batch/copy

批量复制。同 [3.8](#38-post-apibatchmove)。

**响应**：`{ "message": "批量复制成功", "count": 2 }`

**Android 状态**：✅ 已实现

---

### 3.10 POST /api/delete

移至回收站（单个）。

**请求体**（path/id 二选一）：
```json
{ "path": "/docs/file.pdf" }
```
或
```json
{ "id": "uuid-xxx" }
```

**响应**：`{ "message": "已移至回收站" }`

**Android 状态**：✅ 已实现

---

### 3.11 POST /api/batch/delete

批量移至回收站。

**请求体**：
```json
{ "ids": ["uuid-1", "uuid-2"] }
```
或
```json
{ "paths": ["/docs/a.pdf", "/docs/b.pdf"] }
```

**响应**：`{ "message": "批量移至回收站成功", "count": 2 }`

**Android 状态**：✅ 已实现

---

### 3.12 POST /api/trash/restore

恢复回收站文件（单条）。

**请求体**：`{ "id": "uuid-xxx" }`

**响应**：`{ "message": "文件恢复成功" }`

**Android 状态**：⚠️ 已声明但 `FileRepository` 实际调 `batchRestoreTrash`，单条接口为死代码

---

### 3.13 POST /api/trash/delete

彻底删除（单条）。

**请求体**：`{ "id": "uuid-xxx" }`

**响应**：`{ "message": "彻底删除成功" }`

**Android 状态**：⚠️ 同上，死代码

---

### 3.14 POST /api/batch/trash/restore

批量恢复。

**请求体**：`{ "ids": ["uuid-1", "uuid-2"] }`

**响应**：`{ "message": "批量恢复成功", "count": 2 }`

**Android 状态**：✅ 已实现

---

### 3.15 POST /api/batch/trash/delete

批量彻底删除。

**请求体**：`{ "ids": ["uuid-1", "uuid-2"] }`

**响应**：`{ "message": "批量彻底删除成功", "count": 2 }`

**Android 状态**：✅ 已实现

---

### 3.16 GET /api/file/content

读取文本/markdown 文件内容（≤5MB）。

**Query 参数**：
| 参数 | 必填 | 说明 |
|---|---|---|
| `path` 或 `id` | 是 | 文件标识（二选一） |
| `trash` | 否 | 是否回收站文件 |

**响应**：`{ "content": "文件内容字符串" }`

**Android 状态**：❌ 未实现

---

### 3.17 PUT /api/file/content

保存文本/markdown 文件内容。

**请求体**（path/id 二选一）：
```json
{ "id": "uuid-xxx", "content": "新内容" }
```
- `content` ≤ 5MB

**响应**：`{ "message": "文件更新成功" }`

**Android 状态**：❌ 未实现

---

## 4. 上传

> **Android 实现状态**：仅支持 ≤100MB 单文件直传 + 秒传；分片上传未实现。

### 4.1 上传前检查冲突

#### GET /api/upload/conflict

**Query 参数**：
| 参数 | 必填 | 说明 |
|---|---|---|
| `filename` | 是 | 文件名 |
| `path` | 否 | 目标目录（默认 `/`） |

**响应**：
```json
{ "exists": true, "isFolder": false, "id": "uuid-xxx" }
```

**Android 状态**：✅ 已实现

---

### 4.2 获取上传 Token（≤100MB 单文件）

#### GET /api/upload/token

**Query 参数**：
| 参数 | 必填 | 说明 |
|---|---|---|
| `filename` | 是 | 文件名 |
| `filesize` | 是 | 字节数（≤100GB） |
| `path` | 否 | 目标目录 |
| `hash` | 是 | 格式 `sha256:<hex>:<size>` |
| `overwrite` | 否 | 是否覆盖同名 |
| `skipInstant` | 否 | 跳过秒传检测（秒传失败重试时设为 true） |

**响应 A（秒传命中）**：
```json
{
  "method": "INSTANT",
  "instant": true,
  "key": "users/<userId>/<uuid>.pdf",
  "size": "1048576",
  "hash": "sha256:abc...:1048576"
}
```
→ 客户端直接调 [4.4 POST /api/upload/instant-complete](#44-post-apiuploadinstant-complete)

**响应 B（需直传 OSS）**：
```json
{
  "method": "PUT",
  "url": "https://<oss-endpoint>/<bucket>/users/<userId>/<uuid>.pdf?X-Amz-...",
  "key": "users/<userId>/<uuid>.pdf"
}
```
→ 客户端 `PUT` 文件到 `url`（**不带 Cookie**，使用 RawHttpClient）
→ 完成后调 [4.3 POST /api/upload/complete](#43-post-apiuploadcomplete)

**Hash 计算规范**：
- 算法：SHA-256
- 格式：`sha256:<hex>:<size>`
- Android 实现：`Sha256Calculator`（流式计算）
- 前端参考：`hash-wasm` 16MB 分片计算

**Android Retrofit**：
```kotlin
@GET("upload/token")
suspend fun getUploadToken(
    @Query("filename") filename: String,
    @Query("filesize") filesize: Long,
    @Query("path") path: String,
    @Query("hash") hash: String,
    @Query("overwrite") overwrite: Boolean = false
): UploadTokenResponse
```

**Android 状态**：✅ 已实现（`UploadRepository.getUploadToken` + `Uploader` PUT 直传）

---

### 4.3 POST /api/upload/complete

直传 OSS 完成后通知后端。

**请求体**：
```json
{
  "key": "users/<userId>/<uuid>.pdf",
  "filename": "report.pdf",
  "path": "/docs",
  "size": "1048576",
  "hash": "sha256:abc...:1048576",
  "overwrite": false
}
```

**响应**：
```json
{
  "message": "Upload complete",
  "id": "uuid-xxx",
  "key": "users/...",
  "size": "1048576",
  "hash": "sha256:...",
  "overwritten": false,
  "deduped": false
}
```
- `deduped`：true 表示触发了后端去重（hash 已存在）

**Android 状态**：✅ 已实现

---

### 4.4 POST /api/upload/instant-complete

秒传命中后通知后端。

**请求体**：
```json
{
  "key": "users/<userId>/<uuid>.pdf",
  "filename": "report.pdf",
  "path": "/docs",
  "size": "1048576",
  "hash": "sha256:abc...:1048576",
  "overwrite": false
}
```

**响应**：
```json
{
  "message": "Instant upload complete",
  "instant": true,
  "id": "uuid-xxx",
  "key": "users/...",
  "size": "1048576",
  "hash": "sha256:...",
  "overwritten": false,
  "deduped": true
}
```

**Android 状态**：✅ 已实现

---

### 4.5 POST /api/upload/multipart/init

分片上传初始化（文件 >100MB）。

**请求体**：
```json
{
  "filename": "big-video.mp4",
  "filesize": 1073741824,
  "path": "/videos",
  "hash": "sha256:abc...:1073741824",
  "overwrite": false,
  "skipInstant": false
}
```

**响应 A（秒传命中）**：同 [4.2 响应 A](#42-获取上传-token-100mb-单文件)

**响应 B（需分片上传）**：
```json
{
  "uploadId": "<OSS multipart uploadId>",
  "key": "users/<userId>/<uuid>.mp4"
}
```

**Android 状态**：❌ 未实现（`UploadTaskEntity` 已预留 `uploadId/key/completedParts`）

---

### 4.6 POST /api/upload/multipart/urls

获取分片上传 URL（每个分片一个 PUT URL）。

**请求体**：
```json
{
  "uploadId": "<from init>",
  "key": "users/<userId>/<uuid>.mp4",
  "partNumbers": [1, 2, 3, 4, 5]
}
```
- 一次最多 1000 个 part
- partNumber 范围 1-10000

**响应**：
```json
{
  "urls": {
    "1": "https://<oss>/?partNumber=1&uploadId=...&X-Amz-...",
    "2": "https://<oss>/?partNumber=2&uploadId=...&X-Amz-...",
    "3": "..."
  }
}
```

**Android 调用流程**：
```kotlin
// 每个分片
rawHttpClient.newCall(
    Request.Builder().url(urls[partNumber])
        .put(blob.toRequestBody())
        .build()
).execute()
// 从 ETag header 提取，保存到 UploadTaskEntity.completedParts
```

**Android 状态**：❌ 未实现

---

### 4.7 POST /api/upload/multipart/complete

所有分片上传完毕，通知后端合并。

**请求体**：
```json
{
  "uploadId": "<from init>",
  "key": "users/<userId>/<uuid>.mp4",
  "parts": [
    { "ETag": "\"abc\"", "PartNumber": 1 },
    { "ETag": "\"def\"", "PartNumber": 2 }
  ]
}
```
- `parts` 可选（部分 OSS 兼容实现不需要）

**响应**：
```json
{ "message": "分片上传完成，请调用 /upload/complete" }
```
→ 接着调 [4.3 POST /api/upload/complete](#43-post-apiuploadcomplete)

**Android 状态**：❌ 未实现

---

### 4.8 POST /api/upload/multipart/abort

取消分片上传（清理 OSS 上的分片）。

**请求体**：
```json
{ "uploadId": "<from init>", "key": "users/..." }
```

**响应**：`{ "message": "分片上传已取消" }`

**Android 状态**：❌ 未实现

---

### 4.9 POST /api/upload/thumbnail/token

获取图片缩略图上传 URL（图片上传成功后调用）。

**请求体**：
```json
{ "size": 51200 }
```
- `size` ≤ 512KB

**响应**：
```json
{
  "method": "PUT",
  "url": "https://<oss>/thumb-xxx.jpg?X-Amz-...",
  "key": "thumbnails/<uuid>.jpg"
}
```

**Android 状态**：❌ 未实现

---

### 4.10 POST /api/upload/thumbnail/complete

绑定缩略图到文件。

**请求体**（fileKey/fileId 二选一）：
```json
{
  "fileKey": "users/<userId>/<uuid>.jpg",
  "thumbnailKey": "thumbnails/<uuid>.jpg"
}
```

**响应**：`{ "message": "缩略图绑定成功" }`

**Android 状态**：❌ 未实现

---

## 5. 下载与预览

### 5.1 GET /api/download

下载文件（302 重定向到 CDN 签名 URL）。

**Query 参数**：
| 参数 | 必填 | 说明 |
|---|---|---|
| `path` 或 `id` | 是 | 文件标识 |
| `line` | 否 | `default`/`free`/`cdn`；`free` 走免费线 |

**响应**：302 重定向，`Location` 头指向 CDN URL

**Android 实现**：
- 不走 Retrofit，直接用 OkHttp 流式下载（支持 Range 断点续传）
- URL 拼接：`"${ClientConfig.baseUrl}/download?path=<encoded>&id=<encoded>&line=default"`
- `Downloader` 类负责断点续传 + 进度回调

**Android 状态**：✅ 已实现（`Downloader` + `FilesViewModel.download()`）

---

### 5.2 GET /api/preview

预览文件（302 重定向，支持图片/视频/音频）。

**Query 参数**：
| 参数 | 必填 | 说明 |
|---|---|---|
| `path` 或 `id` | 是 | 文件标识 |
| `trash` | 否 | 是否回收站文件 |
| `line` | 否 | 线路 |

**响应**：302 重定向

**Android 状态**：❌ 未实现（media 模块空壳）

---

### 5.3 GET /api/preview/pdf-url

获取 PDF 预览 URL（≤50MB PDF）。

**Query 参数**：同 [5.2](#52-get-apipreview)

**响应**：`{ "url": "https://cdn.../file.pdf?sign=..." }`

**Android 状态**：❌ 未实现

---

### 5.4 GET /api/thumbnail

获取缩略图（302 重定向）。

**Query 参数**：`path` 或 `id`

**响应**：302 重定向

**Android 状态**：❌ 未实现

---

## 6. 分享

> **前置**：所有「创建分享」操作需 `realNameStatus === 'verified'`，否则 403 `REAL_NAME_REQUIRED`。

### 6.1 POST /api/share

创建分享。

| 项 | 值 |
|---|---|
| 鉴权 | 是 |
| 实名 | **是** |
| 限流 | `user-action` |

**请求体**：
```json
{
  "fileId": "uuid-xxx",
  "fileName": "report.pdf",
  "expirationType": "7d",
  "customExpiration": null,
  "extractionCode": "ABCD"
}
```
- `fileId` 或 `filePath`：二选一
- `fileName`：必填
- `expirationType`：`1d` / `7d` / `30d` / `permanent` / `custom`
- `customExpiration`：ISO 8601 字符串（仅 `expirationType=custom` 时）
- `extractionCode`：4-8 位字母数字（可选）

**响应**：
```json
{
  "shareId": "abc1234567",
  "reused": false,
  "activeShare": {
    "shareId": "abc1234567",
    "expiration": "2026-08-02T00:00:00.000Z",
    "extractionCode": "ABCD"
  }
}
```

**Android 状态**：❌ 未实现

---

### 6.2 DELETE /api/share/file/:fileId

关闭文件的分享。

**Path 参数**：`fileId`（UUID）

**响应**：
```json
{ "message": "分享已关闭", "deleted": true }
```

**Android 状态**：❌ 未实现

---

### 6.3 GET /api/share/:shareId

获取分享信息（**无需登录**）。

**Path 参数**：`shareId`（10 位字符）

**响应**：
```json
{
  "fileName": "report.pdf",
  "owner": "alice",
  "createdAt": "2026-07-26T00:00:00.000Z",
  "expiration": "2026-08-02T00:00:00.000Z",
  "needExtractionCode": true,
  "isFolder": false
}
```

**Android 状态**：❌ 未实现

---

### 6.4 POST /api/share/:shareId/files

获取分享内的文件列表（**无需登录**，文件夹分享用）。

| 项 | 值 |
|---|---|
| 鉴权 | 否 |
| 限流 | `shareAccess` 20/min/`shareCode:IP` |

**请求体**：
```json
{
  "path": "/",
  "code": "ABCD"
}
```
- `code`：若分享设了提取码则必填

**响应**：同 [3.1 GET /api/files](#31-get-apifiles) 响应结构

**Android 状态**：❌ 未实现

---

### 6.5 GET /api/share/:shareId/download

下载分享内文件（**无需登录**，302 重定向）。

**Query 参数**：`code`、`path`

**限流**：`shareAccess` + `shareDownload` 40/min/`shareCode:IP`

**响应**：302 重定向

**热点熔断**：单 shareCode 10 分钟内 >600 次访问 → 自动下线 + 标记 `safetyStatus=review`，返回 429

**Android 状态**：❌ 未实现

---

## 7. 直链

> **前置**：`requireRealName` + `directLinkFile` 限流（5min/10次/userId+fileId/filePath）。

### 7.1 POST /api/generate-direct-link

生成直链。

**请求体**（filePath/fileId 二选一）：
```json
{ "fileId": "uuid-xxx" }
```

**响应**：
```json
{
  "linkCode": "xyz9876543",
  "directUrl": "https://edgecos.com/f/xyz9876543"
}
```

**Android 状态**：❌ 未实现

---

### 7.2 GET /api/f/:linkCode

访问直链（302 重定向，**无需登录**）。

**Path 参数**：`linkCode`（10 位字符）

**限流**：`directLinkAccess` 40/min/`linkCode:IP`

**响应**：
- 付费线：302 到 `signKsCdnUrl(...)`（金山云 CDN 签名）
- 免费线：302 到 `https://dl2.edgecos.com/...`（腾讯 EdgeOne 免费 CDN）

**热点熔断**：单 linkCode 10 分钟内 >800 次 → 自动下线，返回 429

**Android 状态**：❌ 未实现

---

## 8. 套餐与订单

### 8.1 GET /api/plans

获取所有套餐列表（**无需登录**）。

**响应**：
```json
[
  {
    "id": 1,
    "name": "免费版",
    "quota": "10737418240",
    "trafficQuota": "10737418240",
    "price": "0.00",
    "originalPrice": null,
    "duration": 0,
    "level": 0,
    "isTrafficPackage": false,
    "discount": null,
    "contactOnly": false,
    "contactEmail": null
  }
]
```
- `quota`：存储配额（BigInt 字符串）
- `trafficQuota`：流量配额
- `price`：价格（DECIMAL 字符串）
- `duration`：0=永久，1=1 月
- `isTrafficPackage`：是否流量包
- `contactOnly`：仅联系销售（不可在线购买）

**Android 状态**：❌ 未实现

---

### 8.2 GET /api/my-plan

获取当前用户套餐信息。

| 项 | 值 |
|---|---|
| 限流 | `planRead` 120/min/userId+IP |

**响应**：
```json
{
  "plan": { "id": 2, "name": "高级版", "quota": "107374182400", "..." : "..." },
  "expireTime": "2027-07-26T00:00:00.000Z",
  "quota": "107374182400",
  "used": "5368709120",
  "trafficQuota": "107374182400",
  "trafficUsed": "5368709120",
  "monthlyTrafficUsed": "5368709120",
  "monthlyTrafficRemaining": "101818716160",
  "extraTraffic": "0",
  "totalPurchasedTraffic": "10737418240",
  "packageTrafficRemaining": "5368709120",
  "packageTrafficUsedInCycle": "5368709120",
  "totalTrafficRemaining": "112589990684262",
  "trafficUsagePercent": 5,
  "trafficStatus": "healthy",
  "packagePolicy": "..."
}
```

**Android 状态**：❌ 未实现（`PlanSummary` domain 模型已预留）

---

### 8.3 GET /api/orders

获取订单列表。

**响应**：
```json
{
  "list": [
    {
      "id": 1,
      "tradeNo": "EC202607261234567890",
      "planId": 2,
      "amount": "99.00",
      "status": "paid",
      "paymentMethod": "alipay",
      "expireAt": "2026-07-26T13:00:00.000Z",
      "payTime": "2026-07-26T12:30:00.000Z",
      "billingMonths": 1,
      "Plan": { "id": 2, "name": "高级版", "..." : "..." }
    }
  ],
  "summary": { "total": 10, "paid": 8, "pending": 1, "cancelled": 1, "expired": 0 }
}
```

**Android 状态**：❌ 未实现

---

### 8.4 GET /api/orders/:tradeNo

获取单个订单详情。

**Path 参数**：`tradeNo`（8-64 位字母数字_-）

**响应**：
```json
{
  "order": {
    "id": 1,
    "tradeNo": "EC202607261234567890",
    "planId": 2,
    "amount": "99.00",
    "status": "paid",
    "Plan": { "id": 2, "name": "高级版" },
    "billingMonths": 1
  }
}
```

**Android 状态**：❌ 未实现

---

### 8.5 POST /api/pay/create

创建支付订单。

| 项 | 值 |
|---|---|
| 鉴权 | 是 |
| 限流 | `paymentApi` 12/min/userId+IP+UA |

**请求体**：
```json
{
  "planId": 2,
  "paymentMethod": "alipay",
  "clientRequestId": "<uuid>",
  "billingMonths": 1
}
```
- `paymentMethod`：`alipay` / `wxpay` / `balance`（余额支付）
- `billingMonths`：1 / 3 / 12（默认 1）
- `clientRequestId`：幂等键，建议用 UUID
- 也可通过 `X-Idempotency-Key` 头传

**响应 A（余额支付成功）**：
```json
{
  "status": "paid",
  "tradeNo": "EC202607261234567890",
  "order": { "id": 1, "..." : "..." },
  "bill": { "id": 1, "tradeNo": "...", "amount": "99.00", "..." : "..." },
  "balance": "901.00",
  "paymentMethod": "balance",
  "traceId": "..."
}
```

**响应 B（网关支付）**：
```json
{
  "tradeNo": "EC202607261234567890",
  "payUrl": "https://api.payqixiang.cn/mapi.php?...",
  "params": { "...": "..." },
  "html": "<form>...</form>",
  "payType": "alipay",
  "expireAt": "2026-07-26T13:00:00.000Z",
  "remainingSeconds": 1800,
  "traceId": "...",
  "reused": false,
  "notice": "..."
}
```
- 客户端打开 `payUrl`（或新窗口写入 `html` 自动提交表单）
- 同时启动轮询 [8.7 POST /api/pay/query](#87-post-apipayquery) 或 [9.4 POST /api/wallet/recharge/query](#94-post-apiwalletrechargequery) 查询支付状态

**Android 状态**：❌ 未实现

---

### 8.6 POST /api/pay/repay

重新支付已取消/未完成的订单。

**请求体**：
```json
{
  "tradeNo": "EC202607261234567890",
  "paymentMethod": "alipay",
  "clientRequestId": "<uuid>"
}
```

**响应**：同 [8.5](#85-post-apipaycreate)

**Android 状态**：❌ 未实现

---

### 8.7 POST /api/pay/query

查询支付状态。

**请求体**：
```json
{ "tradeNo": "EC202607261234567890" }
```

**响应**：
```json
{
  "status": "paid",
  "order": { "..." : "..." },
  "traceId": "...",
  "remainingSeconds": 0
}
```
- `status`：`paid` / `pending` / `expired`

**Android 状态**：❌ 未实现

---

### 8.8 POST /api/pay/cancel

取消订单。

**请求体**：`{ "tradeNo": "EC..." }`

**响应**：`{ "message": "订单已取消", "traceId": "..." }`

**Android 状态**：❌ 未实现

---

## 9. 钱包

> 全局限流 `walletLimiter` 90/min/userId+IP。

### 9.1 GET /api/wallet/summary

获取钱包概览。

**响应**：
```json
{
  "balance": "1000.00",
  "monthExpense": "50.00",
  "totalExpense": "200.00",
  "totalRecharge": "1200.00",
  "pendingRecharge": "0.00"
}
```

**Android 状态**：❌ 未实现

---

### 9.2 GET /api/wallet/bills

获取账单列表。

**Query 参数**：
| 参数 | 类型 | 说明 |
|---|---|---|
| `type` | string | `recharge` / `expense` |
| `category` | string | 账单分类 |
| `status` | string | 账单状态 |
| `limit` | int | 1-100，默认 50 |

**响应**：
```json
{
  "list": [
    {
      "id": 1,
      "tradeNo": "EC...",
      "userId": 1,
      "orderId": null,
      "type": "recharge",
      "category": "wallet",
      "title": "余额充值",
      "amount": "100.00",
      "status": "completed",
      "paymentMethod": "alipay",
      "balanceBefore": "900.00",
      "balanceAfter": "1000.00",
      "payTime": "2026-07-26T12:00:00.000Z",
      "expireAt": null
    }
  ]
}
```

**Android 状态**：❌ 未实现

---

### 9.3 POST /api/wallet/recharge

发起充值。

**请求体**：
```json
{
  "amount": "100.00",
  "paymentMethod": "alipay",
  "clientRequestId": "<uuid>"
}
```
- `amount`：>0 且 ≤1000，字符串

**响应**：同 [8.5 响应 B](#85-post-apipaycreate)（含 `payUrl` / `html`）

**Android 状态**：❌ 未实现

---

### 9.4 POST /api/wallet/recharge/query

查询充值状态。

**请求体**：`{ "tradeNo": "EC..." }`

**响应**：
```json
{
  "status": "completed",
  "bill": { "..." : "..." },
  "traceId": "...",
  "remainingSeconds": 0,
  "message": "..."
}
```
- `status`：`completed` / `pending` / `expired`
- 客户端轮询间隔建议 2.5s

**Android 状态**：❌ 未实现

---

## 10. 流量

### 10.1 GET /api/traffic/history

获取流量使用历史。

**Query 参数**：
| 参数 | 类型 | 默认 | 说明 |
|---|---|---|---|
| `range` | string | `30d` | `30m` / `24h` / `7d` / `30d` |

**响应**：
```json
{
  "range": "7d",
  "granularity": "hour",
  "history": [
    {
      "bucket": "2026-07-26T00:00:00.000Z",
      "label": "07-26 00:00",
      "totalBytes": "1048576",
      "monthlyBytes": "524288",
      "packageBytes": "524288",
      "hitCount": 12
    }
  ]
}
```

**Android 状态**：❌ 未实现

---

### 10.2 GET /api/traffic/packages

获取流量包列表与购买历史。

**响应**：
```json
{
  "totalPurchasedTraffic": "10737418240",
  "packageTrafficRemaining": "5368709120",
  "purchaseCount": 2,
  "history": [
    {
      "id": 1,
      "tradeNo": "EC...",
      "payTime": "2026-07-26T12:00:00.000Z",
      "amount": "10.00",
      "planId": 10,
      "packageName": "10GB 流量包",
      "packageTraffic": "10737418240",
      "remainingTraffic": "5368709120",
      "consumedTraffic": "5368709120",
      "expireTime": "2027-07-26T00:00:00.000Z",
      "daysLeft": 365,
      "sourceType": "order",
      "status": "active"
    }
  ],
  "packagePolicy": "..."
}
```

**Android 状态**：❌ 未实现

---

## 11. 工单

> 全局应用 `authenticateToken` + `userActionRateLimiter`。
> **注意**：管理员（uid=1）只能回复工单，不能提交（返回 403 `ADMIN_REPLY_ONLY`）。

### 11.1 POST /api/workorder

提交工单。

**请求体**：
```json
{
  "title": "无法下载文件",
  "content": "下载时一直 403...",
  "category": "file_download_cdn"
}
```
- `title`：必填，≤120 字
- `content`：必填，≤8192 字，**不允许 HTML 标签**
- `category`：`file_download_cdn`（默认）/ `console` / `feature` / `account_billing` / `other`

**响应**：完整的 `WorkOrder` 对象

**Android 状态**：❌ 未实现

---

### 11.2 GET /api/workorder

获取工单列表。

**响应**：`[WorkOrder, ...]`（管理员 uid=1 看全部）

**Android 状态**：❌ 未实现

---

### 11.3 GET /api/workorder/:id

获取工单详情。

**响应**：
```json
{
  "id": 1,
  "userId": 1,
  "title": "...",
  "content": "...",
  "category": "file_download_cdn",
  "status": "replied",
  "createdAt": "...",
  "User": { "id": 1, "username": "..." },
  "WorkOrderReplies": [
    {
      "id": 1,
      "workOrderId": 1,
      "userId": 1,
      "content": "...",
      "isAdminReply": false,
      "createdAt": "..."
    }
  ]
}
```

**Android 状态**：❌ 未实现

---

### 11.4 POST /api/workorder/:id/reply

回复工单。

**请求体**：
```json
{ "content": "问题仍未解决..." }
```
- `content`：必填，≤8192 字，**允许图片**（HTML `<img>`）

**响应**：`WorkOrderReply` 对象

**Android 状态**：❌ 未实现

---

### 11.5 PATCH /api/workorder/:id/status

更新工单状态（用户只能关闭）。

**请求体**：
```json
{ "status": "closed" }
```

**响应**：完整的 `WorkOrder` 对象

**Android 状态**：❌ 未实现

---

## 12. 公告

> 全局应用 `authenticateToken` + `userActionRateLimiter`。

### 12.1 GET /api/announcements

获取公告列表。

**响应**：
```json
{
  "list": [
    {
      "id": 1,
      "title": "系统维护通知",
      "content": "<p>...</p>",
      "isPublished": true,
      "publishedAt": "2026-07-26T00:00:00.000Z",
      "createdAt": "...",
      "isRead": false
    }
  ],
  "unread": 3
}
```
- `content` 已经过 HTML 消毒（允许 `<img>` 和 `<a target="_blank" rel="noopener">`）

**Android Retrofit**：
```kotlin
@GET("announcements")
suspend fun getAnnouncements(): AnnouncementsResponse

@POST("announcements/{id}/read")
suspend fun markAnnouncementRead(@Path("id") id: Long): Map<String, Any?>
```

**Android 状态**：✅ 已实现

---

### 12.2 POST /api/announcements/:id/read

标记公告已读。

**Path 参数**：`id`（正整数）

**响应**：`{ "id": 1, "isRead": true }`

**Android 状态**：✅ 已实现

---

## 13. 实名认证

### 13.1 GET /api/realname/status

查询实名状态。

**响应**：
```json
{
  "status": "verified",
  "maskedName": "张*三",
  "maskedIdNo": "510***********1234",
  "verifiedAt": "2026-02-01T00:00:00.000Z",
  "lastError": null,
  "canResume": false
}
```
- `status`：`unverified` / `pending` / `verified` / `failed`

**Android 状态**：❌ 未实现

---

### 13.2 POST /api/realname/start

发起实名认证。

| 项 | 值 |
|---|---|
| Captcha | **是**（scene=`realname-start`） |

**请求体**：
```json
{
  "captchaData": { "ticket": "...", "randstr": "..." },
  "captchaPrecheckToken": "<token>"
}
```

**响应**：
```json
{
  "status": "pending",
  "maskedName": null,
  "maskedIdNo": null,
  "verifiedAt": null,
  "lastError": null,
  "canResume": true,
  "verifyUrl": "https://..."
}
```
- 客户端在 WebView 中打开 `verifyUrl`（百度人脸认证）
- 认证完成后百度回调后端，后端更新状态
- 客户端轮询 [13.1](#131-get-apirealnamestatus) 查询结果（建议 5s 间隔）

**Android 状态**：❌ 未实现

---

## 14. Passkey

> WebAuthn 协议，移动端需用 `androidx.credentials`（Credential Manager）或 `com.google.android.gms.fido` 实现。

### 14.1 GET /api/auth/passkey/credentials

获取当前用户的 Passkey 列表。

**响应**：
```json
{
  "credentials": [
    {
      "id": "uuid-xxx",
      "name": "我的 Pixel 7",
      "deviceType": "single_device",
      "backedUp": true,
      "createdAt": "2026-07-26T00:00:00.000Z",
      "lastUsedAt": "2026-07-26T10:00:00.000Z"
    }
  ]
}
```

**Android 状态**：❌ 未实现

---

### 14.2 POST /api/auth/passkey/register/options

获取 Passkey 注册参数。

**请求体**：`{}`

**响应**：
```json
{
  "options": {
    "challenge": "<base64>",
    "rp": { "name": "EdgeCOS", "id": "user.edgecos.com" },
    "user": {
      "id": "<base64>",
      "name": "alice@example.com",
      "displayName": "alice"
    },
    "pubKeyCredParams": [
      { "type": "public-key", "alg": -7 },
      { "type": "public-key", "alg": -257 }
    ],
    "timeout": 60000,
    "excludeCredentials": [...],
    "authenticatorSelection": {
      "authenticatorAttachment": "platform",
      "userVerification": "preferred"
    },
    "attestation": "none"
  },
  "transactionId": "<uuid>"
}
```

→ 客户端用 `androidx.credentials.CreatePublicKeyCredentialRequest(options)` 调起系统 UI
→ 拿到结果后调 [14.3](#143-post-apiauthpasskeyregistverify)

**Android 状态**：❌ 未实现

---

### 14.3 POST /api/auth/passkey/register/verify

验证 Passkey 注册结果。

**请求体**：
```json
{
  "transactionId": "<from 14.2>",
  "response": {
    "id": "<base64url credential id>",
    "rawId": "<base64url>",
    "response": {
      "attestationObject": "<base64url>",
      "clientDataJSON": "<base64url>"
    },
    "type": "public-key"
  },
  "name": "我的 Pixel 7"
}
```

**响应**：`{ "message": "Passkey 注册成功" }`

**Android 状态**：❌ 未实现

---

### 14.4 DELETE /api/auth/passkey/credentials/:id

删除 Passkey。

**响应**：`{ "message": "Passkey 已删除" }`

**Android 状态**：❌ 未实现

---

### 14.5 POST /api/auth/passkey/login/options

获取 Passkey 登录参数（**无需登录**，限流 `auth` + `login`）。

**请求体**：
```json
{ "username": "alice" }
```
- `username` 可选（用于 resident key 场景的精确匹配）

**响应**：同 [14.2](#142-post-apiauthpasskeyregisteroptions)，但 `userVerification` 通常为 `required`

**Android 状态**：❌ 未实现

---

### 14.6 POST /api/auth/passkey/login/verify

验证 Passkey 登录结果。

**请求体**：
```json
{
  "transactionId": "<from 14.5>",
  "response": {
    "id": "...",
    "rawId": "...",
    "response": {
      "authenticatorData": "<base64url>",
      "clientDataJSON": "<base64url>",
      "signature": "<base64url>",
      "userHandle": "<base64url>"
    },
    "type": "public-key"
  }
}
```

**响应**：同 [1.1 POST /api/login](#11-post-apilogin) 响应（含 `Set-Cookie: token`）

**Android 状态**：❌ 未实现

---

## 15. 第三方 OAuth 登录

### 15.1 QQ 互联（旧版）

#### GET /api/auth/qq/login

跳转到 QQ 授权页。

**Query 参数**：`redirect`（可选，安全路径）

**响应**：302 重定向到 QQ 授权 URL

#### GET /api/auth/qq/bind

绑定 QQ（需登录）。

#### GET /api/auth/qq/callback

QQ 回调。

**Query 参数**：`code`、`state`

**响应**：302 重定向到前端 `/login?qq_oauth=success&redirect=...`

**Android 状态**：❌ 未实现

---

### 15.2 七相聚合 OAuth（szczk，推荐）

支持 8 个 provider：`qq` / `wx` / `alipay` / `dingtalk` / `douyin` / `gitee` / `github` / `microsoft`

#### GET /api/auth/szczk/:provider/login

跳转授权。

**响应**：302 重定向到 szczk 授权 URL

#### POST /api/auth/szczk/:provider/login-session

获取扫码登录会话（仅 `wx` provider）。

**请求体**：`{ "redirect": "/profile" }`

**响应**：
```json
{
  "state": "<uuid>",
  "url": "https://open.weixin.qq.com/...",
  "qrcode": "https://...",
  "expiresAt": "2026-07-26T00:05:00.000Z"
}
```

#### POST /api/auth/szczk/wx/login-session

同上（专用于微信）。

#### POST /api/auth/szczk/:provider/bind-session

获取绑定扫码会话（需登录）。

#### GET /api/auth/szczk/qr/:state/status

查询扫码状态（**无需登录**，限流严格，建议 2s 轮询）。

**响应**：
```json
{
  "status": "success",
  "mode": "login",
  "provider": "wx",
  "message": "...",
  "expiresAt": "..."
}
```
- `status`：`pending` / `success` / `not_bound` / `failed` / `expired`

#### POST /api/auth/szczk/qr/:state/complete

完成扫码登录/绑定。

**响应（login 模式）**：
```json
{
  "message": "success",
  "redirect": "/",
  "mode": "login"
}
```
+ `Set-Cookie: token`

**响应（bind 模式）**：`{ "message": "success", "redirect": "/profile", "mode": "bind" }`

#### DELETE /api/auth/szczk/:provider/bind

解绑第三方账号。

**响应**：`{ "message": "OAuth account unbound successfully" }`

#### GET /api/auth/szczk/callback/:state 或 /api/auth/szczk/callback

OAuth 回调，302 重定向到前端。

**Android 状态**：❌ 未实现（建议用 CustomTabs 打开授权 URL，监听 deep link 回调）

---

## 16. 离线下载

> 限流 `offlineSubmit` 10s/10次/userId+IP

### 16.1 POST /api/offline/submit

提交离线下载任务。

**请求体**：
```json
{
  "url": "https://example.com/file.zip",
  "targetPath": "/downloads"
}
```
- `url`：必填，支持 `http(s)://` 和 `magnet:` 协议
- `targetPath`：必填，合法路径

**SSRF 防护**：拒绝 `localhost` / `.local` / `.internal` / 私网 IP / CDN 自身域名

**响应**：
```json
{
  "message": "Task submitted successfully",
  "task": {
    "id": "uuid-xxx",
    "userId": 1,
    "url": "...",
    "targetPath": "/downloads",
    "status": "pending",
    "size": null,
    "errorMsg": null,
    "createdAt": "..."
  }
}
```

**Android 状态**：❌ 未实现

---

### 16.2 GET /api/offline/history

获取离线任务列表（自动同步 pending 任务状态）。

**响应**：`[task, ...]`（最多 50 条）

**Android 状态**：❌ 未实现

---

### 16.3 DELETE /api/offline/tasks/:id

删除离线任务。

**Path 参数**：`id`（UUID）

**响应**：`{ "message": "任务已删除" }`

**Android 状态**：❌ 未实现

---

## 17. 安全（违规举报）

### 17.1 POST /api/safety/report

提交违规举报（**无需登录**，需 Captcha）。

**请求体**：
```json
{
  "targetType": "share",
  "reason": "porn",
  "detail": "包含色情内容...",
  "shareCode": "abc1234567",
  "captchaData": { "ticket": "...", "randstr": "..." },
  "captchaPrecheckToken": "<token>"
}
```
- `targetType`：`share` / `direct_link` / `file`
- `reason`：`porn` / `violence` / `fraud` / `malware` / `copyright` / `other`
- `detail`：≤2000 字
- `shareCode` / `directLinkCode` / `fileNodeId`：根据 `targetType` 提供对应标识

**响应**：
```json
{
  "message": "举报已提交",
  "reportId": 1,
  "reportCountIn1h": 2
}
```

**自动冻结**：1 小时内同目标 ≥3 条 pending/processing 举报 → 自动下线

**Android 状态**：❌ 未实现

---

### 17.2 GET /api/safety/reports 等（管理员）

> ⚠️ 生产环境已下线，返回 403「生产环境已下线该管理功能，请使用 Admin 控制台处理」

包括：`GET /safety/overview`、`GET /safety/reports`、`GET /safety/files/review`、`POST /safety/files/:id/action`、`POST /safety/reports/:id/resolve`

**Android 状态**：❌ 不实现（已下线）

---

## 18. 管理员公告

> 需 `isRootAdmin`（uid=1 + role=admin），其他用户返回 403 `ROOT_ADMIN_ONLY`。

### 18.1 GET /api/admin/announcements

**响应**：`{ "list": [Announcement, ...] }`

### 18.2 POST /api/admin/announcements

**请求体**：
```json
{
  "title": "公告标题",
  "content": "<p>公告内容，支持 HTML</p>"
}
```
- `title` ≤160
- `content` ≤10000

**响应**：`Announcement` 对象（201）

### 18.3 PATCH /api/admin/announcements/:id

**请求体**：`{ "title": "...", "content": "...", "isPublished": true }`

### 18.4 DELETE /api/admin/announcements/:id

**响应**：`{ "deleted": true }`

**Android 状态**：❌ 不实现（管理员用 Web 端）

---

## 附录 A. 环境变量速查

> 仅列变量名，不含真实值。客户端开发者只需关心前 4 项。

### A.1 客户端必需
| 变量名 | 用途 |
|---|---|
| `BASE_URL` | API 基地址（`https://user.edgecos.com/api/`） |
| `WEB_BASE_URL` | Web 端基地址（用于法律协议页等） |
| `TENCENT_CAPTCHA_APP_ID` | 腾讯 Captcha AppID（`197733334`） |
| `DIRECT_LINK_BASE_URL` | 付费直链域名（`https://edgecos.com`） |
| `FREE_DIRECT_LINK_BASE_URL` | 免费直链域名（`https://dl2.edgecos.com`） |

### A.2 后端配置（仅参考）
| 类别 | 变量 |
|---|---|
| 应用 | `NODE_ENV` `TRUST_PROXY` `CORS_ORIGIN` `APP_BASE_URL` `FRONTEND_URL` |
| 鉴权 | `AUTH_SECRET_KEY` `AUTH_COOKIE_DAYS` `AUTH_COOKIE_SECURE` `AUTH_COOKIE_SAME_SITE` `PASSKEY_RP_ID` `PASSKEY_ORIGINS` |
| 数据库 | `DB_HOST` `DB_PORT` `DB_NAME` `DB_USER` `DB_PASSWORD` |
| OSS | `OSS_REGION` `OSS_ENDPOINT` `OSS_ACCESS_KEY_ID` `OSS_ACCESS_KEY_SECRET` `OSS_BUCKET` |
| CDN | `KSC_CDN_URL` `KSC_CDN_PRIVATE_KEY` `CDN_URL` `CDN_PRIVATE_KEY` `TENCENT_EO_FREE_CDN_URL` `TENCENT_EO_FREE_CDN_PRIVATE_KEY` |
| Captcha | `TENCENTCLOUD_SECRET_ID` `TENCENTCLOUD_SECRET_KEY` `TENCENT_CAPTCHA_APP_ID` `TENCENT_CAPTCHA_APP_SECRET_KEY` |
| SMTP | `SMTP_HOST` `SMTP_PORT` `SMTP_USER` `SMTP_PASS` `SMTP_FROM` |
| 支付 | `YIPAY_PID` `YIPAY_KEY` `YIPAY_NOTIFY_URL` `YIPAY_RETURN_URL` |
| OAuth | `SZCZK_OAUTH_APPID` `SZCZK_OAUTH_APPKEY` `SZCZK_OAUTH_REDIRECT_URI` `QQ_CONNECT_APP_ID` `QQ_CONNECT_APP_KEY` |
| 实名 | `BAIDU_FACE_API_KEY` `BAIDU_FACE_SECRET_KEY` `BAIDU_FACE_PLAN_ID` |
| 离线 | `OFFLINE_CALLBACK_BASE_URL` `OFFLINE_CALLBACK_SECRET` `OFFLINE_ENDPOINTS` |

---

## 附录 B. 客户端实现状态总览

> 截至 2026-07-26，Android 客户端已实现 28 个端点（占 ~35%），主要集中在 MVP 阶段。

### B.1 ✅ 已实现（28 个）

| 模块 | 端点 |
|---|---|
| 鉴权与注册 | `POST /login` `POST /register` `POST /send-email-code` `POST /captcha/precheck` |
| 用户中心 | `GET /user/quota`（`GET /user/profile` 已声明未用） |
| 文件管理 | `GET /files` `GET /files/summary` `GET /trash` `POST /mkdir` `POST /rename` `POST /move` `POST /copy` `POST /delete` `POST /batch/move` `POST /batch/copy` `POST /batch/delete` `POST /batch/trash/restore` `POST /batch/trash/delete` |
| 上传 | `GET /upload/conflict` `GET /upload/token` `POST /upload/complete` `POST /upload/instant-complete` |
| 下载 | `GET /download`（用 `Downloader` 直拼 URL） |
| 公告 | `GET /announcements` `POST /announcements/:id/read` |

### B.2 ⚠️ 已声明但实际未调用（死代码）

- `EdgeApi.download()`（被 `Downloader` 替代）
- `EdgeApi.restoreTrash()` / `deleteTrash()`（被批量接口替代）
- `EdgeApi.getUserProfile()`（未接入）

### B.3 ❌ 待实现（按优先级）

**P0（核心功能）**
1. 分片上传 5 个接口（`/upload/multipart/*`）
2. 分享 5 个接口
3. 套餐 `/my-plan` `/plans`
4. 钱包 4 个接口

**P1（增强功能）**
5. 直链 `/generate-direct-link` `/f/:linkCode`
6. 预览 `/preview` `/preview/pdf-url` `/thumbnail`
7. 文本编辑 `/file/content` (GET + PUT)
8. 流量 `/traffic/history` `/traffic/packages`
9. 工单 5 个接口
10. 订单 `/orders` `/orders/:tradeNo`
11. 支付 4 个接口
12. 实名 2 个接口

**P2（可选功能）**
13. Passkey 6 个接口
14. OAuth 第三方登录（QQ + szczk 8 个 provider）
15. 离线下载 3 个接口
16. 缩略图上传 2 个接口
17. 安全举报
18. 个人资料编辑（用户名/密码/邮箱绑定）

---

## 附录 C. 关键文件路径速查

### 后端
- 入口：`EdgeCOS/web/Backend-master/index.js`
- 路由：`EdgeCOS/web/Backend-master/routes/*.js`
- 中间件：`EdgeCOS/web/Backend-master/middleware/{auth,captcha,rateLimit}.js`
- 模型：`EdgeCOS/web/Backend-master/models/index.js`

### 前端
- API 基础设施：`EdgeCOS/web/Frontend-main/src/lib/api.js`
- 业务工具：`EdgeCOS/web/Frontend-main/src/lib/{billing,format,tencentCaptcha}.js`
- 页面：`EdgeCOS/web/Frontend-main/src/pages/*.jsx`

### Android
- Retrofit 接口：`EdgeCOS/android/data/src/main/java/com/edgecos/mobileapp/data/remote/EdgeApi.kt`
- DTO：`EdgeCOS/android/data/src/main/java/com/edgecos/mobileapp/data/remote/dto/Dtos.kt`
- OkHttp 配置：`EdgeCOS/android/data/src/main/java/com/edgecos/mobileapp/data/remote/{EdgeHttpClient,RawHttpClient,ClientConfig}.kt`
- CookieJar：`EdgeCOS/android/data/src/main/java/com/edgecos/mobileapp/data/remote/EncryptedCookieJar.kt`
- 拦截器：`EdgeCOS/android/data/src/main/java/com/edgecos/mobileapp/data/remote/{AuthInterceptor,ResponseInterceptor}.kt`
- Repository：`EdgeCOS/android/data/src/main/java/com/edgecos/mobileapp/data/repository/*.kt`
- 鉴权：`EdgeCOS/android/data/src/main/java/com/edgecos/mobileapp/data/auth/{AuthWatcher,AuthEventBus,AuthRepositoryHolder}.kt`
- Domain 模型：`EdgeCOS/android/domain/src/main/java/com/edgecos/mobileapp/domain/model/Models.kt`
- 错误模型：`EdgeCOS/android/domain/src/main/java/com/edgecos/mobileapp/domain/error/{EdgeError,EdgeResult}.kt`
- 上传器：`EdgeCOS/android/upload/src/main/java/com/edgecos/mobileapp/upload/{Uploader,Downloader,UploadService,Sha256Calculator}.kt`
- Application：`EdgeCOS/android/app/src/main/java/com/edgecos/mobileapp/EdgeCOSApp.kt`
- 根导航：`EdgeCOS/android/app/src/main/java/com/edgecos/mobileapp/RootNav.kt`
- BASE_URL 配置：`EdgeCOS/android/app/build.gradle.kts`
