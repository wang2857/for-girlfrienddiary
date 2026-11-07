package com.synn.diary.data

import java.util.concurrent.CopyOnWriteArraySet

/**
 * "还有没落盘的东西" 的登记处。
 *
 * 编辑器把自己的 flush 挂进来；系统要杀进程前（vivo 内存机制）统一刷一遍，
 * 免得正在输入的几百毫秒内容丢掉。
 */
object PendingWrites {

    private val hooks = CopyOnWriteArraySet<() -> Unit>()

    fun register(hook: () -> Unit) {
        hooks += hook
    }

    fun unregister(hook: () -> Unit) {
        hooks -= hook
    }

    fun flushAll() {
        hooks.toList().forEach { runCatching { it() } }
    }
}
