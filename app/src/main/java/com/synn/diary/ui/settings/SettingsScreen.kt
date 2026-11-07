package com.synn.diary.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synn.diary.data.lock.LockManager
import com.synn.diary.ui.components.DiaryBottomBar
import com.synn.diary.ui.lock.biometricAvailable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    lockManager: LockManager,
    onDisableLock: () -> Unit,
    onOpenTrash: () -> Unit,
    onSelectTab: (String) -> Unit,
) {
    val context = LocalContext.current
    val lockEnabled by lockManager.enabled.collectAsStateWithLifecycle()

    var showSetPin by remember { mutableStateOf(false) }
    var showDisableConfirm by remember { mutableStateOf(false) }
    var biometric by remember { mutableStateOf(lockManager.biometricEnabled) }
    val canBiometric = remember { biometricAvailable(context) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }) },
        bottomBar = { DiaryBottomBar(currentRoute = "settings", onSelect = onSelectTab) },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "安全",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))

            SettingSwitchRow(
                title = "应用锁",
                subtitle = "离开应用后需要密码才能打开",
                checked = lockEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        if (lockManager.hasPin()) lockManager.enableLock() else showSetPin = true
                    } else {
                        showDisableConfirm = true
                    }
                },
            )

            if (lockEnabled) {
                SettingActionRow(
                    title = "修改密码",
                    subtitle = "4 位数字",
                    onClick = { showSetPin = true },
                )
                val allowShots by lockManager.allowScreenshots.collectAsStateWithLifecycle()
                SettingSwitchRow(
                    title = "允许截屏",
                    subtitle = if (allowShots) "当前可以截屏 / 录屏" else "截屏与录屏被禁止，最近任务里也不显示内容",
                    checked = allowShots,
                    onCheckedChange = { lockManager.setAllowScreenshots(it) },
                )
                SettingSwitchRow(
                    title = "指纹解锁",
                    subtitle = if (canBiometric) "用系统指纹快速解锁" else "此设备不支持指纹",
                    checked = biometric && canBiometric,
                    enabled = canBiometric,
                    onCheckedChange = {
                        biometric = it
                        lockManager.setBiometricEnabled(it)
                    },
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "数据",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            SettingActionRow(
                title = "回收站",
                subtitle = "删除的日记会留在这里，可以还原",
                actionLabel = "查看",
                onClick = onOpenTrash,
            )

            Spacer(Modifier.height(24.dp))
            Text(
                text = "关于",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "日记 0.24.4 · 本地存储 · 不上传",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showSetPin) {
        PinDialog(
            onDismiss = { showSetPin = false },
            onConfirm = { pin ->
                lockManager.setPin(pin)
                showSetPin = false
            },
        )
    }

    if (showDisableConfirm) {
        AlertDialog(
            onDismissRequest = { showDisableConfirm = false },
            title = { Text("关闭应用锁？") },
            text = { Text("关闭后打开应用不再需要密码，截屏保护一并取消；已上锁的日记也会全部解锁——没有密码就没法再验证它们。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDisableLock()
                        biometric = false
                        showDisableConfirm = false
                    },
                ) { Text("关闭") }
            },
            dismissButton = {
                TextButton(onClick = { showDisableConfirm = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun SettingActionRow(
    title: String,
    subtitle: String,
    actionLabel: String = "设置",
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 整行可点，不只是右边那个按钮
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        TextButton(onClick = onClick) { Text(actionLabel) }
    }
}

@Composable
private fun PinDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置 4 位密码") },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = { input -> pin = input.filter { it.isDigit() }.take(4) },
                singleLine = true,
                label = { Text("密码") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(pin) },
                enabled = pin.length == 4,
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
