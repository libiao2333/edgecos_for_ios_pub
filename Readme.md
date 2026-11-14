
### 签名配置

Release 构建需要签名密钥。签名文件  **不随仓库分发**（防止泄露）。

### 第三方依赖说明

- 仓库通过阿里云镜像加速依赖下载（见 `settings.gradle.kts` / `gradle.properties`）
- AndroidDocViewer 为外部模块，位于仓库外的 `../AndroidDocViewer-main/docviewer`（由 `settings.gradle.kts` 引入）。

## 配置

主要配置项集中在 `app/build.gradle.kts`：

| 配置项 | 值 | 说明 |
|---|---|---|
| `BASE_URL` | `https://user.edgecos.com/api/` | 后端 API 地址（需以 `/` 结尾） |
| `WEB_BASE_URL` | `https://user.edgecos.com` | Web 端地址 |
| `TENCENT_CAPTCHA_APP_ID` | `197733334` | 腾讯云验证码 AppId（与前端一致） |

网络与安全配置见 `app/src/main/res/xml/network_security_config.xml`、`app/src/main/AndroidManifest.xml`。

## 相关文档

- [API_REFERENCE.md](API_REFERENCE.md) — 后端 API 接口参考

