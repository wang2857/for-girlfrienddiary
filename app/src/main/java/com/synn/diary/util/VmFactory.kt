package com.synn.diary.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * 最小可用的 ViewModel 工厂：取代 Hilt 省掉一整套注解处理，够用到依赖图变复杂为止。
 */
class VmFactory(private val builder: () -> ViewModel) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = builder() as T
}
