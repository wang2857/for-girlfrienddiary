# 存档记录

存档时间：**2026-11-08 19:33**

版本：**1.0.4-debug**（versionCode 76）｜ 数据库 **v9（SQLCipher + FTS5）** ｜ 纯 64 位（arm64-v8a）
｜ 含应用锁 / 恢复码 / 自动备份 / 跨设备备份 / 崩溃日志 / 正文字体

> 1.0 = 第一个"可以天天用"的存档。只保留调试版：正式包（`com.synn.diary`）未构建、未安装。

## 产物

| 文件 | 体积 | SHA-256 |
|---|---|---|
| `dist/日记-1.0.4-debug.apk` | 21 MB | `4e09baa75a29b86eced11913c8ec9f44f3ca84c02ccc196fb0088e0b5f32c184` |
| `dist/日记-1.0.4-源码.zip` | 6.5 MB | `b79eafe5e4183704ff2d5c2c8ab53d4f54307a24e88eaa037b1168c13ee4e32e` |

包名 `com.synn.diary.debug`，用 `keystore/debug.keystore` 签名，`install -r` 覆盖安装可保住数据。

## 代码状态

源码快照（**不含 `.git`**，tag 与分支历史留在工作目录的仓库里，当前 `v1.0.4`）：

- `app/` 源码 + `app/schemas/`（Room 导出的 schema，当前 `9.json`）
- `docs/` 界面截图 + `plan.md`（产品方案）+ `database.md`（表结构、单位约定、FTS、备份格式）
- `keystore/debug.keystore`（签名，随仓库保留以便继续覆盖升级）
- `CHANGELOG.md`（0.1 → 1.0.4 的完整改动）

分支：

- `main`：稳定线，1.0.0 在这里
- `beautify`：美化分支（从 v1.0.0 拉出），改视觉/动效，验证满意再合回 `main`

## 如何重新构建

```bash
cd diary
./build.sh assembleDebug     # 日常只用这个
./build.sh check64Bit        # 64 位自检
./build.sh :app:lintDebug    # 静态检查（当前 0 Error）
```

正式包按需：`./build.sh assembleRelease`（默认不做）。

工具链在仓库同级 `.toolchain/`（不进版本库）：Corretto 21 / Android SDK 36 / Gradle 8.14.3。
依赖已全量缓存，可离线构建。

## 数据与备份

三条路，边界各不相同：

1. **手动导出**（设置 → 数据 → 导出备份）：zip = 加密库 + `images/` + `backgrounds/` + 恢复码封的 `key.bin`
2. **自动备份**：每天 3:00 往 `files/backup/` 存一份，留 7 份；只在本机
3. **换机恢复**：在新设备导入时输入恢复码 → 解出备份库口令 → `PRAGMA rekey` 换成本机口令

库文件在 `databases/diary.db`（SQLCipher 密文），图片 `files/images/`、自定义纸张 `files/backgrounds/`、
导入的字体 `files/fonts/`、崩溃日志 `files/crash/`。

## 安全模型

- 应用锁：4 位 PIN（PBKDF2-HmacSHA256，12 万次 + 16 字节盐），只存哈希
- 恢复码：16 位、只存哈希 + Keystore 封装的明文副本；可查看、可重设
- 数据库：SQLCipher，口令随机 32 字节、Keystore AES-256-GCM 包裹，与 PIN 无关
- 指纹走 `androidx.biometric`；默认 `FLAG_SECURE`
- 切后台先盖不透明挡板、`onStop` 再上锁，回到前台不会闪出正文
- 备份包里的库是密文；跨设备靠恢复码封的口令（`key.bin`）

## 已知欠账

见 `README.md` §6。单测只覆盖纯逻辑（33 个用例），数据库/备份/UI 仍靠手测；Markdown 只存不渲染。
