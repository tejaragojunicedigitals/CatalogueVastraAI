package com.nice.cataloguevastra.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.CatalogueVastraApp
import com.nice.cataloguevastra.MainActivity
import com.nice.cataloguevastra.databinding.ActivityLoginBinding
import com.nice.cataloguevastra.ui.base.BaseBindingActivity
import com.nice.cataloguevastra.utils.DeviceInfoProvider
import com.nice.cataloguevastra.viewmodel.LoginEvent
import com.nice.cataloguevastra.viewmodel.LoginViewModel
import com.nice.cataloguevastra.viewmodel.UiState

class LoginActivity : BaseBindingActivity<ActivityLoginBinding>() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if ((application as CatalogueVastraApp).appContainer.sessionManager.hasToken()) {
            openMainAndFinish()
            return
        }

        restorePrefilledUsername()
        setupClicks()
        observeUiState()
        observeEvents()
    }

    override fun inflateBinding(inflater: LayoutInflater): ActivityLoginBinding {
        return ActivityLoginBinding.inflate(inflater)
    }

    override fun loaderViewId(): Int = R.id.progressIndicator

    private fun restorePrefilledUsername() {
        val username = intent.getStringExtra(EXTRA_USERNAME).orEmpty()
        if (username.isNotBlank()) {
            binding.usernameInput.setText(username)
            binding.usernameInput.setSelection(username.length)
        }
        intent.getStringExtra(EXTRA_MESSAGE)?.takeIf { it.isNotBlank() }?.let { message ->
            showMessage(message)
        }
    }

    private fun setupClicks() = with(binding) {
        signInButton.setOnClickListener {
            viewModel.login(
                username = usernameInput.text?.toString().orEmpty(),
                password = passwordInput.text?.toString().orEmpty(),
                deviceId = DeviceInfoProvider.getDeviceId(this@LoginActivity),
                deviceName = DeviceInfoProvider.getDeviceName()
            )
        }

        signUpCta.setOnClickListener {
            startActivity(Intent(this@LoginActivity, SignUpActivity::class.java))
        }
    }

    private fun observeUiState() {
        collectLatestLifecycleFlow(viewModel.formState) { state ->
            binding.usernameInput.error = state.usernameError
            binding.passwordInputLayout.error = state.passwordError
        }

        collectLatestLifecycleFlow(viewModel.apiState) { state ->
            when (state) {
                UiState.Idle, is UiState.Success -> {
                    hideLoader()
                    binding.signInButton.isEnabled = true
                    viewModel.consumeApiState()
                }

                UiState.Loading -> {
                    showLoader()
                    binding.signInButton.isEnabled = false
                }

                is UiState.Error -> {
                    hideLoader()
                    binding.signInButton.isEnabled = true
                    showError(state.message)
                    viewModel.consumeApiState()
                }
            }
        }
    }

    private fun observeEvents() {
        collectLatestLifecycleFlow(viewModel.events) { event ->
            when (event) {
                LoginEvent.NavigateToHome -> openMainAndFinish()
            }
        }
    }

    private fun openMainAndFinish() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    companion object {
        const val EXTRA_USERNAME = "extra_username"
        const val EXTRA_MESSAGE = "extra_message"
    }
}
