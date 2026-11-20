# 日记 · 自用

安卓本地日记 App。只为自己用：不上架、不联网、数据全在手机上。

仓库：<https://github.com/wang2857/for-girlfrienddiary> ｜ 许可证：MIT ｜ CI：[![CI](https://github.com/wang2857/for-girlfrienddiary/actions/workflows/ci.yml/badge.svg)](https://github.com/wang2857/for-girlfrienddiary/actions/workflows/ci.yml)

**当前版本 1.1.1（手绘改版，MIT 开源）** ｜ 包名 `com.synn.diary`（正式）/ `com.synn.diary.debug`（调试）｜ 纯 64 位
｜ 数据库 v9 ｜ 变更历史见 [CHANGELOG.md](CHANGELOG.md) ｜ 存档信息见 [ARCHIVE.md](ARCHIVE.md)

---

## 目录

- [1. 功能](#1-功能)
  - [写](#写) · [看](#看) · [首次安装引导](#首次安装引导) · [保护与整理](#保护与整理) · [每日提醒](#每日提醒) · [备份与整理](#备份与整理) · [稳定性](#稳定性) · [外观与文字](#外观与文字)
- [2. 构建、打包、安装](#2-构建打包安装)
- [3. 工程](#3-工程)
- [4. vivo 适配](#4-vivo-适配)
- [5. 验证记录](#5-验证记录)
- [6. 已知欠账](#6-已知欠账)
- [7. 开源与许可](#7-开源与许可)

---

## 1. 功能

### 写

- 「写一笔」直接进编辑：光标落在正文、键盘自动弹出，标题可选
- 打开已有日记时**光标在正文末尾**，可以直接接着写
- **底部工具栏**：插入图片 / **横线开关** / 更换纸张 / 删除，键盘弹出时跟着上浮
- **整页是同一张纸**：纸的底色 / 花纹 / 自定义照片一直铺到状态栏和顶栏，
  上方不再有一条颜色不同的顶栏
- **整界面手绘风**：卡片/按钮/对话框/顶栏/开关/图标全部是手绘描边，8 种心情有四套画法可选
- **深浅色**：设置里可选跟随系统 / 强制浅色 / 强制深色
- **正文字体可选**：工具栏「字体」→ 系统默认 / 系统衬线 / 系统等宽 / 霞鹜文楷，
  还能**导入自己的 ttf/otf**。字体只作用于这条日记的正文和标题，界面本身仍是手机系统字体
- **纸页横线可以关**：工具栏「横线 · 开/关」一键切换，关掉就是一张干净的白纸；选择会记住，
  跟具体哪篇日记无关。带花纹的纸和自定义照片本来就不画线（两种纹理叠一起太吵），
  在这种纸上打开开关会提示一句
- 顶栏只有两个图标：**←** 返回、**💾** 保存（软盘，没有文字，`contentDescription` 里仍是「返回」「保存」）；
  **点保存先弹「✓ 已收好」（带一次触感反馈），约半秒后回到列表**
- 顶栏下方一行抬头：**日期 星期 时间 · 农历 · 天气 · 地点**，居中。
  放得下就一行；放不下时**在天气前显式换行**（最多两行），保证农历、天气、地点都不丢，
  也不会让自动折行把「小雨」劈成两半。只有窄到两行都装不下，才按「星期 → 天气地点 → 农历」省略
  （用 `TextMeasurer` 实测宽度选最长能放下的组合）
- **插入图片**：工具栏「图片」→ 系统选图器 → 插到当前光标处，可放在正文任意位置
- **纸张**：8 种预设（默认 / 米黄 / 薄荷 / 蜜桃 / 樱花 / 晴空 / 奶黄 / 格纸），花纹（圆点 / 爱心 / 星星 / 格线）用 Canvas 画，不带图片资源；
  还支持**自定义背景**——从相册选一张图铺成纸张，自动盖一层淡色蒙版保证字能看清
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
- 搜索：**FTS5（trigram）全文索引**，3 个字以上直接走索引，任意子串都能命中，不用分词；
  2 个字及以下自动退回 `LIKE`（trigram 只索引 3 字窗口）。250ms 防抖；顶栏「搜索」居中、返回是 ← 图标
- 日历：月历网格，**每格数字下方是农历**——初一显示月份（「十月」）并加重；
  有记录那天的农历用主色；**心情 emoji 挂在数字右上角**；一天多条取"当天最后修改那条"的心情；今天有描边
- 右滑卡片：**删除 / 收藏 / 上锁**

### 首次安装引导

装好第一次打开会走：说明 → 设置 4 位密码 → 再输一次确认 → **抄恢复码**；设备支持指纹时再多一步「顺便开指纹？」。
可以「以后再说」，但默认路径是让你设密码——日记只有一份数据，忘记密码的代价比设置密码更大。

设完密码会立刻给一串 16 位恢复码（`XGGY GJWH CSD7 JPVT` 这种，4 组、去掉了 0/O、1/I 等容易抄错的字符）。
忘记密码时靠它救回数据；当场没抄也没关系——设置 → 安全 → 恢复码 →「查看」，验一次密码就能再看一遍。

### 保护与整理

- **数据库加密**：SQLCipher + 随机 32 字节口令（口令用 AndroidKeyStore 的 AES-256-GCM 裹起来存）。
  口令跟 4 位密码无关——4 位密码只有 1 万种，拿来派生密钥等于没加密
- **不再兼容明文库**：0.33.0 之前的明文数据库一旦被发现，直接删掉、重新建加密库；
  备份导入也只接受加密备份，老明文备份会被拒绝（产品上放弃了那批数据）
- **应用锁**：4 位密码（PBKDF2-HmacSHA256，12 万次迭代 + 16 字节盐）；**指纹优先**，密码兜底；
  进应用先锁、切后台就锁；默认禁截屏（可开关）
- **切走时先把画面盖住**：一按 home / 切到别的应用，立刻盖一块与窗口同色的不透明挡板，
  回来时先看到挡板再看到锁屏，不会有一瞬间露出正文。
  权限弹窗、系统选图这类"只暂停没真走"的界面不算离开——不盖、也不锁
- **忘记密码有退路**：锁屏「忘记密码？」→ 输恢复码 → 直接设新密码，日记不丢
- **恢复码可以再看**：哈希只用于验证，另存一份用 AndroidKeyStore 的 AES-256-GCM 密钥封起来的明文；
  设置里点「查看」先验 4 位密码，再显示那 16 位。改密码、用恢复码重置都不影响它
- 设置里可「重设」（旧码立刻作废）；重设后旧的那张纸就没用了
- **单篇上锁**：右滑「上锁」（**前提是应用锁已开启且有密码**——没有凭证就不让锁，否则会锁出永远解不开的日记）；**打开**（时间线 / 日历 / 搜索任何入口）和**解除锁定**都要验证。
  列表里上锁的日记只显示日期 + 「🔒 已上锁 · 解锁后查看」，正文、图片、标签一律不渲染
- **收藏**：卡片出现 ★，**不置顶**，列表始终按时间倒序
- **回收站**：删除是软删除；设置里可逐条还原、彻底删除、清空（上锁的日记在回收站里也只显示「🔒 已上锁」）；
  自动清理可选 1 周 / 1 个月 / 1 年 / **永久保留**（默认 1 个月）

### 每日提醒

- **设置 → 提醒 → 每日提醒**，默认关闭；打开时会请求通知权限
- 固定 **21:00**，用 WorkManager 每 24 小时一次；**当天已经写过就不发**（空条目不算）
- 通知用系统标准样式（`BigTextStyle`），渠道重要性 `IMPORTANCE_DEFAULT`（不会弹悬浮窗）
- 用固定的 notification id，重复触发只覆盖不堆叠；`ongoing = false`
- 点通知 / 点「写一句」直达一篇**新日记**（带 `EXTRA_NEW_ENTRY`，解锁后才生效）
- 设置里有「试一下提醒」，立刻发一条看效果

对照 vivo 两份通知规范（[doc/577](https://dev.vivo.com.cn/documentCenter/doc/577)、[doc/901](https://dev.vivo.com.cn/documentCenter/doc/901)）：
不用自定义 RemoteViews、不用 ongoing、不自定义分组、文案短、有落地页。

### 备份与整理

- **导出备份**：设置 → 数据 → 导出备份，把 `diary.db` + 正文图片 + 自定义纸张 + **导入的字体**打成一个 zip（走系统文件选择器，存哪都行）
- **导入备份（覆盖）**：选 zip → 确认 → 自动重启。包里带 `manifest.json`（条数、库版本），版本比本机新会拒绝导入
- **换机可恢复**：导出时用**恢复码**把数据库口令再封一份放进包里（`key.bin`）。
  在新手机导入时输入当初抄下的 16 位恢复码，就地把备份库换成本机口令再写回——
  前提是导出那一刻设过恢复码（没设的话只能做本机备份）
- **合并导入**：选 zip → 按 `uuid` 只把"本机没有的日记"加进来，不动现有内容，**不用重启**；
  回收站里的记录跳过（不把有意删掉的捞回来），标签按名字复用，缺的图片/纸张才补。
  跨设备的备份也能合并——会先问恢复码，解开备份库口令再读
- 数据库表结构、单位约定、全文索引、常用查询写在 [docs/database.md](docs/database.md)
- **标签管理**：筛选栏「管理」→ 逐条删除标签（只删标签，日记内容不动）
- **自动备份**：设置 → 数据 → 打开开关，每天凌晨 3:00 往应用私有目录（`files/backup/`）存一份完整 zip，
  保留最近 7 份、总量超过 600 MB 从最旧的删；打开开关时立刻先存一份
- 启动时清理**没人引用的图片**（删掉正文里的 `[[img:]]` 标记不会删文件，这里兜底；1 小时内新建的不动）

### 稳定性

- **崩溃日志**：装了全局未捕获异常处理器，闪退时把时间、版本、机型、系统、线程和完整堆栈写到
  `files/crash/`，最多留 10 份；设置 → 关于 → 崩溃日志可以看全文、复制、清空
- 不联网、不上报，日志只在这台手机上

### 外观与文字

- **正文字体**：霞鹜文楷（`res/font/lxgw_wenkai.ttf`，只保留 GB2312 常用字 + 标点，3.4 MB）；
  正文/标题用它，按钮和标签仍走系统字体
- 统一圆角：8 / 12 / 16 / 24 / 28（`Shapes`），不再用 M3 默认梯度
- 卡片：日期 16sp 半粗 + 心情色，星期/农历/天气降为一行小字；心情 emoji 有 28dp 浅色圆底
- 分组标题：月份写中文数字（「十一月」），周标题用一条细线代替「本周」胶囊
- 空状态：按时段问候 + 一句「哪怕一句话也算」
- 编辑器：纸面加极淡横线（不参与布局，纯装饰），可在工具栏关掉；纸铺满整页（含状态栏和顶栏）
- 深色模式：卡片心情底色 16% → 22%，避免深底吃掉颜色

- 跟随系统深色模式；Android 12+ 壁纸动态取色
- 农历用 `android.icu.util.ChineseCalendar`，不引第三方历法库
- 8 种心情及配色（库里只存 emoji，加心情不需要迁移）
- 空条目（新建后一个字没写）不会污染时间线

---

## 2. 构建、打包、安装

工具链装在仓库同级 `.toolchain/`（不污染系统）：Corretto 21 / Android SDK 36 / Gradle 8.14.3 + 全量依赖缓存（可离线）。

```bash
cd diary
./build.sh assembleDebug          # app/build/outputs/apk/debug/app-debug.apk  ← 日常只出这个
./build.sh check64Bit             # 64 位合规自检：混进 32 位 so 就构建失败
./build.sh --stop                 # 改过环境变量后先停 daemon

# 正式包（com.synn.diary）只在明确需要时才构建，默认不构建：
./build.sh assembleRelease
```

> **日常开发仍用调试包**（`com.synn.diary.debug`）；正式包（`com.synn.diary`）只在存档/交付时构建，
> 它和调试包是两个不同的应用，可以同时装、数据互不相通。

| | debug（日常） | release（正式） |
|---|---|---|
| 包名 | `com.synn.diary.debug` | `com.synn.diary` |
| 签名 | `keystore/debug.keystore`（入库） | 自备（RSA 4096，不入库） |
| 体积 | 约 21 MB | 约 17 MB |
| 数据 | 独立 | 独立 |

- 仓库里**没有**正式签名材料（公开仓库放私钥等于公开升级凭据）。自己发布时把
  `keystore/release.properties.example` 复制成 `keystore/release.properties`，配上自己的 keystore；
  没有这份配置时 `assembleRelease` 会回落到调试签名，照样能构建
- 你那套 `release.keystore` + `release.properties` **丢了，已装的正式版就再也升不了级**，务必单独备份
- 两个包的数据不互通：要把调试版的数据搬到正式版，先在调试版设恢复码 → 导出备份 →
  在正式版用恢复码导入（普通"设备备份"不行，Keystore 不同解不开）
- **关掉 R8**（`isMinifyEnabled = false`）：Room / Coil 的反射在混淆下有踩坑风险，个人用不值得冒险

```bash
./build.sh assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk      # 调试版升级
adb install -r app/build/outputs/apk/release/app-release.apk  # 正式版升级（同一个 key 才能 -r）
```

`allowBackup=false` + `dataExtractionRules` 全排除，所以**卸载重装会清空**。

### 归档

APK / 源码包不进仓库（公开仓库保持轻量，签名材料更不能进），只在本地 `dist/` 留一份，配 `SHA256SUMS`：

```bash
./scripts/archive.sh            # 按当前 HEAD 重打源码包，刷新 dist/SHA256SUMS
./scripts/archive.sh --verify   # 校验 dist/ 里现有产物是否对得上
```

源码包走 `git archive`，不含 `.git` / `build/` / `dist/`，同一个 commit 重打字节一致。
更细的批次记录见 [ARCHIVE.md](ARCHIVE.md)。

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

### 测试与 CI

```bash
./build.sh :app:testDebugUnitTest   # 纯逻辑单测（JVM，不用模拟器）
./build.sh :app:lintDebug           # 静态检查，0 Error 才过
./build.sh :app:check64Bit          # 构建 + 64 位自检
```

- **JVM 单测 10 个文件 / 37 个用例**：正文图片标记、搜索策略（含 emoji 码点）、恢复码规范化、
  备份口令封装、明文库文件头、FTS 查询转义、纸张 id 映射、日期格式化；
  另有 Robolectric 用例：`8→9` 真迁移（拿导出的 8.json 建库再跑迁移）、时间线过滤（空壳/回收站/收藏）
- **Android 仪器测试 12 个用例**（`androidTest`，跑在真机/arm64 模拟器上）：
  FTS5 触发器（插入/更新/删除同步、多命中排序）＋ 合并往返（导出→删除→合并）＋
  覆盖导入往返（导出→再写一条→整包覆盖→快照回来、后写的消失）＋
  **Compose UI 测试**：手绘开关/chip/对话框按钮的回调与禁用态、时间线显示日记与铅笔钮、
  日历点标题弹年月选择器并跳转、今天按钮存在
- 覆盖导入那条**会真的重置应用数据**（这是 import 的定义），所以各测试自己建 `AppContainer`，互不影响
- Robolectric 的 SQLite 不带 FTS5，所以索引相关的只能用仪器测试；反过来仪器测试要设备，CI 上只编译不跑
- `.github/workflows/ci.yml`（推到 GitHub 后生效）：JDK 21 + SDK 36，依次跑单测 → lint → 构建 + 64 位自检，
  失败时上传测试与 lint 报告
- 目前**只测纯逻辑**：数据库/DAO、UI、备份导入这类要 Android 环境的还没有集成测试

### 数据模型

`entry` 表（v9）：`id / uuid / entryDate(epochDay) / createdAt / updatedAt / title / body / mood /
deletedAt / weatherText / locationText / latitude / longitude / background / font / favorite / locked`

- 另有 `tag`、`entry_tag` 两张业务表（外键 `CASCADE`），以及全文索引虚拟表 `entry_fts`（FTS5 trigram）
- 图片**不进库**：正文用 `[[img:<文件名>|宽度%|对齐]]` 标记，文件在 `files/images/`；
  自定义纸张是 `background = img:<文件名>`，文件在 `files/backgrounds/`
- `exportSchema = true`，schema JSON 在 `app/schemas/com.synn.diary.data.db.AppDatabase/`（当前 `9.json`）
- v8 之前没有迁移链（删表重建）；**v8→v9 加字体列这条是正经迁移**（`ALTER TABLE ADD COLUMN`），
  以后新增字段都按这个来`

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
- 图片**不建元数据表**（尺寸、大小都不落库）：展示只认正文标记，清理靠"扫正文 + 扫目录"，
  少一张可能和真实状态对不上的表
- 启动时清理孤儿文件，**正文图和自定义纸张都清**：删掉 `[[img:]]` 标记、或把纸张从自定义换掉之后，
  旧文件会在下次启动被删（1 小时内的新文件不动）

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

通知相关（doc/577、doc/901）：只用标准样式；渠道 `IMPORTANCE_DEFAULT`；不发 ongoing；不做自定义分组；
每天最多一条且"写过就不发"；通知点击有明确落地页（新日记）。

### 4.2 64 位（[doc/511](https://dev.vivo.com.cn/documentCenter/doc/511)）

```
$ zipinfo -1 app-debug.apk | grep -E "\.so$"
lib/arm64-v8a/libandroidx.graphics.path.so
lib/arm64-v8a/libsqlcipher.so                   ← SQLCipher（约 5.8 MB），全包最大的一个
$ dumpsys package … | grep primaryCpuAbi
primaryCpuAbi=arm64-v8a   secondaryCpuAbi=null  ← 纯 64 位运行
```

- 无 RenderScript 位码（`.bc` 文件数 0）
- `./build.sh check64Bit` 在构建期兜底：混进 32 位 so 直接失败
- 代价：x86_64 模拟器装不上（lint 的 ChromeOsAbiSupport 已显式关掉）
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
| 正文字体（预设） | ✅ 选「霞鹜文楷」后正文立刻变，界面仍是系统字体；重开应用仍记得 |
| 正文字体（导入） | ✅ 从 Downloads 导入 3.5 MB 的 ttf → 出现在「我的字体」并自动选中；重启后仍选中 |
| 数据库 8→9 | ✅ 迁移后原有 2 篇日记还在，`font` 默认 NULL（系统字体） |
| 孤儿背景图清理 | ✅ 塞一张 2 小时前的 `orphan-test.jpg` 进 `files/backgrounds/`，重启后被删；仍被引用的背景图留着 |
| 工具栏横线开关 | ✅ 点一下横线消失、按钮变灰「横线 · 关」；再点回来就恢复；重开应用仍记得 |
| 横线跟纸张的关系 | ✅ 格纸上不画线，此时打开开关会提示原因；切回纯色纸横线立刻回来 |
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
| 切后台不露内容 | ✅ 从列表 / 编辑器按 home，最后一帧是纯色挡板；回来 0.3s 内已是锁屏，正文一帧都没出现 |
| 系统界面不误锁 | ✅ 通知权限弹窗点允许后仍停在设置页；相册选图回来仍停在编辑器，图也插进去了 |
| 回收站 | ✅ 还原、彻底删除（连图片）、清空；400 天前的条目在「永久保留」下不被清、切「1 周」立刻被清 |
| 恢复码（引导 → 显示 → 抄好） | ✅ 设完密码弹出 16 位码，走完进入时间线 |
| 恢复码再查看 | ✅ 设置「查看」→ 错误密码被拒 → 正确密码后显示同一串；改密码、用恢复码重置之后仍然一样 |
| 恢复码重设 | ✅ 对话框里「重设一个」立刻换新码，旧码随即失效（用新码才走得通重置流程） |
| 忘记密码 → 恢复码重置 | ✅ 旧密码被拒、恢复码换新密码后直接进入时间线；恢复码本身不轮换 |
| 修改密码 | ✅ 设置里改完留在设置页（不再被踢回锁屏），恢复码不变 |
| 自动备份 | ✅ 打开开关立刻产出一份；`files/backup/` 里有 zip，`manifest.json` + `diary.db` 齐全；「立即备份」再产出一份 |
| 崩溃日志 | ✅ 假的崩溃文件能在设置里读出全文、复制、清空；列表随重启刷新 |
| 高长宽比 1080×2400/2460/2520 | ✅ 无黑边无裁切 |
| 小窗 700×1000 + 键盘 | ✅ 正文可编辑 |
| 单元测试 | ✅ 33 个用例全绿（`testDebugUnitTest`） |
| 64 位 | ✅ `primaryCpuAbi=arm64-v8a`，`secondaryCpuAbi=null` |
| release 包 | ✅ 安装、启动、写一条、保存、列表可见 |

界面截图在 `docs/`。ATD 镜像 + 无窗口渲染下 `screencap` 全黑，所以早期验证靠 `uiautomator dump` 读控件文本；
后改用 `google_apis` 镜像后截图正常。

---

## 6. 已知欠账

**安全**

- 恢复码的明文用 Keystore 密钥封在应用私有目录里：能拿 root / adb 备份的人照样解得开
  （数据库本身就是明文，威胁模型里防的是"捡到手机的人"）
- 恢复码没有"上次生成时间"，用户无法判断手里那张纸是不是还有效

**数据**

- 自动备份写在应用私有目录：误删、写坏、导入覆盖都能救；**卸载、换机、丢手机救不了**，那种情况得手动导出到手机外
- 跨设备恢复依赖**恢复码**：没设恢复码时导出的备份只能在原机恢复；恢复码丢了也就恢复不了（跟忘密码同级别）
- 旧版本的明文备份（0.32.0 及更早导出的 zip）**不能导入**，会提示"这是旧版本导出的明文备份"

- 导入仍不能"挑一条恢复"，只能整包合并或整包覆盖
- 图片没有元数据（尺寸/大小/哈希都不落库），也没有相册页：想要"按图找日记""清理大图"这类功能，
  得先补一张 `image` 表并让它真的被读写，而不是再留一张只写不读的表
- 提醒固定 21:00、自动备份固定 3:00，都不可调
- WorkManager 的 24 小时周期会随时间漂移，不是严格到点

**功能**

- Markdown 只存不渲染：正文是纯文本，`**粗体**`、`#` 按字面显示
- 那年今日 / 统计 / 主屏小组件 / 语音速记 / 多日记本 / PDF 年度书都还没做（计划里属于 V1、V2）
- 日记日期不可改：补写昨天、补写上周做不到
- 搜索只有关键词，没有标签 + 日期范围筛选
- 没有字号 / 行距调节，深色只能跟随系统

**稳定性 / 工程**

- 崩溃日志只覆盖 Java/Kotlin 未捕获异常：native crash、ANR、被系统杀进程都不留文件
  （真要补，下一步是 `ApplicationExitInfo`，能拿到系统的 ANR / 低内存记录）
- 单测覆盖纯逻辑与数据库（41 个用例），流程类（编辑、锁屏、备份对话）仍靠手测
- CI 在 GitHub Actions 上跑（单测 → lint → 编译仪器测试 → 构建 + 64 位自检）
- `docs/` 里的截图是逐个功能加的，风格不完全统一

产品层面的规划见仓库外的 `ANDROID-DIARY-PLAN.md`（已复制一份到 `docs/plan.md`）。

---

## 7. 开源与许可

本项目以 **MIT 许可证**发布，全文见 [LICENSE](LICENSE)。

| 文件 | 内容 |
|---|---|
| [LICENSE](LICENSE) | MIT 许可证全文（Copyright (c) 2026 Synn） |
| [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) | 随 APK 分发的第三方组件、版本、许可证，以及开发期依赖 |
| [LICENSE-OFL.txt](LICENSE-OFL.txt) | 霞鹜文楷（LXGW WenKai）的 SIL Open Font License 1.1 全文 |

- 应用内同样能看：**设置 → 关于 → 开源许可**。文本打包在 `app/src/main/res/raw/licenses.txt`，
  离线可看、跟着 APK 走
- APK 里保留 androidx 各库的 `META-INF/androidx/**/LICENSE.txt`（只在打包时挡掉重复的 `AL2.0` / `LGPL2.1` 别名文件）
- 你自己写的日记、导入的图片和字体都属于你，不在上面任何许可范围内
- 仓库不含签名私钥：`keystore/release.keystore` 与 `keystore/release.properties` 被 gitignore 挡住，
  只留公开的 `debug.keystore` 和一份 [keystore/release.properties.example](keystore/release.properties.example)
