package com.nice.cataloguevastra.ui.base

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nice.cataloguevastra.viewmodel.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class BaseActivity : AppCompatActivity() {

    private var loaderView: View? = null

    protected open fun loaderViewId(): Int? = null

    protected open fun windowInsetTarget(): View? = null

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    @CallSuper
    override fun onContentChanged() {
        super.onContentChanged()
        loaderView = loaderViewId()?.let(::findViewById)
        applyWindowInsets()
    }

    protected fun <T> collectLatestLifecycleFlow(
        flow: Flow<T>,
        collector: suspend (T) -> Unit
    ) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collectLatest(collector)
            }
        }
    }

    open fun showLoader() {
        loaderView?.bringToFront()
        loaderView?.visibility = View.VISIBLE
    }

    open fun hideLoader() {
        loaderView?.visibility = View.GONE
    }

    open fun showMessage(message: String) {
        if (message.isBlank()) return
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    open fun showError(message: String) {
        showMessage(message)
    }

    protected fun handleState(
        state: UiState<*>,
        onSuccess: (() -> Unit)? = null,
        onError: (() -> Unit)? = null
    ) {
        when (state) {
            is UiState.Idle -> hideLoader()
            is UiState.Loading -> showLoader()
            is UiState.Success<*> -> {
                hideLoader()
                onSuccess?.invoke()
            }

            is UiState.Error -> {
                hideLoader()
                showError(state.message)
                onError?.invoke()
            }
        }
    }

    protected fun applyInsetsTo(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { target, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            target.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }
    }

    private fun applyWindowInsets() {
        windowInsetTarget()?.let(::applyInsetsTo)
    }
}
