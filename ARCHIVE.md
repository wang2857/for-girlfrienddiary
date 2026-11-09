# 存档记录

存档时间：**2026-11-09 23:23**

版本：**1.1.0（手绘改版，调试包 + 正式签名包）** ｜ versionCode **77** ｜ 数据库 **v9（SQLCipher + FTS5）** ｜ 纯 64 位（arm64-v8a）

> 1.1.0 = 1.0.4 之后在 `beautify` 分支上做的一整套视觉重做，已合回 `main`。
> 正式包用 `keystore/release.keystore`（RSA 4096）签名，包名 `com.synn.diary`，和调试包可共存。

## 产物

| 文件 | 体积 | SHA-256 |
|---|---|---|
| `dist/日记-1.1.0-release.apk` | 17 MB | `eb934276afdb206ec187f998f2d4a64a5b9552d4f4a6fb308609789030de6e34` |
| `dist/日记-1.1.0-debug.apk` | 22 MB | `962fbd5727142f4363d9cde5661735f623a0973cc62f86aade61fe37496724b7` |
| `dist/日记-1.1.0-源码.zip` | 6.5 MB | `2d5734e91a3f93de1dc989801cd1ea9c2ff7523d12eea030462fb64e385dddbb` |

调试包 `com.synn.diary.debug`，正式包 `com.synn.diary`；各自 `install -r` 覆盖安装保数据。

## 代码状态

- `main`：1.1.0（tag `v1.1.0`），包含 `beautify` 的全部改动（`--no-ff` 合并）
- `beautify`：保留在合并点，后续视觉调整可以继续在它上面做、再合回来
- 源码 zip 不含 `.git`：`app/` + `app/schemas/` + `docs/`（含 `beautify-plan.md`）+ `keystore/`
- 测试：JVM 38 个用例、Robolectric 4 个、仪器测试 **12 个**（含 8 个 Compose UI 测试）
- CI：`.github/workflows/ci.yml`（单测 → lint → 编译仪器测试 → 构建 + 64 位自检）；仓库无远端，推上去才跑

## 如何重新构建

```bash
cd diary
./build.sh assembleDebug     # 日常调试包
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
