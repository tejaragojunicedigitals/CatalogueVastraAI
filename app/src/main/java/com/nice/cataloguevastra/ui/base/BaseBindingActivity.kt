package com.nice.cataloguevastra.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.CallSuper
import androidx.viewbinding.ViewBinding

abstract class BaseBindingActivity<VB : ViewBinding> : BaseActivity() {

    private var _binding: VB? = null
    protected val binding: VB
        get() = requireNotNull(_binding) { "Binding is only valid between onCreate and onDestroy." }

    protected abstract fun inflateBinding(inflater: LayoutInflater): VB

    protected open fun bindingInsetTarget(): View = binding.root

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = inflateBinding(layoutInflater)
        setContentView(binding.root)
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    final override fun windowInsetTarget(): View = bindingInsetTarget()
}
