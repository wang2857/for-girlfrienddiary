package com.synn.diary.ui.lock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.synn.diary.ui.components.DoodleDialog
import com.synn.diary.ui.components.DoodleTextButton

/**
 * 看恢复码之前先过一道 4 位密码。
 *
 * 恢复码相当于"万能钥匙"，放在能随手打开的地方等于把锁拆了；
 * 但完全不让人看，抄丢的那一天就只能清数据。
 */
@Composable
fun VerifyPinDialog(
    title: String = "输入密码",
    description: String = "看恢复码之前，先确认是你本人。",
    verifyPin: (String) -> Boolean,
    onVerified: () -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    DoodleDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter { c -> c.isDigit() }.take(4)
                        error = false
                    },
                    label = { Text("密码") },
                    singleLine = true,
                    isError = error,
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
            }
        },
        confirmButton = {
            DoodleTextButton(
                enabled = pin.length == 4,
                onClick = {
                    if (verifyPin(pin)) {
                        onVerified()
                    } else {
                        error = true
                        pin = ""
                    }
                },
            ) { Text("确认") }
        },
        dismissButton = { DoodleTextButton(onClick = onDismiss) { Text("取消") } },
    )
}
