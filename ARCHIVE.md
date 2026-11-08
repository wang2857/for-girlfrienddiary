# 存档记录

存档时间：**2026-11-08 16:22**

版本：**0.30.0-debug**（versionCode 56）｜ 数据库 **v6** ｜ 纯 64 位（arm64-v8a）
｜ 含每日提醒、恢复码、自动备份、崩溃日志

> 只保留调试版：正式包（`com.synn.diary`）未构建、未安装，需要时再加。

## 产物

| 文件 | 体积 | SHA-256 |
|---|---|---|
| `dist/日记-0.30.0-debug.apk` | 15.7 MB | `7c68a928902632763cb46393e1ec287d50185ddc8fa150159676ea66c561dc75` |
| `dist/日记-0.30.0-源码.zip` | 4.9 MB | `b1ecd909cfdc867360df0e4e795b05bed56d90d6e56455325f8295b983c65851` |

包名 `com.synn.diary.debug`，用 `keystore/debug.keystore` 签名，`install -r` 覆盖安装可保住数据。

## 代码状态

源码快照（**不含 `.git`**，tag 历史留在工作目录的仓库里，当前 `v0.30.0`）：

- `app/` 源码 + `app/schemas/`（Room 导出的 6 个版本 schema）
- `docs/` 界面截图 + `plan.md`（产品方案原始文档）
- `keystore/debug.keystore`（签名，随仓库保留以便继续覆盖升级）
- `CHANGELOG.md`（0.1 → 0.30.0 的完整改动）

## 如何重新构建

```bash
cd diary
./build.sh assembleDebug     # 日常只用这个
./build.sh check64Bit        # 64 位自检
```

正式包按需：`./build.sh assembleRelease`（默认不做）。

工具链在仓库同级 `.toolchain/`（不进版本库）：Corretto 21 / Android SDK 36 / Gradle 8.14.3。
依赖已全量缓存，可离线构建。

## 数据与备份

三条路，各有各的边界：

1. **手动导出**（设置 → 数据 → 导出备份）：zip 存到手机任意位置，**唯一能扛卸载 / 换机的路**
2. **自动备份**（设置 → 数据 → 自动备份）：每天 3:00 往 `files/backup/` 存一份，留 7 份，
   防误删 / 写坏 / 导入覆盖；卸载或换机会跟着一起没
3. **忘记密码**：锁屏「忘记密码？」+ 引导时抄下的 16 位恢复码 → 直接设新密码，日记不丢

手机上只有 `com.synn.diary.debug` 一个包。
库文件在 `/data/data/com.synn.diary.debug/databases/diary.db`，图片在 `files/images/`，
自动备份在 `files/backup/`，崩溃日志在 `files/crash/`；
手动取库要拿全 `diary.db` + `-wal` + `-shm`。

## 每日提醒的实现要点

- WorkManager 每 24h 一次，首次延迟对齐到下一个 21:00（`PeriodicWorkRequest` + `initialDelay`）
- 渠道 `daily_reminder`，`IMPORTANCE_DEFAULT`；标准 `BigTextStyle`；`ongoing=false`；固定 id 1001 不堆叠
- 当天已有非空日记则不发（`countOnDay`）
- 系统里通知权限 / 渠道被关时不发，也不报错
- 点击通知 → 带 `EXTRA_NEW_ENTRY` 打开 App，解锁后自动新建一篇

## 安全模型（0.30.0）

- 应用锁：4 位 PIN → PBKDF2-HmacSHA256，12 万次迭代 + 16 字节盐，只存哈希
- 恢复码：16 位（32 字母表，去 0/O/1/I/L），同样只存哈希；明文只在生成那一刻显示
- 指纹走 `androidx.biometric`；默认 `FLAG_SECURE` 禁截屏
- **数据库本身仍是明文 SQLite**——应用锁挡的是界面，不是文件
