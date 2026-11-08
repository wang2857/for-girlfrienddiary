# 存档记录

存档时间：**2026-11-08 19:30**

版本：**1.0.4（首个正式签名版）** ｜ versionCode **76** ｜ 数据库 **v9（SQLCipher + FTS5）** ｜ 纯 64 位（arm64-v8a）

> 调试包 `com.synn.diary.debug` 与正式包 `com.synn.diary` 是两个独立应用，可同时安装，数据不互通。

## 产物

| 文件 | 体积 | SHA-256 |
|---|---|---|
| `dist/日记-1.0.4-release.apk` | 17 MB | `fccb1d8c12bbf2637dac4a1ad66f0300033450b904ac6a39c2106d7457fbb00f` |
| `dist/日记-1.0.4-debug.apk` | 21 MB | `4e09baa75a29b86eced11913c8ec9f44f3ca84c02ccc196fb0088e0b5f32c184` |
| `dist/日记-1.0.4-源码.zip` | 6.4 MB | `c34b47afc53a96f3e6da18e92128c9a9fcda39f40fc3473e0f5022ddebd33f2c` |

> 源码 zip **包含 `keystore/`（正式签名材料）**，只做本地存档，不要外传。

## 正式签名

| 项 | 值 |
|---|---|
| keystore | `keystore/release.keystore`（PKCS12，RSA 4096，有效期 10000 天） |
| 密码 | `keystore/release.properties`（`storePassword` / `keyPassword`，与 keystore 同目录） |
| alias | `diary-release` |
| 证书 DN | `CN=Synn Diary, OU=Personal, O=Synn, L=Shanghai, ST=Shanghai, C=CN` |
| 证书 SHA-256 | `93:D2:B5:2B:A9:D5:DA:D9:47:C9:08:37:B3:D4:7D:D7:B2:DE:F9:7C:C4:28:26:14:02:E6:09:F0:36:F0:99:9C` |
| 证书 SHA-1 | `85:C0:43:2D:2D:0A:12:70:33:FF:BC:6A:F7:FF:BD:B5:73:D5:8E:6C` |

> ⚠️ **keystore + 密码是升级的唯一凭据**。丢了，已装的正式版无法覆盖升级，只能卸载重装（数据全删）。
> 请把这两个文件单独备份到仓库之外的地方。

## 安装

```bash
adb install -r dist/日记-1.0.4-release.apk   # 正式版；同一个 key 才能 -r 升级
adb install -r dist/日记-1.0.4-debug.apk     # 调试版，可共存
```

`allowBackup=false` + `dataExtractionRules` 全排除，**卸载重装会清空数据**。

## 代码状态

- 仓库 tag：`v1.0.4`（main）；`beautify` 从它拉出，改视觉/动效，验证后合回 main
- 源码快照**不含 `.git`**：`app/` + `app/schemas/`（Room 导出，当前 `9.json`）+ `docs/` + `keystore/` + `dist/`
- 测试：JVM 37 个用例、Robolectric 4 个、仪器测试 4 个（`./build.sh :app:testDebugUnitTest` / `:app:connectedDebugAndroidTest`）
- CI：`.github/workflows/ci.yml`（单测 → lint → 编译仪器测试 → 构建 + 64 位自检）；仓库无远端，推上去才跑

## 如何重新构建

```bash
cd diary
./build.sh assembleDebug     # 日常调试包
./build.sh assembleRelease   # 正式包（需要 keystore/release.properties）
./build.sh :app:testDebugUnitTest
./build.sh :app:lintDebug    # 当前 0 Error
./build.sh check64Bit        # 64 位自检
```

工具链在同级 `.toolchain/`（不进版本库）：Corretto 21 / SDK 36 / Gradle 8.14.3。

## 数据与备份

1. **手动导出**：设置 → 数据 → 导出备份 → zip（加密库 + 图片 + 自定义纸张 + 导入的字体 + 恢复码封的 `key.bin`）
2. **自动备份**：每天 3:00 存 `files/backup/`，留 7 份，只在本机
3. **换机 / 跨包恢复**：导入时输入恢复码 → 解出备份库口令 → `PRAGMA rekey` 换成本机口令。
   调试版 ↔ 正式版之间搬数据也走这条路（普通"设备备份"因 Keystore 不同解不开）

## 安全模型

- 应用锁：4 位 PIN（PBKDF2-HmacSHA256，12 万次 + 16 字节盐），只存哈希
- 恢复码：16 位，只存哈希 + Keystore 封装的明文副本；可查看、可重设
- 数据库：SQLCipher，口令随机 32 字节、Keystore AES-256-GCM 包裹，与 PIN 无关
- 指纹走 `androidx.biometric`；默认 `FLAG_SECURE`
- 切后台先盖挡板、`onStop` 再上锁
- 正式包用独立 RSA 4096 签名，与调试包互不覆盖

## 已知欠账

见 `README.md` §6：单测只覆盖纯逻辑与数据库基础路径；UI 流程靠手测；Markdown 只存不渲染；
提醒/自动备份时间不可调；没有云同步。
