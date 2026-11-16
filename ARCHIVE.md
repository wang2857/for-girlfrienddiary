# 存档记录

最近一次归档：**2026-11-16**（第 2 批）｜ 版本：**1.1.0（手绘改版）** ｜ versionCode **77** ｜ 数据库 **v9（SQLCipher + FTS5）** ｜ 纯 64 位（arm64-v8a）

> 1.1.0 = 1.0.4 之后在 `beautify` 分支上做的一整套视觉重做，已合回 `main`。
> 正式包用 `keystore/release.keystore`（RSA 4096）签名，包名 `com.synn.diary`，和调试包可共存。

## 归档批次

| 批次 | 日期 | 版本 | tag | 形态 |
|---|---|---|---|---|
| 1 | 2026-11-09 | 1.1.0 | `v1.1.0` | 源码包入库；两个 APK 只在 `dist/`（已 gitignore） |
| 2 | 2026-11-16 | 1.1.0 | `v1.1.0-archive` | **APK + 源码包全部入库**，仓库自带可安装产物 |

从第 2 批起仓库是自包含的：clone 出来就有源码、就有能直接 `adb install` 的包，不再依赖某台机器的
`dist/` 目录。代价是 git 历史变重（每个版本约 +40 MB）。

## 产物（`dist/`）

| 文件 | 体积 | 说明 |
|---|---|---|
| `dist/日记-1.1.0-release.apk` | 17.2 MiB | 正式签名，`com.synn.diary`，装她手机用这个 |
| `dist/日记-1.1.0-debug.apk` | 21.5 MiB | 调试签名，`com.synn.diary.debug`，日常开发用 |
| `dist/日记-1.1.0-源码.zip` | 约 6.5 MB | 源码快照，不含 `.git` / `build/` / `dist/` |

调试包 `com.synn.diary.debug`，正式包 `com.synn.diary`；两个是不同的应用，各自 `install -r`
覆盖安装保数据，但**数据互不相通**（要搬数据走恢复码导出/导入）。

## 校验

```bash
cd diary
shasum -a 256 -c dist/SHA256SUMS     # 全部产物逐一对账
./scripts/archive.sh --verify        # 同一件事的封装
```

SHA-256 不写在本文件里：源码包会把自己打进自己，写进去就自相矛盾。**唯一事实来源是
`dist/SHA256SUMS`**（随版本入库）。

## 代码状态

- `main`：1.1.0（tag `v1.1.0`，归档批 tag `v1.1.0-archive`），含 `beautify` 的全部改动（`--no-ff` 合并）
- `beautify`：保留在合并点，后续视觉调整可以继续在它上面做、再合回来
- 源码包内容 = `git archive HEAD`（已提交的文件），含 `app/`、`app/schemas/`、`docs/`、`keystore/`、
  `README.md` / `CHANGELOG.md` / `ARCHIVE.md` / `gradle*` / `.github/`
- `keystore/` 是**故意入库**的：`release.keystore` + `release.properties` 丢了，已装的正式版就再也升不了级
- 测试：JVM 38 个用例、Robolectric 4 个、仪器测试 **12 个**（含 8 个 Compose UI 测试）
- CI：`.github/workflows/ci.yml`（单测 → lint → 编译仪器测试 → 构建 + 64 位自检）；仓库无远端，推上去才跑

## 重新打包 / 再归档

```bash
cd diary
./scripts/archive.sh            # 按当前 HEAD 重打源码包 + 刷新 dist/SHA256SUMS
./scripts/archive.sh --verify   # 只校验
```

源码包走 `git archive`，时间戳取 commit 时间，同一个 commit 重打出来字节一致；工作区不干净时脚本直接拒绝。

## 如何重新构建

```bash
cd diary
./build.sh assembleDebug     # 日常调试包
./build.sh assembleRelease   # 正式包（只在需要时）
./build.sh :app:testDebugUnitTest
./build.sh :app:lintDebug    # 当前 0 Error
ANDROID_SERIAL=emulator-5554 ./build.sh :app:connectedDebugAndroidTest
./build.sh check64Bit
```

工具链在同级 `.toolchain/`（不进版本库）：Corretto 21 / SDK 36 / Gradle 8.14.3。

## 这一版视觉上的变化

- 卡片、按钮、对话框、顶栏、开关、图标、分隔线全部手绘描边
- 心情表情手绘化，四套画法可选（小熊 / 鹅黄 / 圆环 / 蜡笔）
- 时间线卡片按心情着色更浓，正文两行；底栏药丸在 tab 间滑动
- 日历：换月横滑、今天脉冲、年月选择器、「今天」按钮
- 设置：跟随取色 / 深浅色三档 / 心情图标 / 液态玻璃（OriginOS 7 默认开）
- 桌面图标：手绘线圈本

## 数据与备份

1. 手动导出：加密库 + 图片 + 自定义纸张 + 导入字体 + 恢复码封的 `key.bin`
2. 自动备份：每天 3:00，留 7 份
3. 换机/跨包恢复：输入恢复码 → 解开备份库口令 → 本机口令重封

## 已知欠账

见 `README.md` §6：没有云同步、Markdown 只存不渲染、提醒/自动备份时间不可调、
仪器测试不覆盖编辑器输入与锁屏流程。
