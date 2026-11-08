# 存档记录

存档时间：**2026-11-08 13:14**

版本：**0.26.0-debug**（versionCode 50）｜ 数据库 **v6** ｜ 纯 64 位（arm64-v8a）｜ 含每日提醒

> 只保留调试版：正式包（`com.synn.diary`）未构建、未安装，需要时再加。

## 产物

| 文件 | 体积 | SHA-256 |
|---|---|---|
| `dist/日记-0.26.0-debug.apk` | 13.5 MB | `d188a03ea40020a426303654d832351465feb8da3095e6c4dd193d7c52ba9b91` |
| `dist/日记-0.26.0-源码.zip` | 2.6 MB | `146e3071ef0d6aeb2965cca6db117e146cadfcb6dc0cc3e8b2fd4afd751fe839` |

包名 `com.synn.diary.debug`，用 `keystore/debug.keystore` 签名，`install -r` 覆盖安装可保住数据。

## 代码状态

完整源码快照（含 git 仓库，tag `v0.26.0`）：

- `app/` 源码 + `app/schemas/`（Room 导出的 6 个版本 schema）
- `docs/` 界面截图 + `plan.md`（产品方案原始文档）
- `keystore/debug.keystore`（签名，随仓库保留以便继续覆盖升级）
- `CHANGELOG.md`（0.1 → 0.26.0 的完整改动）

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

**有备份功能**：设置 → 数据 → 导出备份，得到含 `diary.db` 和全部图片的 zip。
换机 / 重置前导出一次即可；导入会覆盖本机数据并自动重启。

手机上只有 `com.synn.diary.debug` 一个包。
库文件在 `/data/data/com.synn.diary.debug/databases/diary.db`，图片在 `files/images/`；
手动取库要拿全 `diary.db` + `-wal` + `-shm`。

## 每日提醒的实现要点

- WorkManager 每 24h 一次，首次延迟对齐到下一个 21:00（`PeriodicWorkRequest` + `initialDelay`）
- 渠道 `daily_reminder`，`IMPORTANCE_DEFAULT`；标准 `BigTextStyle`；`ongoing=false`；固定 id 1001 不堆叠
- 当天已有非空日记则不发（`countOnDay`）
- 系统里通知权限 / 渠道被关时不发，也不报错
- 点击通知 → 带 `EXTRA_NEW_ENTRY` 打开 App，解锁后自动新建一篇
