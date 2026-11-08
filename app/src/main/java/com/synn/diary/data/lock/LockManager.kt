package com.synn.diary.data.lock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 锁的状态机：谁决定"现在要不要显示锁屏"。
 *
 * 规则很简单——是否启用看设置；启用后只要离开 app 就锁，
 * 回到前台必须解锁才能看到内容。
 */
class LockManager(val store: LockStore) {

    // 初始就是锁着的：新进程起来（冷启动）必须先验密码，
    // 否则"启动锁"在最该生效的场景下形同虚设
    private val _locked = MutableStateFlow(store.enabled && store.hasPin())
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private val _enabled = MutableStateFlow(store.enabled && store.hasPin())
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _allowScreenshots = MutableStateFlow(store.allowScreenshots)
    val allowScreenshots: StateFlow<Boolean> = _allowScreenshots.asStateFlow()

    val biometricEnabled: Boolean
        get() = store.biometricEnabled

    fun setAllowScreenshots(value: Boolean) {
        store.allowScreenshots = value
        _allowScreenshots.value = value
    }

    fun setBiometricEnabled(value: Boolean) {
        store.biometricEnabled = value
    }

    fun hasPin(): Boolean = store.hasPin()

    fun verifyPin(pin: String): Boolean = store.verifyPin(pin)

    // ---- 恢复码 ----

    fun hasRecoveryCode(): Boolean = store.hasRecoveryCode()

    /**
     * 设完密码后调用：还没有恢复码就生成一个并返回明文——
     * 明文只在这里出现一次，之后只留哈希，抄丢就再也看不到。
     */
    fun ensureRecoveryCode(): String? {
        if (store.hasRecoveryCode()) return null
        val code = RecoveryCode.generate()
        store.setRecoveryCode(code)
        return code
    }

    /** 设置里「重设恢复码」：旧的那张纸立刻作废。 */
    fun resetRecoveryCode(): String {
        val code = RecoveryCode.generate()
        store.setRecoveryCode(code)
        return code
    }

    fun verifyRecoveryCode(code: String): Boolean =
        store.hasRecoveryCode() && store.verifyRecoveryCode(code)

    /**
     * 用恢复码换一个新密码。成功就顺手解锁——用户此刻正站在锁屏前，
     * 再让他输一遍刚设的密码只是添堵。
     *
     * 恢复码本身不轮换：写下来的那张纸还能继续用，除非用户主动重设。
     */
    fun recoverWithCode(code: String, newPin: String): Boolean {
        if (!verifyRecoveryCode(code)) return false
        store.setPin(newPin)
        store.enabled = true
        _enabled.value = true
        _locked.value = false
        return true
    }

    /**
     * 设置/修改密码后立即生效。
     *
     * [lockNow] 默认 true：引导页设完密码就锁上，避免"设了密码还以为没锁"。
     * 设置页里要传 false——用户人就在应用内，一锁会把刚弹出的恢复码顶掉，
     * 那串码只显示一次，丢了等于白设。
     */
    fun setPin(pin: String, lockNow: Boolean = true) {
        store.setPin(pin)
        store.enabled = true
        _enabled.value = true
        if (lockNow) _locked.value = true
    }

    fun enableLock() {
        if (!store.hasPin()) return
        store.enabled = true
        _enabled.value = true
    }

    fun disableLock() {
        // 关闭应用锁 = 应用里不再有密码/指纹这回事，
        // 所以哈希也要清掉。否则会出现"没设密码却能上锁、解锁又要密码"的死循环。
        store.clear()
        _enabled.value = false
        _locked.value = false
        _allowScreenshots.value = false
    }

    /** 能不能给单篇日记上锁：必须真的有一个可验证的凭证。 */
    fun canLockEntries(): Boolean = _enabled.value && store.hasPin()

    /** 离开 app 时调用。 */
    fun lockIfEnabled() {
        if (_enabled.value && store.hasPin()) {
            _locked.value = true
        }
    }

    fun unlock() {
        _locked.value = false
    }
}
