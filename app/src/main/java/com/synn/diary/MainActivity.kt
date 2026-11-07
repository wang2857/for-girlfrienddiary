package com.synn.diary

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.captionBarPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synn.diary.ui.DiaryNavHost
import com.synn.diary.ui.lock.LockScreen
import com.synn.diary.ui.theme.DiaryTheme

// 用 FragmentActivity：androidx.biometric 的 BiometricPrompt 需要它
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as DiaryApp).container

        setContent {
            DiaryTheme {
                val locked by container.lockManager.locked.collectAsStateWithLifecycle()
                val lockEnabled by container.lockManager.enabled.collectAsStateWithLifecycle()
                val allowScreenshots by container.lockManager.allowScreenshots.collectAsStateWithLifecycle()

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
                    if (lockEnabled && locked) {
                        LockScreen(
                            biometricEnabled = container.lockManager.biometricEnabled,
                            verifyPin = container.lockManager::verifyPin,
                            onUnlock = container.lockManager::unlock,
                        )
                    } else {
                        DiaryNavHost(container = container)
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // 切后台就上锁；改配置（我们声明了 configChanges）不算离开
        if (!isChangingConfigurations) {
            (application as DiaryApp).container.lockManager.lockIfEnabled()
        }
    }
}
