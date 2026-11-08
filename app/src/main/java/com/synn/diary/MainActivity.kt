package com.synn.diary

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.captionBarPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.synn.diary.ui.onboarding.OnboardingScreen
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synn.diary.ui.DiaryNavHost
import com.synn.diary.ui.lock.LockScreen
import com.synn.diary.ui.theme.DiaryTheme

// 用 FragmentActivity：androidx.biometric 的 BiometricPrompt 需要它
class MainActivity : FragmentActivity() {

    /**
     * 盖在所有内容之上的一块不透明挡板。
     *
     * 光把 locked 置 true 不够：切后台时系统记住的是"最后一帧"，
     * 等回来时先把那一帧（可能是正文/列表）贴出来，下一帧才轮到锁屏。
     * 所以在锁上的同一时刻把这块挡板显示出来，最后一帧就变成一块纯色。
     */
    private var privacyCover: View? = null

    /** 挡板已经盖上了、但还没上锁。 */
    private var coverArmed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as DiaryApp).container
        handleNotificationIntent(intent, container)
        installPrivacyCover()

        setContent {
            DiaryTheme {
                val locked by container.lockManager.locked.collectAsStateWithLifecycle()
                val lockEnabled by container.lockManager.enabled.collectAsStateWithLifecycle()
                val allowScreenshots by container.lockManager.allowScreenshots.collectAsStateWithLifecycle()
                // 首次安装先走引导（设密码），走完才进主界面
                var onboarded by remember { mutableStateOf(com.synn.diary.data.Onboarding.isDone(this@MainActivity)) }

                // 开关一改立刻生效，不用等下次回到前台
                LaunchedEffect(lockEnabled, allowScreenshots) {
                    if (lockEnabled && !allowScreenshots) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .captionBarPadding(),
                ) {
                    if (!onboarded) {
                    OnboardingScreen(
                        lockManager = container.lockManager,
                        onFinish = { onboarded = true },
                    )
                } else if (lockEnabled && locked) {
                        LockScreen(
                            biometricEnabled = container.lockManager.biometricEnabled,
                            verifyPin = container.lockManager::verifyPin,
                            verifyRecoveryCode = container.lockManager::verifyRecoveryCode,
                            onRecover = container.lockManager::recoverWithCode,
                            onUnlock = container.lockManager::unlock,
                            onShown = { hidePrivacyCover() },
                        )
                    } else {
                        DiaryNavHost(container = container)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent, (application as DiaryApp).container)
    }

    /** 每日提醒点进来时带 EXTRA_NEW_ENTRY，解锁后直接开一篇新日记。 */
    private fun handleNotificationIntent(
        intent: android.content.Intent?,
        container: com.synn.diary.data.AppContainer,
    ) {
        if (intent?.getBooleanExtra(com.synn.diary.notify.DailyReminder.EXTRA_NEW_ENTRY, false) == true) {
            container.pendingNewEntry.value = true
            intent.removeExtra(com.synn.diary.notify.DailyReminder.EXTRA_NEW_ENTRY)
        }
    }

    /**
     * onPause 先把画面盖住，onStop 再真正上锁。
     *
     * 只锁不上挡板不行：系统记住的是"最后一帧"，回来时先把那一帧（可能是正文）贴出来。
     * 只上挡板不锁也不行：写日记时弹个权限框再回来就被踢去锁屏，太烦。
     * 分成两步——paused 时盖住，真的切走了（onStop）才锁；
     * 中途就回来的（权限框、指纹框）在 onResume 里把挡板撤掉。
     */
    override fun onPause() {
        super.onPause()
        val container = (application as DiaryApp).container
        if (isChangingConfigurations || container.externalUiInProgress) return

        val lock = container.lockManager
        if (!lock.locked.value && lock.enabled.value && lock.hasPin()) {
            showPrivacyCover()
            coverArmed = true
        }
    }

    override fun onResume() {
        super.onResume()
        val container = (application as DiaryApp).container
        // 自愈：文件选择器 / 权限框如果没回调（进程被杀等），标记会一直挂着，
        // 那之后就再也不会自动上锁了
        container.externalUiInProgress = false

        // 没真的切走（权限弹窗 / 系统对话框）：锁没上，把挡板撤掉
        if (coverArmed && !container.lockManager.locked.value) hidePrivacyCover()
    }

    private fun installPrivacyCover() {
        val cover = View(this).apply {
            setBackgroundColor(windowBackgroundColor())
            isClickable = true
            isFocusable = true
            visibility = View.GONE
        }
        (window.decorView as? ViewGroup)?.addView(
            cover,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        privacyCover = cover
    }

    private fun showPrivacyCover() {
        privacyCover?.visibility = View.VISIBLE
        // 立刻请求重绘，别等到窗口都不可见了
        window.decorView.invalidate()
    }

    private fun hidePrivacyCover() {
        privacyCover?.visibility = View.GONE
        coverArmed = false
    }

    /** 挡板颜色跟窗口底色一致，不然会闪一下别的颜色。 */
    private fun windowBackgroundColor(): Int {
        val value = TypedValue()
        if (theme.resolveAttribute(android.R.attr.windowBackground, value, true)) {
            if (value.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT) {
                return value.data
            }
            val drawable = androidx.core.content.ContextCompat.getDrawable(this, value.resourceId)
            if (drawable is ColorDrawable) return drawable.color
        }
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        return if (night) Color.BLACK else Color.WHITE
    }

    override fun onStop() {
        super.onStop()
        // 切后台就上锁；改配置（我们声明了 configChanges）不算离开
        val container = (application as DiaryApp).container
        handleNotificationIntent(intent, container)
        // 正在选文件时切后台是"系统界面"导致的，不该上锁
        if (!isChangingConfigurations && !container.externalUiInProgress) {
            container.lockManager.lockIfEnabled()
        }
    }
}
