package com.nice.cataloguevastra.ui.base

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nice.cataloguevastra.viewmodel.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class BaseFragment : Fragment() {

    private var loaderView: View? = null

    protected open fun loaderViewId(): Int? = null

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loaderView = loaderViewId()?.let { id ->
            view.findViewById(id) ?: requireActivity().findViewById(id)
        }
    }

    protected fun <T> collectLatestLifecycleFlow(
        flow: Flow<T>,
        collector: suspend (T) -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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

    protected fun handleEmptyState(
        isEmpty: Boolean,
        contentView: View,
        emptyView: View
    ) {
        contentView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }
}
