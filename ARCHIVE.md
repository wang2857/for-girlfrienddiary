# 存档记录

存档时间：**2026-11-08 13:03**

版本：**0.25.0-debug**（versionCode 49）｜ 数据库 **v6** ｜ 纯 64 位（arm64-v8a）

> 只保留调试版：正式包（`com.synn.diary`）已从手机卸载、产物已删除，需要时再单独构建。

## 产物

| 文件 | 体积 | SHA-256 |
|---|---|---|
| `dist/日记-0.25.0-debug.apk` | 13.2 MB | `64b525b680e61e61ffe974e3f93e10b2e9cb2b76c61b598db8965e7ac9543ec9` |
| `dist/日记-0.25.0-源码.zip` | 2.6 MB | `18f92ce03203b65466191acd9b6356006b50193e8b3e26414d347b3bdbe833ce` |

`日记-0.25.0-debug.apk` 包名 `com.synn.diary` + `.debug` 后缀 → `com.synn.diary.debug`，
用 `keystore/debug.keystore` 签名，`install -r` 覆盖安装可保住数据。

## 代码状态

本目录是完整源码快照（含 git 仓库，tag `v0.25.0`）：

- `app/` 源码 + `app/schemas/`（Room 导出的 6 个版本 schema）
- `docs/` 界面截图 + `plan.md`（产品方案原始文档）
- `keystore/debug.keystore`（签名，随仓库保留以便继续覆盖升级）
- `CHANGELOG.md`（0.1 → 0.25.0 的完整改动）

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
