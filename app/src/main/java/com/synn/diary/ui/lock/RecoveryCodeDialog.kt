package com.synn.diary.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synn.diary.data.lock.RecoveryCode

/**
 * 显示恢复码。
 *
 * [again] 为 false 时是"刚生成、只此一次"；为 true 是"验过密码后再看一遍"，
 * 文案得改，否则用户会以为看第二次就把码弄没了。
 */
@Composable
fun RecoveryCodeDialog(
    code: String,
    onDismiss: () -> Unit,
    again: Boolean = false,
    onReset: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (again) "你的恢复码" else "抄下这串恢复码") },
        text = {
            Column {
                Text(
                    text = if (again) {
                        "忘了密码时用它重置。关掉窗口它仍然有效，直到你重设为止。"
                    } else {
                        "忘记密码时，只有它能把日记救回来。只显示这一次。"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = RecoveryCode.format(code),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = 16.dp, horizontal = 8.dp),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "抄在纸上，或者记到这台手机以外的地方。丢了就只能清空数据重来。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(if (again) "关闭" else "我已抄好") }
        },
        dismissButton = {
            if (onReset != null) {
                TextButton(onClick = onReset) { Text("重设一个") }
            }
        },
    )
}
