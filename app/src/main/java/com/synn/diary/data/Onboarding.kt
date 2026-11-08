package com.synn.diary.data

import androidx.core.content.edit
import android.content.Context

/** 首次安装的引导是否走完了。 */
object Onboarding {

    private const val PREFS = "onboarding"
    private const val KEY_DONE = "done_v1"

    fun isDone(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DONE, false)

    fun markDone(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_DONE, true) }
    }
}
