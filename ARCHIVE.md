# 存档记录

存档时间：**2026-11-08 12:49**

版本：**0.25.0**（versionCode 49）｜ 数据库 **v6** ｜ 纯 64 位（arm64-v8a）

## 产物

| 文件 | 体积 | SHA-256 |
|---|---|---|
| `dist/日记-0.25.0-debug.apk` | 13.2 MB | `64b525b680e61e61ffe974e3f93e10b2e9cb2b76c61b598db8965e7ac9543ec9` |
| `dist/日记-0.25.0-release.apk` | 9.1 MB | `d31be0af6322d888bf5013c24844e2ea4a25b92a2e531f4231e3c44637c4a358` |
| `dist/日记-0.25.0-源码.zip` | 2.6 MB | `61575d47893d912a4f45b83b4dd81ded4a0f5feda4e9ee55ef6d97ba239c289a` |

两个 APK 都用 `keystore/debug.keystore` 签名，包名不同、数据互不相通：

- `日记-0.25.0-release.apk` → `com.synn.diary`（正式版）
- `日记-0.25.0-debug.apk` → `com.synn.diary.debug`（调试版，日常在用的那个）

## 代码状态

本目录是完整源码快照（含 git 仓库，tag `v0.25.0`）：

- `app/` 源码 + `app/schemas/`（Room 导出的 6 个版本 schema）
- `docs/` 界面截图 + `plan.md`（产品方案原始文档）
- `keystore/debug.keystore`（签名，随仓库保留以便继续覆盖升级）
- `CHANGELOG.md`（0.1 → 0.25.0 的完整改动）

## 如何重新构建

```bash
cd diary
./build.sh assembleRelease   # app/build/outputs/apk/release/app-release.apk
./build.sh check64Bit        # 64 位自检
```

工具链在仓库同级 `.toolchain/`（不进版本库）：Corretto 21 / Android SDK 36 / Gradle 8.14.3。
依赖已全量缓存，可离线构建。

## 数据与备份

**现在有备份了**：设置 → 数据 → 导出备份，得到含 `diary.db` 和全部图片的 zip。
换机 / 重置前导出一次即可；导入会覆盖本机数据并自动重启。

真机上两个包：

- `com.synn.diary.debug` — 日常在用的那个
- `com.synn.diary` — 正式版

库文件在 `/data/data/<包名>/databases/diary.db`，图片在 `files/images/`。
手动取库要拿全 `diary.db` + `-wal` + `-shm`。
