package com.synn.diary.ui.settings

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.widget.Toast
import com.synn.diary.data.AppContainer
import com.synn.diary.data.backup.AutoBackup
import com.synn.diary.data.backup.AutoBackupScheduler
import com.synn.diary.notify.DailyReminder
import com.synn.diary.notify.ReminderScheduler
import com.synn.diary.ui.lock.RecoveryCodeDialog
import com.synn.diary.ui.lock.VerifyPinDialog
import com.synn.diary.util.CrashLog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synn.diary.ui.components.DiaryBottomBar
import com.synn.diary.ui.lock.biometricAvailable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onDisableLock: () -> Unit,
    onOpenTrash: () -> Unit,
    onExport: (android.net.Uri, () -> Unit) -> Unit,
    onImport: (android.net.Uri, () -> Unit) -> Unit,
    onMergeImport: (android.net.Uri, () -> Unit) -> Unit,
    pendingImport: kotlinx.coroutines.flow.StateFlow<android.net.Uri?>,
    onPickImport: (android.net.Uri) -> Unit,
    onClearPendingImport: () -> Unit,
    onExternalUiStart: () -> Unit,
    onExternalUiEnd: () -> Unit,
    onSelectTab: (String) -> Unit,
) {
    val context = LocalContext.current
    val lockManager = container.lockManager
    val scope = rememberCoroutineScope()
    val lockEnabled by lockManager.enabled.collectAsStateWithLifecycle()
    var reminderOn by remember { mutableStateOf(DailyReminder.isEnabled(context)) }
    val notifyPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        onExternalUiEnd()
        if (granted) {
            ReminderScheduler.setEnabled(context, true)
            reminderOn = true
        } else {
            reminderOn = false
            Toast.makeText(context, "没有通知权限，提醒发不出来", Toast.LENGTH_LONG).show()
        }
    }

    var showSetPin by remember { mutableStateOf(false) }
    var showResetRecovery by remember { mutableStateOf(false) }
    var showRevealPin by remember { mutableStateOf(false) }
    var pendingRecoveryCode by remember { mutableStateOf<String?>(null) }
    var recoveryAgain by remember { mutableStateOf(false) }
    // 生成/重设之后要把这一行刷成「查看」，而 lockManager 不是可观察状态
    var hasRecovery by remember { mutableStateOf(lockManager.hasRecoveryCode()) }
    var canRevealRecovery by remember { mutableStateOf(lockManager.canRevealRecoveryCode()) }
    var showCrashLog by remember { mutableStateOf(false) }
    // 每次进设置页重新扫一遍：闪退之后重开应用，这里才拿得到上一次的记录
    var crashFiles by remember { mutableStateOf(CrashLog.files(context)) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        crashFiles = CrashLog.files(context)
    }

    var autoBackupOn by remember { mutableStateOf(AutoBackup.isEnabled(context)) }
    var autoBackupBusy by remember { mutableStateOf(false) }
    var lastBackupAt by remember { mutableStateOf(AutoBackup.lastRunAt(context)) }
    var lastBackupNote by remember { mutableStateOf(AutoBackup.lastNote(context)) }

    /** 备份可能跑几秒，统一走这个入口，顺便把界面上的时间刷一下。 */
    fun runBackupNow() {
        if (autoBackupBusy) return
        autoBackupBusy = true
        scope.launch {
            val result = AutoBackup.run(context, container)
            autoBackupBusy = false
            lastBackupAt = AutoBackup.lastRunAt(context)
            lastBackupNote = AutoBackup.lastNote(context)
            Toast.makeText(
                context,
                result.fold({ "已备份一份" }, { "备份失败：${it.message}" }),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    val pending by pendingImport.collectAsState()
    var busy by remember { mutableStateOf<String?>(null) }
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        onExternalUiEnd()
        if (uri != null) {
            busy = "正在导出…"
            // 导完把 busy 清掉，否则这一页会一直卡在"正在导出"，后面的按钮也点不动
            onExport(uri) { busy = null }
        }
    }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        onExternalUiEnd()
        if (uri != null) onPickImport(uri)
    }
    val mergeLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        onExternalUiEnd()
        if (uri != null) {
            busy = "正在合并…"
            onMergeImport(uri) { busy = null }
        }
    }

    // 选完文件先弹确认框（覆盖是破坏性操作），确认后才真正导入
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
                // 设置项已经超过一屏，不滚动的话最下面的"立即备份"和"崩溃日志"点不到
                .verticalScroll(rememberScrollState())
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
                SettingActionRow(
                    title = "恢复码",
                    subtitle = when {
                        !hasRecovery -> "还没生成，强烈建议现在设一个"
                        canRevealRecovery -> "忘了密码时用它重置，忘了抄可以再查看"
                        else -> "旧版本生成的，明文已经看不到了，可以重设"
                    },
                    actionLabel = when {
                        !hasRecovery -> "生成"
                        canRevealRecovery -> "查看"
                        else -> "重设"
                    },
                    onClick = {
                        when {
                            !hasRecovery -> {
                                recoveryAgain = false
                                pendingRecoveryCode = lockManager.resetRecoveryCode()
                                hasRecovery = true
                                canRevealRecovery = lockManager.canRevealRecoveryCode()
                            }
                            canRevealRecovery -> showRevealPin = true
                            else -> showResetRecovery = true
                        }
                    },
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
                text = "提醒",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            SettingSwitchRow(
                title = "每日提醒",
                subtitle = if (reminderOn) "每天 21:00，当天还没写才提醒一次" else "关着，不会打扰",
                checked = reminderOn,
                onCheckedChange = { checked ->
                    if (!checked) {
                        ReminderScheduler.setEnabled(context, false)
                        reminderOn = false
                    } else if (DailyReminder.canNotify(context)) {
                        ReminderScheduler.setEnabled(context, true)
                        reminderOn = true
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // 权限框也算"系统界面"：不挡着就上锁的话，回来直接被踢去锁屏
                        onExternalUiStart()
                        notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        Toast.makeText(context, "系统里这个应用的通知被关掉了，先在系统设置里打开", Toast.LENGTH_LONG).show()
                    }
                },
            )
            if (reminderOn) {
                SettingActionRow(
                    title = "试一下提醒",
                    subtitle = "立刻发一条，看看长什么样",
                    actionLabel = "发送",
                    onClick = {
                        if (DailyReminder.canNotify(context)) {
                            DailyReminder.notify(context)
                        } else {
                            Toast.makeText(context, "没有通知权限", Toast.LENGTH_SHORT).show()
                        }
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

            SettingSwitchRow(
                title = "自动备份",
                subtitle = if (autoBackupOn) {
                    val when_ = if (lastBackupAt > 0) "上次 ${formatShortTime(lastBackupAt)}" else "还没跑过"
                    val note = if (lastBackupNote != null && lastBackupNote != "成功") "（${lastBackupNote}）" else ""
                    "每天 3:00 存一份，留最近 ${AutoBackup.MAX_COPIES} 份 · $when_$note"
                } else {
                    "关着，想留底就手动导出"
                },
                checked = autoBackupOn,
                enabled = !autoBackupBusy,
                onCheckedChange = { checked ->
                    autoBackupOn = checked
                    AutoBackupScheduler.setEnabled(context, checked)
                    // 打开时立刻存一份，别等到明天凌晨还不知道有没有生效
                    if (checked) runBackupNow()
                },
            )

            if (autoBackupOn) {
                SettingActionRow(
                    title = "立即备份",
                    subtitle = if (autoBackupBusy) "正在打包…" else "现在存一份，不影响每天那次",
                    actionLabel = if (autoBackupBusy) "…" else "备份",
                    onClick = { runBackupNow() },
                )
            }

            SettingActionRow(
                title = "导出备份",
                subtitle = if (busy == "正在导出…") "正在导出…" else "把日记和图片打包成一个 zip",
                actionLabel = "导出",
                onClick = {
                    if (busy == null) {
                        onExternalUiStart()
                        exportLauncher.launch("日记备份-${System.currentTimeMillis()}.zip")
                    }
                },
            )

            SettingActionRow(
                title = "导入备份",
                subtitle = "用备份包覆盖本机数据（会替换现有内容）",
                actionLabel = "导入",
                onClick = {
                    if (busy == null) {
                        onExternalUiStart()
                        importLauncher.launch(arrayOf("application/zip"))
                    }
                },
            )

            SettingActionRow(
                title = "合并导入",
                subtitle = if (busy == "正在合并…") "正在合并…" else "只把备份里没有的日记加进来，不删本机的东西",
                actionLabel = "合并",
                onClick = {
                    if (busy == null) {
                        onExternalUiStart()
                        mergeLauncher.launch(arrayOf("application/zip"))
                    }
                },
            )

            Spacer(Modifier.height(24.dp))
            Text(
                text = "关于",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "日记 ${appVersion(context)} · 本地存储 · 不上传",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SettingActionRow(
                title = "崩溃日志",
                subtitle = if (crashFiles.isEmpty()) {
                    "没有闪退记录"
                } else {
                    "${crashFiles.size} 份 · 最近 ${formatShortTime(crashFiles.first().lastModified())}"
                },
                actionLabel = "查看",
                onClick = {
                    if (crashFiles.isEmpty()) {
                        Toast.makeText(context, "还没有闪退记录", Toast.LENGTH_SHORT).show()
                    } else {
                        showCrashLog = true
                    }
                },
            )
        }
    }

    if (showSetPin) {
        PinDialog(
            onDismiss = { showSetPin = false },
            onConfirm = { pin ->
                lockManager.setPin(pin, lockNow = false)
                showSetPin = false
                // 引导里选了「以后再说」的人：第一次设密码时补发恢复码
                lockManager.ensureRecoveryCode()?.let { code ->
                    recoveryAgain = false
                    pendingRecoveryCode = code
                    hasRecovery = true
                    canRevealRecovery = lockManager.canRevealRecoveryCode()
                }
            },
        )
    }

    pending?.let { uri ->
        AlertDialog(
            onDismissRequest = onClearPendingImport,
            title = { Text("用备份覆盖本机数据？") },
            text = { Text("现有的日记和图片会被备份包里的内容替换，且无法撤销。应用会自动重启。") },
            confirmButton = {
                TextButton(onClick = {
                    onClearPendingImport()
                    busy = "正在导入…"
                    // 成功会重启应用；失败时必须把 busy 清掉，否则整页按钮全被卡住
                    onImport(uri) { busy = null }
                }) { Text("覆盖导入") }
            },
            dismissButton = {
                TextButton(onClick = onClearPendingImport) { Text("取消") }
            },
        )
    }

    if (showResetRecovery) {
        AlertDialog(
            onDismissRequest = { showResetRecovery = false },
            title = { Text("重设恢复码？") },
            text = { Text("旧的恢复码会立刻作废。如果之前那份还留着，记得划掉。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetRecovery = false
                        recoveryAgain = false
                        pendingRecoveryCode = lockManager.resetRecoveryCode()
                        hasRecovery = true
                        canRevealRecovery = lockManager.canRevealRecoveryCode()
                    },
                ) { Text("重设") }
            },
            dismissButton = {
                TextButton(onClick = { showResetRecovery = false }) { Text("取消") }
            },
        )
    }

    if (showRevealPin) {
        VerifyPinDialog(
            description = "看恢复码之前，先确认是你本人。",
            verifyPin = lockManager::verifyPin,
            onVerified = {
                showRevealPin = false
                val code = lockManager.revealRecoveryCode()
                if (code == null) {
                    Toast.makeText(context, "这条恢复码读不出来了，重设一个吧", Toast.LENGTH_LONG).show()
                } else {
                    recoveryAgain = true
                    pendingRecoveryCode = code
                }
            },
            onDismiss = { showRevealPin = false },
        )
    }

    pendingRecoveryCode?.let { code ->
        RecoveryCodeDialog(
            code = code,
            again = recoveryAgain,
            onDismiss = { pendingRecoveryCode = null },
            onReset = {
                recoveryAgain = false
                pendingRecoveryCode = lockManager.resetRecoveryCode()
                hasRecovery = true
                canRevealRecovery = lockManager.canRevealRecoveryCode()
            },
        )
    }

    if (showCrashLog) {
        CrashLogDialog(
            text = remember { CrashLog.files(context).firstOrNull()?.readText().orEmpty() },
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("崩溃日志", CrashLog.files(context).firstOrNull()?.readText().orEmpty()))
                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
            },
            onClear = {
                CrashLog.clear(context)
                crashFiles = CrashLog.files(context)
                showCrashLog = false
            },
            onDismiss = { showCrashLog = false },
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

@Composable
private fun CrashLogDialog(
    text: String,
    onCopy: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("崩溃日志") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = text.ifBlank { "（空）" },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        },
        confirmButton = { TextButton(onClick = onCopy) { Text("复制") } },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text("清空") }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        },
    )
}

private fun formatShortTime(millis: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.ROOT).format(Date(millis))

private fun appVersion(context: android.content.Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: "?"
