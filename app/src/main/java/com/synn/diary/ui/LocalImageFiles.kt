package com.synn.diary.ui

import androidx.compose.runtime.staticCompositionLocalOf
import java.io.File

/**
 * 图片文件名 → 本地文件。图片存在 App 私有目录，卡片、编辑器都要用，
 * 用一个 CompositionLocal 传下去，免得每个用到的地方都多一个参数。
 */
val LocalImageFiles = staticCompositionLocalOf<(String) -> File> { { File("") } }

/** 自定义纸张背景（存在 files/backgrounds/，和正文图片分开）。 */
val LocalBackgroundFiles = staticCompositionLocalOf<(String) -> File> { { File("") } }
