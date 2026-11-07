package com.synn.diary.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private enum class UnlockMode { FINGERPRINT, PIN }

/**
 * 单篇日记的解锁框。
 *
 * 指纹和密码是**两种模式**，同一时刻只显示一种，互不叠加；
 * 底部有「用密码 / 用指纹」按钮随时切换。
 * 凭证复用应用锁的 4 位密码。
 */
@Composable
fun UnlockEntryDialog(
    title: String = "这篇日记已上锁",
    verifyPin: (String) -> Boolean,
    biometricEnabled: Boolean,
    onUnlocked: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val canUseBiometric = biometricEnabled && activity != null && biometricAvailable(context)

    var mode by remember(canUseBiometric) {
        mutableStateOf(if (canUseBiometric) UnlockMode.FINGERPRINT else UnlockMode.PIN)
    }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    val prompt = remember(activity) {
        activity?.let { fragmentActivity ->
            BiometricPrompt(
                fragmentActivity,
                ContextCompat.getMainExecutor(fragmentActivity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onUnlocked()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        when (errorCode) {
                            BiometricPrompt.ERROR_LOCKOUT,
                            BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
                            -> {
                                error = true
                                mode = UnlockMode.PIN
                            }
                            // 用户主动取消：留在当前界面，不做处理
                            else -> Unit
                        }
                    }
                },
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose { runCatching { prompt?.cancelAuthentication() } }
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("解锁这篇日记")
            .setNegativeButtonText("取消")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setConfirmationRequired(false)
            .build()
    }

    fun submit() {
        if (verifyPin(pin)) onUnlocked() else {
            error = true
            pin = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (mode == UnlockMode.FINGERPRINT) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .clickable {
                                error = false
                                runCatching { prompt?.authenticate(promptInfo) }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("👆", fontSize = 40.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (error) "指纹识别失败，可以改用密码" else "触摸指纹解锁",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (error) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { error = false; mode = UnlockMode.PIN },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("用密码解锁")
                    }
                }
            } else {
                Column {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { input ->
                            pin = input.filter { it.isDigit() }.take(4)
                            error = false
                        },
                        singleLine = true,
                        label = { Text("输入 4 位密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (error) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "密码不对",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (canUseBiometric) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { error = false; pin = ""; mode = UnlockMode.FINGERPRINT },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("用指纹解锁")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (mode == UnlockMode.PIN) {
                TextButton(onClick = { submit() }, enabled = pin.length == 4) { Text("解锁") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
