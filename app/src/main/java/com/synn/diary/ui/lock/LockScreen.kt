package com.synn.diary.ui.lock

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private const val PIN_LENGTH = 4

@Composable
fun LockScreen(
    biometricEnabled: Boolean,
    verifyPin: (String) -> Boolean,
    verifyRecoveryCode: (String) -> Boolean,
    onRecover: (String, String) -> Boolean,
    onUnlock: () -> Unit,
    onShown: () -> Unit = {},
) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    var showForgot by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val canUseBiometric = biometricEnabled && activity != null && biometricAvailable(context)

    // 有指纹时优先走指纹：默认不显示密码键盘，点了"用密码"才切过去
    var usePin by remember(canUseBiometric) { mutableStateOf(!canUseBiometric) }

    val prompt = remember(activity) {
        activity?.let { fragmentActivity ->
            BiometricPrompt(
                fragmentActivity,
                ContextCompat.getMainExecutor(fragmentActivity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onUnlock()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        when (errorCode) {
                            // 用户点了系统弹窗里的"用密码"，或者主动取消
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_CANCELED,
                            -> usePin = true

                            BiometricPrompt.ERROR_LOCKOUT,
                            BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
                            -> {
                                hint = "指纹尝试次数过多，请用密码"
                                usePin = true
                            }

                            BiometricPrompt.ERROR_NO_BIOMETRICS -> hint = "没有可用的指纹"
                            else -> Unit
                        }
                    }
                },
            )
        }
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("解锁日记")
            .setSubtitle("使用指纹")
            .setNegativeButtonText("用密码")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setConfirmationRequired(false)
            .build()
    }

    fun startBiometric() {
        if (!canUseBiometric) return
        hint = null
        runCatching { prompt?.authenticate(promptInfo) }
    }

    // 锁屏已经画出来了，可以把上锁时盖的挡板撤掉（否则会挡住锁屏自己）
    LaunchedEffect(Unit) { onShown() }

    // 锁屏一出现就拉指纹，用户不用多点一次
    LaunchedEffect(canUseBiometric) {
        if (canUseBiometric) startBiometric()
    }
    DisposableEffect(Unit) {
        onDispose { runCatching { prompt?.cancelAuthentication() } }
    }

    fun submit() {
        if (verifyPin(input)) onUnlock() else {
            error = true
            input = ""
        }
    }

    fun append(digit: String) {
        if (input.length >= PIN_LENGTH) return
        error = false
        hint = null
        input += digit
        if (input.length == PIN_LENGTH) submit()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (canUseBiometric && !usePin) {
            FingerprintPanel(
                hint = hint,
                onRetry = { startBiometric() },
                onUsePin = {
                    runCatching { prompt?.cancelAuthentication() }
                    usePin = true
                },
            )
        } else {
            PinPanel(
                input = input,
                error = error,
                hint = hint,
                canUseBiometric = canUseBiometric,
                onDigit = ::append,
                onDelete = { if (input.isNotEmpty()) input = input.dropLast(1) },
                onBiometric = {
                    usePin = false
                    startBiometric()
                },
                onForgot = { showForgot = true },
            )
        }
    }

    if (showForgot) {
        ForgotPinDialog(
            verifyCode = verifyRecoveryCode,
            onRecover = onRecover,
            onDismiss = { showForgot = false },
        )
    }
}

@Composable
private fun FingerprintPanel(
    hint: String?,
    onRetry: () -> Unit,
    onUsePin: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onRetry),
            contentAlignment = Alignment.Center,
        ) {
            Text("👆", fontSize = 44.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text("触摸指纹解锁", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            text = hint ?: "也可以点下面的按钮改用密码",
            style = MaterialTheme.typography.bodySmall,
            color = if (hint != null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        TextButton(onClick = onUsePin) { Text("用密码") }
    }
}

@Composable
private fun PinPanel(
    input: String,
    error: Boolean,
    hint: String?,
    canUseBiometric: Boolean,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onBiometric: () -> Unit,
    onForgot: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🔒", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text("日记已锁定", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = hint ?: if (error) "密码不对，再试一次" else "输入 4 位密码",
            style = MaterialTheme.typography.bodySmall,
            color = if (error || hint != null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.outline,
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
                row.forEach { key -> KeyButton(key) { onDigit(key) } }
            }
            Spacer(Modifier.height(14.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (canUseBiometric) {
                KeyButton("指纹", small = true, onClick = onBiometric)
            } else {
                Spacer(Modifier.size(64.dp))
            }
            KeyButton("0") { onDigit("0") }
            KeyButton("⌫", small = true, onClick = onDelete)
        }

        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onForgot) {
            Text(
                text = "忘记密码？",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun KeyButton(label: String, small: Boolean = false, onClick: () -> Unit) {
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

fun biometricAvailable(context: Context): Boolean =
    BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
        BiometricManager.BIOMETRIC_SUCCESS
