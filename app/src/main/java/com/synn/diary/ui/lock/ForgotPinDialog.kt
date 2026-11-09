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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.synn.diary.ui.components.DoodleDialog
import com.synn.diary.ui.components.DoodleTextButton

/**
 * 忘密码时的唯一出路：输恢复码 → 设新密码。
 *
 * 先验码再让输新密码，是因为"输完两遍 4 位才发现恢复码抄错"很难受。
 */
@Composable
fun ForgotPinDialog(
    verifyCode: (String) -> Boolean,
    onRecover: (String, String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var onNewPin by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    DoodleDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (onNewPin) "设置新密码" else "用恢复码重置") },
        text = {
            Column {
                Text(
                    text = if (onNewPin) {
                        "恢复码对了，重新设一个 4 位密码。"
                    } else {
                        "设密码时抄下来的那 16 位，4 组、每组 4 个，大小写和空格都不用管。"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                if (onNewPin) {
                    PinField(
                        label = "新密码",
                        value = pin1,
                        onValueChange = {
                            pin1 = it
                            error = null
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    PinField(
                        label = "再输一次",
                        value = pin2,
                        onValueChange = {
                            pin2 = it
                            error = null
                        },
                    )
                } else {
                    OutlinedTextField(
                        value = code,
                        onValueChange = {
                            code = it.take(32)
                            error = null
                        },
                        label = { Text("恢复码") },
                        singleLine = true,
                        isError = error != null,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Ascii,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            DoodleTextButton(
                enabled = if (onNewPin) pin1.length == 4 && pin2.length == 4 else code.isNotBlank(),
                onClick = {
                    if (!onNewPin) {
                        if (verifyCode(code)) {
                            onNewPin = true
                            error = null
                        } else {
                            error = "恢复码不对"
                        }
                    } else if (pin1 != pin2) {
                        error = "两次不一致"
                        pin1 = ""
                        pin2 = ""
                    } else if (!onRecover(code, pin1)) {
                        error = "恢复码不对"
                        onNewPin = false
                    }
                    // onRecover 成功时 locked 变 false，整个锁屏会被换掉，不用收尾
                },
            ) {
                Text(if (onNewPin) "重置并进入" else "下一步")
            }
        },
        dismissButton = { DoodleTextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun PinField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() }.take(4)) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
    )
}
