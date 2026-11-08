package com.synn.diary.data

import androidx.core.content.edit
import android.content.Context

/** 回收站里保留多久后自动清理。 */
class TrashSettings(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("trash", Context.MODE_PRIVATE)

    var retentionDays: Int
        get() = prefs.getInt(KEY_DAYS, DEFAULT_DAYS)
        set(value) = prefs.edit { putInt(KEY_DAYS, value) }

    private companion object {
        const val KEY_DAYS = "retention_days"

        /** 默认一个月，既不会塞满，也不至于误删后没机会找回。 */
        const val DEFAULT_DAYS = 30
    }
}
