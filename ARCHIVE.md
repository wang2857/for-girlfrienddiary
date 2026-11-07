# 存档记录

存档时间：**2026-11-08 02:39**

版本：**0.24.4**（versionCode 47）｜ 数据库 **v6** ｜ 纯 64 位（arm64-v8a）

## 产物

| 文件 | 体积 | SHA-256 |
|---|---|---|
| `dist/日记-0.24.4-debug.apk` | 13.1 MB | `1233381fa54fd959e65e304ec40defe50e682b6a1bfee842c843627d5fd7f47d` |
| `dist/日记-0.24.4-release.apk` | 9.1 MB | `9464555d4a4586176fa63a03b174bbcef8595341d99c5cc4ce08cfa14af981c9` |

两个包都用 `keystore/debug.keystore` 签名，包名不同、数据互不相通：

- `日记-0.24.4-release.apk` → `com.synn.diary`（正式版）
- `日记-0.24.4-debug.apk` → `com.synn.diary.debug`（调试版，日常在用的那个）

## 代码状态

本目录是完整源码快照，含：

- `app/` 源码 + `app/schemas/`（Room 导出的 6 个版本 schema）
- `docs/` 20 张界面截图 + `plan.md`（产品方案原始文档）
- `keystore/debug.keystore`（签名，随仓库保留以便继续覆盖升级）
- `CHANGELOG.md`（从 0.1 到 0.24.4 的完整改动）

## 如何重新构建

```bash
cd diary
./build.sh assembleRelease   # 产物 app/build/outputs/apk/release/app-release.apk
./build.sh check64Bit        # 64 位自检
```

工具链在仓库同级 `.toolchain/`（不进版本库）：Corretto 21 / Android SDK 36 / Gradle 8.14.3。
依赖已全量缓存，可离线构建。

## 真机上的数据


手机（Redmi 22081212C / Android 15）：

- `com.synn.diary.debug` — 15 条日记 + 1.3 MB 图片（日常在用的）
- `com.synn.diary` — 正式版，空库（按选择未迁移）

库文件在 `/data/data/<包名>/databases/diary.db`，图片在 `files/images/`。
两级都要拿全：`diary.db` + `diary.db-wal` + `diary.db-shm`。
