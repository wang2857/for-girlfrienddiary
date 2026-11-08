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

    /** 设置/修改密码后立即生效并锁一次，避免"设完密码还以为没锁"。 */
    fun setPin(pin: String) {
        store.setPin(pin)
        store.enabled = true
        _enabled.value = true
        _locked.value = true
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
