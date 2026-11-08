package com.synn.diary.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synn.diary.data.lock.LockManager
import com.synn.diary.ui.lock.biometricAvailable

private const val PIN_LENGTH = 4

private enum class Step { WELCOME, SET_PIN, CONFIRM, FINGERPRINT }

/**
 * 首次安装的三步引导：说明 → 设密码 → 确认。
 * 密码不是强制的（可以"以后再说"），但默认路径是让你设一个——
 * 日记只有这份数据，忘密码的代价比设密码的麻烦大得多。
 */
@Composable
fun OnboardingScreen(
    lockManager: LockManager,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(Step.WELCOME) }
    var firstPin by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun handleComplete(pin: String) {
        when (step) {
            Step.SET_PIN -> {
                firstPin = pin
                input = ""
                error = null
                step = Step.CONFIRM
            }
            Step.CONFIRM -> {
                if (pin == firstPin) {
                    lockManager.setPin(pin)
                    if (biometricAvailable(context)) {
                        step = Step.FINGERPRINT
                    } else {
                        com.synn.diary.data.Onboarding.markDone(context)
                        lockManager.unlock()
                        onFinish()
                    }
                } else {
                    error = "两次输入不一致，再来一次"
                    firstPin = ""
                    input = ""
                    step = Step.SET_PIN
                }
            }
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 30.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (step) {
            Step.WELCOME -> WelcomeStep(
                onStart = { step = Step.SET_PIN; input = ""; error = null },
                onSkip = {
                    com.synn.diary.data.Onboarding.markDone(context)
                    onFinish()
                },
            )

            Step.SET_PIN, Step.CONFIRM -> PinStep(
                title = if (step == Step.SET_PIN) "设置 4 位密码" else "再输一次",
                subtitle = error ?: if (step == Step.SET_PIN) {
                    "打开日记时要输的密码，别忘"
                } else {
                    "确认一下刚才那 4 位"
                },
                isError = error != null,
                input = input,
                onDigit = { digit ->
                    if (input.length < PIN_LENGTH) {
                        input += digit
                        if (input.length == PIN_LENGTH) handleComplete(input)
                    }
                },
                onDelete = { if (input.isNotEmpty()) input = input.dropLast(1) },
            )

            Step.FINGERPRINT -> FingerprintStep(
                onEnable = {
                    lockManager.setBiometricEnabled(true)
                    lockManager.unlock()
                    com.synn.diary.data.Onboarding.markDone(context)
                    onFinish()
                },
                onSkip = {
                    lockManager.unlock()
                    com.synn.diary.data.Onboarding.markDone(context)
                    onFinish()
                },
            )
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit, onSkip: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "📔", fontSize = 52.sp)
        Spacer(Modifier.height(18.dp))
        Text(
            text = "开始之前",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "日记只存在这台手机里，不上传、不联网。\n建议给应用设一个 4 位密码——\n只有你知道。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
        )
        Spacer(Modifier.height(36.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onStart)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "设置密码",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onSkip) {
            Text("以后再说", color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun PinStep(
    title: String,
    subtitle: String,
    isError: Boolean,
    input: String,
    modifier: Modifier = Modifier,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(text = "🔒", fontSize = 36.sp)
        Spacer(Modifier.height(10.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(PIN_LENGTH) { index ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < input.length) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9").chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { key -> OnboardKey(key) { onDigit(key) } }
            }
            Spacer(Modifier.height(14.dp))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(64.dp))
            OnboardKey("0") { onDigit("0") }
            OnboardKey("⌫", small = true, onClick = onDelete)
        }
    }
}

@Composable
private fun FingerprintStep(onEnable: () -> Unit, onSkip: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "👆", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(text = "顺便开指纹解锁？", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "开了以后可以用指纹代替输密码。\n密码仍然有效，随时能切回来。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onEnable)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "开启指纹",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onSkip) {
            Text("只用密码", color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun OnboardKey(label: String, small: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(if (small) 64.dp else 72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = if (small) 15.sp else 24.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
