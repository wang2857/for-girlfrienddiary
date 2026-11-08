# 数据库格式（v7，SQLCipher 加密）

这页写给"三个月后想用 sqlite3 直接查点什么"的自己。所有内容都对着设备上真实的库核对过。

## 1. 文件在哪

```
/data/data/com.synn.diary.debug/databases/diary.db      ← 主库（SQLCipher 加密，v0.33.0 起）
                                        diary.db-wal
                                        diary.db-shm
/data/data/com.synn.diary.debug/shared_prefs/db_security.xml  ← 被 Keystore 包裹的口令
/data/data/com.synn.diary.debug/files/images/           ← 正文图片
/data/data/com.synn.diary.debug/files/backgrounds/      ← 自定义纸张
/data/data/com.synn.diary.debug/files/backup/           ← 自动备份（每天一份）
/data/data/com.synn.diary.debug/files/crash/            ← 崩溃日志
```

拉下来看：

```bash
adb shell "run-as com.synn.diary.debug cat databases/diary.db"     > diary.db
adb shell "run-as com.synn.diary.debug cat databases/diary.db-wal" > diary.db-wal
sqlite3 diary.db ".schema"
```

**`-wal` 必须一起拿**：最近写的内容还在 WAL 里，只拿主库会看到旧数据。

**但 0.33.0 起拿到的都是密文**：文件头不再是 `SQLite format 3`，`sqlite3 diary.db` 会报
`file is not a database`。要读内容只有两条路：

1. 用应用自己的导出功能拿到 zip（里面还是加密库，只能在**同一台设备、同一次安装**里导入）
2. 在设备上用 SQLCipher 命令行 + 口令打开——口令是随机 32 字节，被 AndroidKeyStore 的
   AES-256-GCM 裹在 `db_security.xml` 里，**取不出来**（这正是加密的意义）

想做临时排查，可以把 `DbKey` 改成打印口令的调试版，排查完撤掉。

## 2. 单位约定

| 列 | 单位 | 例子 |
|---|---|---|
| `entryDate` | **epoch 天数**（1970-01-01 起第几天） | `20765` = 2026-11-08 |
| `createdAt` / `updatedAt` / `deletedAt` | **毫秒**时间戳 | `1794125838456` |
| `tag.createdAt` | 毫秒时间戳 | `1793667600000` |
| `latitude` / `longitude` | 十进制度 | `30.2883` / `120.1624` |

最容易踩的坑：`date(entryDate,'unixepoch')` 会得到 1970。要么用天数，要么写
`date(entryDate * 86400, 'unixepoch')`。查"某天"直接比数字更省心：

```sql
SELECT * FROM entry WHERE entryDate = 20765;
SELECT * FROM entry WHERE entryDate >= 20759 AND entryDate <= 20765;   -- 最近一周
```

## 3. 表结构

### entry

```sql
CREATE TABLE `entry` (
  `id`           INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  `uuid`         TEXT    NOT NULL,                 -- 合并导入去重键
  `entryDate`    INTEGER NOT NULL,                 -- 归属日，epochDay
  `createdAt`    INTEGER NOT NULL,                 -- 毫秒
  `updatedAt`    INTEGER NOT NULL,                 -- 毫秒
  `title`        TEXT    NOT NULL,
  `body`         TEXT    NOT NULL,                 -- 纯文本 + 图片标记
  `mood`         TEXT,                             -- 存 emoji
  `deletedAt`    INTEGER,                          -- 非空 = 在回收站
  `weatherText`  TEXT,                             -- 如「小雨 15°」
  `locationText` TEXT,                             -- 如「30.29°N 120.16°E」
  `latitude`     REAL,
  `longitude`    REAL,
  `background`   TEXT,                             -- 纸张，见下
  `favorite`     INTEGER NOT NULL DEFAULT 0,
  `locked`       INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX        `index_entry_entryDate` ON `entry` (`entryDate`);
CREATE UNIQUE INDEX `index_entry_uuid`      ON `entry` (`uuid`);
```

`background` 的取值：

- `default` / `cream` / `mint` / `peach` / `sakura` / `sky` / `butter` / `grid`
- `img:<文件名>` —— 自定义图片纸，文件在 `files/backgrounds/<文件名>`
- `NULL` 等价于 `default`

`mood` 存的是 emoji 本身（`😔 🙂 😐 😄` 等 8 种），不是枚举 id，所以加心情不需要迁移。

`locked = 1` 只在"应用锁开着且有密码"时才可能成立；关掉应用锁时启动逻辑会把它们全部解成 0。

### tag / entry_tag

```sql
CREATE TABLE `tag` (
  `id`        INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  `name`      TEXT NOT NULL,
  `createdAt` INTEGER NOT NULL
);
CREATE UNIQUE INDEX `index_tag_name` ON `tag` (`name`);

CREATE TABLE `entry_tag` (
  `entryId` INTEGER NOT NULL,
  `tagId`   INTEGER NOT NULL,
  PRIMARY KEY(`entryId`, `tagId`),
  FOREIGN KEY(`entryId`) REFERENCES `entry`(`id`) ON DELETE CASCADE,
  FOREIGN KEY(`tagId`)   REFERENCES `tag`(`id`)   ON DELETE CASCADE
);
```

### attachment：v7 已删除

v3 加过一张 `attachment` 表记录"某条日记用过哪些图"，但从来没有读取方——
图片归属一直由正文里的标记决定，孤儿清理扫的是目录。留着只会和真实状态对不上，
所以 **v6 → v7 的迁移直接 `DROP TABLE attachment`**。

## 4. 正文与图片

正文是纯文本，图片是一个标记：

```
今天很好。[[img:335a4b32-…-f385.jpg|100|c]]晚上散步。
```

- 文件名限制 `[A-Za-z0-9._-]`（防止路径穿越）
- 宽度百分比 20–100，对齐 `l` / `c` / `r`
- 老格式 `[[img:文件名]]` 仍可解析（满宽居中）

图片本体在 `files/images/`，**不在库里**。删条目时先删文件再删行。

## 5. 备份包格式

```
日记备份-<时间戳>.zip
├── manifest.json     { exportedAt, schemaVersion, entryCount, imageCount, backgroundCount }
├── diary.db          整库文件
├── images/           正文图片
└── backgrounds/      自定义纸张
```

两种导入方式：

| 方式 | 行为 | 是否需要重启 |
|---|---|---|
| 覆盖导入 | 关库 → 整个 db 文件替换 → images/backgrounds 全量替换 → 重启应用 | 要 |
| 合并导入 | 逐条读备份里的 entry，按 `uuid` 去重后插入；标签按名字复用；缺的图片/纸张才补 | 不用 |

合并导入的细节：

- 备份里已在本机存在（同 `uuid`）的跳过
- **回收站里的跳过**（不把有意删掉的东西又捞回来）
- 标签按 `name` 找，没有就新建
- 图片文件名是随机 UUID，同名即同图，本地已有就不重复拷
- 全部写在一个 Room 事务里；失败整批回滚

## 6. 迁移链

| 版本 | 改了什么 |
|---|---|
| 1 → 2 | 新增 `tag`、`entry_tag` |
| 2 → 3 | `entry` 加天气、地点、经纬度四列 |
| 3 → 4 | 新增 `attachment` |
| 4 → 5 | `entry` 加 `background` |
| 5 → 6 | `entry` 加 `favorite`、`locked` |
| 6 → 7 | 删除 `attachment` |

每步都在真机上验过。`app/schemas/<版本>.json` 是 Room 编译期导出的权威结构，
改字段必须同时写迁移，否则启动直接崩。

## 7. 几个常用查询

```sql
-- 每个月写了多少篇（不含回收站、不含空壳）
SELECT strftime('%Y-%m', entryDate * 86400, 'unixepoch') AS month, count(*)
FROM entry
WHERE deletedAt IS NULL AND (length(trim(title)) > 0 OR length(trim(body)) > 0)
GROUP BY month ORDER BY month;

-- 带标签的日记
SELECT e.id, e.title, group_concat(t.name, ' / ')
FROM entry e JOIN entry_tag et ON et.entryId = e.id JOIN tag t ON t.id = et.tagId
WHERE e.deletedAt IS NULL GROUP BY e.id;

-- 回收站里最老的几条
SELECT id, title, datetime(deletedAt/1000,'unixepoch','+8 hours') AS deleted
FROM entry WHERE deletedAt IS NOT NULL ORDER BY deletedAt ASC LIMIT 10;
```

## 8. 加密与迁移

- 口令：随机 32 字节 → hex → 用 Keystore AES-GCM 封装后存 `db_security.xml`；
  **不跟 4 位 PIN 绑定**（PIN 只有 1 万种，派生密钥等于没加密）
- **不再兼容明文库**：启动时如果发现 `databases/diary.db` 是明文（文件头是 `SQLite format 3`），
  直接删掉（连 `-wal/-shm`），让 Room 重新建一个加密库。0.33.0 之前的明文数据按产品决定放弃
- 备份只接受加密库：导入/合并时如果备份里的 `diary.db` 是明文，直接报
  "这是旧版本导出的明文备份，当前版本只接受加密备份"，不会替换任何数据
- 备份包里的库就是加密库本身：**换机 / 重装后口令丢失，备份也打不开**
- 搜索是 `LIKE '%关键词%'`，数据量大再换 FTS5

## 9. 已知

- 备份的可移植性问题（见 §8）：想要"换机也能恢复"，得把口令用恢复码再包一层带进备份，还没做
