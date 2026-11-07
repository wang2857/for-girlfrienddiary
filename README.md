# 日记 · 自用

安卓本地日记 App。只为自己用：不上架、不联网、数据全在手机上。

**当前版本 0.24.4** ｜ 包名 `com.synn.diary`（正式）/ `com.synn.diary.debug`（调试）｜ 纯 64 位
｜ 数据库 v6 ｜ 变更历史见 [CHANGELOG.md](CHANGELOG.md) ｜ 存档信息见 [ARCHIVE.md](ARCHIVE.md)

---

## 目录

- [1. 功能](#1-功能)
  - [写](#写) · [看](#看) · [保护与整理](#保护与整理) · [外观与文字](#外观与文字)
- [2. 构建、打包、安装](#2-构建打包安装)
- [3. 工程](#3-工程)
- [4. vivo 适配](#4-vivo-适配)
- [5. 验证记录](#5-验证记录)
- [6. 已知欠账](#6-已知欠账)

---

## 1. 功能

### 写

- 「写一笔」直接进编辑：光标落在正文、键盘自动弹出，标题可选
- 打开已有日记时**光标在正文末尾**，可以直接接着写
- **底部工具栏**：插入图片 / 更换纸张 / 删除，键盘弹出时跟着上浮
- 顶栏只有 返回 / 保存；**点保存立即落盘并回到列表**
- 顶栏下方一行抬头：**日期 星期 时间 · 农历 · 天气 · 地点**，居中。
  放得下就一行；放不下时**在天气前显式换行**（最多两行），保证农历、天气、地点都不丢，
  也不会让自动折行把「小雨」劈成两半。只有窄到两行都装不下，才按「星期 → 天气地点 → 农历」省略
  （用 `TextMeasurer` 实测宽度选最长能放下的组合）
- **插入图片**：工具栏「图片」→ 系统选图器 → 插到当前光标处，可放在正文任意位置
- **纸张底色**：默认 / 米黄 / 淡绿 / 淡蓝 / 淡灰，按日记单独存
- 输入停顿 700ms 自动存盘；退出页面强制再存一次；底部显示「已自动保存 · 时:分」
- 心情 8 种，每种一个颜色；卡片底色跟着心情走
- 标签：点「＋ 标签」新建或勾选已有，点已加的标签即移除
- **自动记录环境**：新建后 2 分钟内首次打开时抓一次天气 + 经纬度；失败就什么都不显示，绝不编造
- 删除进回收站（软删除）

### 看

- 时间线：**年 → 月 → 周 三级分组，每一级都能点标题折叠**；只有一年数据时自动隐藏"年"这层；
  贴顶的永远是最顶层那级（多年贴年、单年贴月）
- 卡片抬头：「11月7日 周六 九月廿九 小雨 3°」——**只到日、不显示时分**，农历和天气都在左侧，
  右侧只留 ★ / 🔒 / 心情。位置信息不进列表
- 筛选条：标签 + **心情** 合并一行，两者 AND（「工作」+ 😄 = 工作中开心的那天）；
  点「全部」清掉所有筛选；另有「★ 收藏」只看收藏
- 搜索：标题 + 正文子串匹配，中文直接搜，250ms 防抖
- 日历：月历网格，**每格数字下方是农历**——初一显示月份（「十月」）并加重；
  有记录那天的农历用主色；**心情 emoji 挂在数字右上角**；一天多条取"当天最后修改那条"的心情；今天有描边
- 右滑卡片：**删除 / 收藏 / 上锁**

### 保护与整理

- **应用锁**：4 位密码（PBKDF2-HmacSHA256，12 万次迭代 + 16 字节盐）；**指纹优先**，密码兜底；
  进应用先锁、切后台就锁；默认禁截屏（可开关）；忘记密码只能重置（数据仍在）
- **单篇上锁**：右滑「上锁」；**打开**（时间线 / 日历 / 搜索任何入口）和**解除锁定**都要验证。
  列表里上锁的日记只显示日期 + 「🔒 已上锁 · 解锁后查看」，正文、图片、标签一律不渲染
- **收藏**：卡片出现 ★，**不置顶**，列表始终按时间倒序
- **回收站**：删除是软删除；设置里可逐条还原、彻底删除、清空；
  自动清理可选 1 周 / 1 个月 / 1 年 / **永久保留**（默认 1 个月）

### 外观与文字

- 跟随系统深色模式；Android 12+ 壁纸动态取色
- 农历用 `android.icu.util.ChineseCalendar`，不引第三方历法库
- 8 种心情及配色（库里只存 emoji，加心情不需要迁移）
- 空条目（新建后一个字没写）不会污染时间线

---

## 2. 构建、打包、安装

工具链装在仓库同级 `.toolchain/`（不污染系统）：Corretto 21 / Android SDK 36 / Gradle 8.14.3 + 全量依赖缓存（可离线）。

```bash
cd diary
./build.sh assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./build.sh assembleRelease        # app/build/outputs/apk/release/app-release.apk
./build.sh check64Bit             # 64 位合规自检：混进 32 位 so 就构建失败
./build.sh --stop                 # 改过环境变量后先停 daemon
```

| | release | debug |
|---|---|---|
| 包名 | `com.synn.diary` | `com.synn.diary.debug` |
| 体积 | 9.0 MB | 13 MB |
| 数据 | 独立，与 debug 不相通 | 独立 |

- release 沿用 `keystore/debug.keystore`（不上架，没另建正式签名）
- **关掉 R8**（`isMinifyEnabled = false`）：Room / Coil 的反射在混淆下有踩坑风险，个人用不值得冒险

```bash
adb install -r dist/日记-0.24.4-debug.apk   # 升级请用 -r，数据库迁移会保住数据
```

`allowBackup=false` + `dataExtractionRules` 全排除，所以**卸载重装会清空**。

---

## 3. 工程

### 结构

```
app/src/main/java/com/synn/diary/
├── DiaryApp.kt             Application：启动清理回收站、注册内存压力桥
├── MainActivity.kt         edge-to-edge、应用锁闸门、FLAG_SECURE
├── data/
│   ├── AppContainer.kt     手写依赖容器（不上 Hilt）
│   ├── EntryRepository.kt  唯一数据入口（读/写/标签/收藏/上锁/回收站）
│   ├── PendingWrites.kt    未落盘内容登记处（供 vivo KILL 广播刷盘）
│   ├── TrashSettings.kt    回收站保留策略
│   ├── body/BodyMarkers.kt 正文里的图片标记
│   ├── db/                 Room：Entity / Dao / Database / 迁移 / EntryWithTags
│   ├── lock/               应用锁存储与状态机
│   ├── media/ImageStore.kt 图片导入、压缩、EXIF 摆正
│   ├── memory/             vivo 公平运行内存机制客户端
│   └── weather/            WeatherService（open-meteo + LocationManager）
├── ui/
│   ├── DiaryNavHost.kt     路由 + 统一的「打开日记」闸门
│   ├── components/         EntryCard、SwipeActionsContainer、DiaryBottomBar…
│   ├── timeline/ calendar/ search/ editor/ trash/ settings/ lock/
│   └── theme/               M3 主题
└── util/                   日期与农历、ViewModel 工厂
```

### 数据模型

`entry` 表（v6）：`id / uuid / entryDate(epochDay) / createdAt / updatedAt / title / body / mood /
deletedAt / weatherText / locationText / latitude / longitude / background / favorite / locked`

- 另有 `tag`、`entry_tag`、`attachment` 三张表，外键 `CASCADE`
- `exportSchema = true`，schema JSON 在 `app/schemas/`；迁移 SQL 照它核对，不凭记忆写
- 迁移链：1→2（标签表）· 2→3（天气/位置）· 3→4（附件表）· 4→5（纸张）· 5→6（收藏、上锁）

### 天气与定位

- 定位用平台 `LocationManager`（不用 play-services：国内 ROM 常缺 GMS）
- 天气走 [open-meteo](https://open-meteo.com/)，**不需要 API key**
- 权限只在第一次「写一笔」时问一次；拒绝后不再打扰，日记照写
- 只对**新建后 2 分钟内**打开的条目抓环境；老记录不补历史天气
- `updatedAt` 不因抓天气而改变（日历心情靠它取"当天最后修改"）
- **抓取跑在 Application 作用域**：写完立刻退出是常态，用 `viewModelScope` 会被连带取消
- 先看"最近 10 分钟的已知位置"，没有再并行向网络/GPS 要一次（单个 3s 超时）

**已知限制**：城市名靠系统 `Geocoder`，国内 ROM 基本走 Google 服务、查不到，退化成经纬度。

### 图片

- 选图用系统 Photo Picker（`PickVisualMedia`），**不需要存储权限**
- 图片复制进 `files/images/`，长边 1600、JPEG 85、EXIF 摆正（外部 Uri 会失效）
- 正文存 `[[img:<文件名>|宽度%|对齐]]` 标记，编辑器解析成图片块插在文本块之间
- 点图片在手指位置弹浮层：**宽度滑块 20%–100%** + 左/中/右对齐
- 列表卡片显示正文第一张图（裁剪 150dp）+ 去掉标记的摘要

### 几个刻意的设计

- **`entryDate` 用 epochDay，与 `createdAt` 分开**：补写昨天能归到正确那天；日历和「那年今日」都依赖它
- **新建即入库**：点「写一笔」先插空行，之后都是 update；空行用 SQL 过滤，比退出时判断"要不要删"更稳
- **退出时用 Application 作用域兜底写盘**：`viewModelScope` 在返回那一刻就取消，最后 700ms 的字会丢
- **搜索先用 LIKE**：几万条内够用、零依赖；量大了再上 FTS5 trigram
- **版本组合锁在 AGP 8.13 能吃的范围**：更新的 androidx 要求 AGP 9.1 + compileSdk 37

---

## 4. vivo 适配

五份文档逐条对过，结论都写在各小节。

### 4.1 Android 16（[doc/832](https://dev.vivo.com.cn/documentCenter/doc/832)）

| 条目 | 状态 |
|---|---|
| 1.1 `scheduleAtFixedRate` | 不使用，N/A |
| 1.2 `elegantTextHeight` | 未设置，N/A |
| 1.3 大屏自适应（target 36 忽略方向/尺寸/宽高比） | 不锁方向、`resizeableActivity=true`，实测 700×1000 小窗；state 在 ViewModel，重建不丢正文 |
| 1.4 预测性返回 | 显式 `enableOnBackInvokedCallback="true"`，返回走 Navigation 的 `OnBackPressedDispatcher`；实测键盘先收、再返回列表 |
| 1.5 健康权限 / 1.6 本地网络权限 | 不使用，N/A |
| 2.2 Intent 重定向加固 | **试过后撤销**：`intentMatchingFlags="enforceIntentFilter"` 会拦掉不匹配 filter 的显式启动（日志 `Access blocked`），而我们不处理外来 Intent |
| 2.3–2.5 JobScheduler / 废弃 API | 无后台作业，N/A |
| 2.7 无边框强制 | 已 edge-to-edge，未用 `windowOptOutEdgeToEdgeEnforcement` |
| 2.11 Java API | 未用 `Thread.destroy/stop`、未用 `Locale` 构造器 |

### 4.2 64 位（[doc/511](https://dev.vivo.com.cn/documentCenter/doc/511)）

```
$ zipinfo -1 app-debug.apk | grep -E "\.so$"
lib/arm64-v8a/libandroidx.graphics.path.so      ← 只有这一个
$ dumpsys package … | grep primaryCpuAbi
primaryCpuAbi=arm64-v8a   secondaryCpuAbi=null  ← 纯 64 位运行
```

- 无 RenderScript 位码（`.bc` 文件数 0）
- `./build.sh check64Bit` 在构建期兜底：混进 32 位 so 直接失败
- 代价：x86_64 模拟器装不上
- native 库 LOAD 段按 `0x4000` 对齐，满足 16KB 页设备

### 4.3 小窗（[doc/582](https://dev.vivo.com.cn/documentCenter/doc/582)）

| 条目 | 做法 |
|---|---|
| 2.1 声明多窗口 | `resizeableActivity="true"` |
| 2.2 Configuration 变化 | 声明 `configChanges`，编辑中缩放窗口不重建、正文不丢 |
| 2.3 taskAffinity | 显式声明 |
| 2.4 Context | UI 全走 Activity Context；Application Context 只用于文件与网络 |
| 2.5 小窗功能条 | `Modifier.captionBarPadding()` 预留，不写死高度 |
| 2.6 布局建议 | 浮层位置从「物理屏幕」改成 `LocalWindowInfo.containerSize`（窗口尺寸） |

整页滚动：情绪、标签、标题、正文在同一个滚动容器里——正文若是 `weight(1f)` 的独立滚动区，
小窗一开键盘就被挤成 0 高，完全没法打字。

### 4.4 内存机制（[doc/1013](https://dev.vivo.com.cn/documentCenter/doc/1013)）

| 广播 | 处理 |
|---|---|
| `itgsa.intent.action.TRIM` | 清 Coil 图片内存缓存 |
| `itgsa.intent.action.KILL` | `PendingWrites.flushAll()` 把没落盘的正文写库 |

动态注册（`HandlerThread` + `RECEIVER_EXPORTED`），3 秒内通过 Binder 回执（`TRANSACTION_EXCEPTION_REPLY`，`result=0`）。
只在 vivo 上收得到，别的机型注册了也无副作用。

### 4.5 高长宽比（[doc/195](https://dev.vivo.com.cn/documentCenter/doc/195)）

按文档 §3 实测 `wm size 1080x2400 / 2460 / 2520`：无黑边、无裁切，首尾像素都是应用背景色。
没有写死任何宽高比，底部导航贴真实底边。

### 4.6 为什么没接 doc/104

那份是 vivo X21（2018）的私有库 `com.vivo.fingerprint.ud.FingerprintInsets`，只告诉应用
"屏下指纹图标画在哪"，好让应用自己画指纹 UI。我们用系统 `BiometricPrompt`，识别界面由系统绘制，
不需要它，也不该引闭源 jar + `READ_STATE` 权限。

---

## 5. 验证记录

在 API 36 arm64 模拟器 + 真机（Redmi 22081212C / Android 15 / HyperOS）上跑过：

| 场景 | 结果 |
|---|---|
| 数据库迁移 v1→…→v6 | ✅ 每步都实测过；v5→v6 在真机上 15 条记录完整 |
| 新建 + 自动保存 + 保存返回列表 | ✅ |
| 天气自动记录 | ✅ 新建 19 秒后写入「小雨 15° · 30.29°N 120.16°E」 |
| 老记录不补天气 | ✅ 把 `createdAt` 改成 10 分钟前，打开后不抓取、天气仍为空 |
| 编辑不改变时间/天气/位置 | ✅ 只有 `body` 和 `updatedAt` 变 |
| 标签 / 心情筛选（AND） | ✅ |
| 搜索（中文直接搜） | ✅ |
| 日历（农历、初一显示月份、心情取最后修改） | ✅ |
| 收藏 | ✅ 出现 ★；不置顶；「★ 收藏」只筛收藏 |
| 右滑菜单 | ✅ 删除 / 收藏 / 上锁；弹性动画；面板四角圆 |
| 单篇上锁 | ✅ 时间线 / 日历 / 搜索三个入口都要验证；解锁也要验证 |
| 上锁卡片隐藏内容 | ✅ 只显示日期 + 占位文案 |
| 应用锁 | ✅ 冷启动要密码；切后台再回来要密码；输错提示；允许截屏开关立即生效 |
| 回收站 | ✅ 还原、彻底删除（连图片）、清空；400 天前的条目在「永久保留」下不被清、切「1 周」立刻被清 |
| 高长宽比 1080×2400/2460/2520 | ✅ 无黑边无裁切 |
| 小窗 700×1000 + 键盘 | ✅ 正文可编辑 |
| 64 位 | ✅ `primaryCpuAbi=arm64-v8a`，`secondaryCpuAbi=null` |
| release 包 | ✅ 安装、启动、写一条、保存、列表可见 |

界面截图在 `docs/`。ATD 镜像 + 无窗口渲染下 `screencap` 全黑，所以早期验证靠 `uiautomator dump` 读控件文本；
后改用 `google_apis` 镜像后截图正常。

---

## 6. 已知欠账

- **没有导出 / 导入备份**——所有日记只有手机里一份。这是最该补的一项
- 图片附件表 `attachment` 目前空着（图片信息直接从正文标记解析），彻底删除时才顺带清图片文件
- 数据库未加密（用了应用锁 + `FLAG_SECURE`，但库本身是明文 SQLite）
- 从没实现过的：每日提醒、Markdown 预览、那年今日
- `docs/` 里的截图是逐个功能加的，风格不完全统一

产品层面的规划见仓库外的 `ANDROID-DIARY-PLAN.md`（已复制一份到 `docs/plan.md`）。
