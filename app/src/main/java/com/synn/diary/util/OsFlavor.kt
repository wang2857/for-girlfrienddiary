package com.synn.diary.util

import android.os.Build

/**
 * 判断当前是不是 vivo / iQOO 的 OriginOS 7 及以上。
 *
 * OriginOS 7 的"液态玻璃"是系统级材质，没有对第三方开放的绘制接口；
 * 我们能用的是平台自己的窗口模糊（[android.view.Window.setBackgroundBlurRadius]，API 31+），
 * 效果接近"毛玻璃对话框"，但不是系统那套像素级透彩。
 */
object OsFlavor {

    val isVivo: Boolean by lazy {
        // 单元测试跑在 JVM 上时 Build.BRAND 可能是 null
        val brand = Build.BRAND?.lowercase().orEmpty()
        val manufacturer = Build.MANUFACTURER?.lowercase().orEmpty()
        brand == "vivo" || brand == "iqoo" || manufacturer == "vivo"
    }

    /** 形如 "7.0" / "6.1"；读不到就 null。 */
    val originOsVersion: String? by lazy {
        runCatching {
            val clazz = Class.forName("android.os.SystemProperties")
            val get = clazz.getMethod("get", String::class.java)
            (get.invoke(null, "ro.vivo.os.version") as? String)?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private val majorVersion: Int?
        get() = originOsVersion?.substringBefore('.')?.toIntOrNull()

    /** OriginOS 7+：才默认打开玻璃效果。 */
    val supportsLiquidGlass: Boolean by lazy {
        isVivo && (majorVersion ?: 0) >= 7
    }
}
