package com.synn.diary.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.synn.diary.MainActivity
import com.synn.diary.R
import java.time.LocalDateTime
import java.time.LocalTime

/** 每日提醒的设置（开关 + 时间）与通知发送。 */
object DailyReminder {

    const val CHANNEL_ID = "daily_reminder"
    private const val NOTIFICATION_ID = 1001
    private const val KEY_ENABLED = "daily_reminder_enabled"

    /** 固定 21:00：OriginOS 不建议在深夜弹通知，也给"睡前补一句"留了时间。 */
    val defaultTime: LocalTime = LocalTime.of(21, 0)

    private fun prefs(context: Context) =
        context.getSharedPreferences("reminder", Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** 通知权限是否可用（API 33+ 需要运行时授权）。 */
    fun canNotify(context: Context): Boolean {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "每日提醒",
                // 用 DEFAULT 而不是 HIGH：HIGH 会弹悬浮通知，晚上打扰太大
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "晚上提醒写日记；当天已经写过就不打扰"
                setShowBadge(true)
            },
        )
    }

    /**
     * 发提醒。用固定的 notification id，重复提醒会覆盖而不是堆叠
     * （OriginOS 单应用通知上限 50 条）。
     */
    fun notify(context: Context) {
        if (!canNotify(context)) return
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_NEW_ENTRY, true)
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify_diary)
            .setContentTitle("今天还没写日记")
            .setContentText("睡前一句话也好。")
            .setStyle(NotificationCompat.BigTextStyle().bigText("睡前一句话也好。今天的天气、见的人、心情，随手记一句就够。"))
            .setContentIntent(pending)
            .setAutoCancel(true)
            // OriginOS 限制 ongoing 通知，这里必须保持 false
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, "写一句", pending)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    /** 下一次提醒的时刻（今天没到点就是今天，过了就是明天）。 */
    fun nextTrigger(): LocalDateTime {
        val now = LocalDateTime.now()
        val today = now.toLocalDate().atTime(defaultTime)
        return if (now.isBefore(today)) today else today.plusDays(1)
    }

    const val EXTRA_NEW_ENTRY = "open_new_entry"
}
