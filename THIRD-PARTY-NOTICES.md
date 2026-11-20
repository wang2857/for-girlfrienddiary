# 第三方组件与许可

本项目（日记 / Diary，包名 `com.synn.diary`）以 **MIT 许可证**发布，全文见 [LICENSE](LICENSE)。

下面列出随 APK 一起分发的第三方组件。它们各自的许可证优先于本项目的 MIT 许可证适用于对应部分。
应用内同一份内容在 **设置 → 关于 → 开源许可** 里也能看到（`app/src/main/res/raw/licenses.txt`）。

## 运行时依赖

| 组件 | 版本 | 许可证 |
|---|---|---|
| AndroidX Core (`androidx.core:core-ktx`) | 1.17.0 | Apache License 2.0 |
| AndroidX Activity Compose | 1.11.0 | Apache License 2.0 |
| AndroidX Lifecycle (runtime / viewmodel-compose) | 2.9.4 | Apache License 2.0 |
| AndroidX Navigation Compose | 2.9.8 | Apache License 2.0 |
| Jetpack Compose（UI / Foundation / Material3 / Material Icons，BOM 2025.10.01） | 1.9.x | Apache License 2.0 |
| AndroidX Room（runtime / ktx） | 2.7.2 | Apache License 2.0 |
| AndroidX WorkManager | 2.10.5 | Apache License 2.0 |
| AndroidX Biometric | 1.1.0 | Apache License 2.0 |
| AndroidX ExifInterface | 1.4.2 | Apache License 2.0 |
| Kotlin 标准库 / kotlinx.coroutines | 2.2.21 / 1.10.2 | Apache License 2.0 |
| Coil 3（图片加载） | 3.1.0 | Apache License 2.0 |
| SQLCipher for Android（数据库加密，Zetetic LLC） | 4.6.1 | BSD 3-Clause |

AndroidX / Jetpack / Compose 各库的许可文本随 APK 一起打包在
`META-INF/androidx/<group>/<artifact>/LICENSE.txt`，解包即可查看。

## 字体

| 字体 | 来源 | 许可证 |
|---|---|---|
| 霞鹜文楷 LXGW WenKai (`app/src/main/res/font/lxgw_wenkai.ttf`) | [lxgw/LxgwWenKai](https://github.com/lxgw/LxgwWenKai) | SIL Open Font License 1.1 |

```
Copyright 2021-2026 LXGW (https://github.com/lxgw/LxgwWenKai), with Reserved Font Name '霞鹜', '霞鶩', '落霞孤鹜', '落霞孤鶩' and 'LXGW'.
Copyright 2020 The Klee Project Authors (https://github.com/fontworks-fonts/Klee)

This Font Software is licensed under the SIL Open Font License, Version 1.1.
```

OFL 1.1 全文见 [LICENSE-OFL.txt](LICENSE-OFL.txt)（应用内许可页也带全文）。

## 仅开发期使用（不随 APK 分发）

| 组件 | 版本 | 许可证 |
|---|---|---|
| JUnit 4 | 4.13.2 | Eclipse Public License 1.0 |
| Robolectric | 4.14.1 | MIT |
| AndroidX Test (core / runner / ext-junit) | — | Apache License 2.0 |
| Gradle / Android Gradle Plugin / Kotlin 编译器 | — | Apache License 2.0 |

## 说明

- 你在应用里写的文字、导入的图片和字体属于**你自己**，不在上述任何许可范围内，本项目不主张任何权利。
- 应用导出的备份（zip）只包含你的数据与应用生成的元数据。
- 修改或再分发本项目的衍生作品时，请保留本目录下的 `LICENSE`、`THIRD-PARTY-NOTICES.md`、
  `LICENSE-OFL.txt`，并按 OFL 1.1 的要求保留字体的版权声明与保留字体名（Reserved Font Name）限制。
