package com.synn.diary.data.memory

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import coil3.SingletonImageLoader
import com.synn.diary.data.PendingWrites

/**
 * vivo「公平运行内存机制」客户端。
 *
 * 系统在内存吃紧时发两条广播：
 * - `itgsa.intent.action.TRIM`：赶紧放内存（图片缓存、临时对象）
 * - `itgsa.intent.action.KILL`：马上保存现场，3 秒后进程会被杀
 *
 * 两条都要在 3 秒内通过广播里带的 Binder 回执，否则系统按超时处理。
 * 这套协议只在 vivo 设备上会收到，别的机型上注册了也收不到广播，无副作用。
 */
class MemoryPressureBridge(private val context: Context) {

    private val thread = HandlerThread("memory-pressure").apply { start() }
    private val handler = Handler(thread.looper)

    private var remote: IBinder? = null
    private var registered = false

    private val deathRecipient = IBinder.DeathRecipient {
        synchronized(this) { remote = null }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i(TAG, "received ${intent.action}")
            val data = intent.extras ?: return
            val common = data.getBundle(KEY_COMMON) ?: return

            val notifyType = common.getInt(KEY_NOTIFY_TYPE)
            val notifyId = common.getInt(KEY_NOTIFY_ID)
            val reason = common.getString(KEY_REASON).orEmpty()
            val action = common.getString(KEY_ACTION).orEmpty()
            val callback = common.getBinder(KEY_CALLBACK)

            Log.i(TAG, "memory notify: action=$action type=$notifyType reason=$reason")

            if (action == ACTION_TRIM_VALUE) {
                releaseMemory()
            } else {
                // KILL 或其它：先把没落盘的东西刷掉，再回执
                PendingWrites.flushAll()
            }

            if (callback != null) {
                reply(callback, notifyType, notifyId, RESULT_OK)
            }
        }
    }

    fun initialize() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(ACTION_TRIM)
            addAction(ACTION_KILL)
        }
        // vivo 的 TRIM/KILL 是系统服务发过来的私有广播，必须 RECEIVER_EXPORTED 才收得到
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            null,
            handler,
            ContextCompat.RECEIVER_EXPORTED,
        )
        registered = true
        Log.i(TAG, "memory pressure bridge initialized")
    }

    private fun releaseMemory() {
        // 图片缓存是这类应用里最大的一块；清掉后重启加载会重新解码，代价可接受
        runCatching { SingletonImageLoader.get(context).memoryCache?.clear() }
            .onFailure { Log.w(TAG, "clear image cache failed", it) }
    }

    private fun reply(callback: IBinder, notifyType: Int, notifyId: Int, result: Int) {
        synchronized(this) {
            if (remote == null) {
                remote = callback
                runCatching { callback.linkToDeath(deathRecipient, 0) }
                    .onFailure { remote = null; return }
            }
        }

        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInt(notifyType)
            data.writeInt(notifyId)
            data.writeInt(result)
            data.writeBundle(Bundle().apply { putString(KEY_REPLY, "saved") })
            remote?.transact(TRANSACTION_EXCEPTION_REPLY, data, reply, IBinder.FLAG_ONEWAY)
            reply.readException()
        } catch (e: Exception) {
            Log.e(TAG, "reply failed", e)
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    private companion object {
        const val TAG = "MemoryPressure"

        const val ACTION_TRIM = "itgsa.intent.action.TRIM"
        const val ACTION_KILL = "itgsa.intent.action.KILL"
        const val ACTION_TRIM_VALUE = "trim"

        const val KEY_COMMON = "common"
        const val KEY_NOTIFY_TYPE = "notifyType"
        const val KEY_NOTIFY_ID = "notifyId"
        const val KEY_REASON = "reason"
        const val KEY_ACTION = "action"
        const val KEY_CALLBACK = "callback"
        const val KEY_REPLY = "reply"

        const val RESULT_OK = 0
        const val TRANSACTION_EXCEPTION_REPLY = IBinder.FIRST_CALL_TRANSACTION
    }
}
