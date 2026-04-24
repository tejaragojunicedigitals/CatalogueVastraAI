package com.nice.cataloguevastra.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.net.toUri
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.ActivitySignUpBinding
import com.nice.cataloguevastra.ui.base.BaseBindingActivity
import com.nice.cataloguevastra.utils.ImagePickerManager
import com.nice.cataloguevastra.utils.queryDisplayName
import com.nice.cataloguevastra.viewmodel.SignUpEvent
import com.nice.cataloguevastra.viewmodel.SignUpViewModel
import com.nice.cataloguevastra.viewmodel.UiState

class SignUpActivity : BaseBindingActivity<ActivitySignUpBinding>() {

    private val viewModel: SignUpViewModel by viewModels()

    private lateinit var profileImagePicker: ImagePickerManager
    private lateinit var businessLogoPicker: ImagePickerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupImagePickers()
        setupClicks()
        observeUiState()
        observeEvents()
    }

    override fun inflateBinding(inflater: LayoutInflater): ActivitySignUpBinding {
        return ActivitySignUpBinding.inflate(inflater)
    }

    override fun loaderViewId(): Int = R.id.progressIndicator

    private fun setupImagePickers() {
        profileImagePicker = ImagePickerManager(this, this) { uri ->
            bindSelectedUserImage(uri)
        }
        businessLogoPicker = ImagePickerManager(this, this) { uri ->
            bindSelectedBusinessLogo(uri)
        }
    }

    private fun setupClicks() = with(binding) {
        profileImageCard.setOnClickListener { profileImagePicker.openPicker() }
        profileImageButton.setOnClickListener { profileImagePicker.openPicker() }
        businessLogoCard.setOnClickListener { businessLogoPicker.openPicker() }
        businessLogoButton.setOnClickListener { businessLogoPicker.openPicker() }
        createAccountButton.setOnClickListener {
            viewModel.signUp(
                username = usernameInput.text?.toString().orEmpty(),
                businessName = businessNameInput.text?.toString().orEmpty(),
                email = emailInput.text?.toString().orEmpty(),
                phoneNumber = phoneInput.text?.toString().orEmpty(),
                password = passwordInput.text?.toString().orEmpty()
            )
        }
        signInCta.setOnClickListener {
            finish()
        }
    }

    private fun observeUiState() {
        collectLatestLifecycleFlow(viewModel.formState) { state ->
            binding.usernameInput.error = state.usernameError
            binding.businessNameInput.error = state.businessNameError
            binding.emailInput.error = state.emailError
            binding.phoneInput.error = state.phoneNumberError
            binding.passwordInputLayout.error = state.passwordError

            renderSelectedImage(
                imageUri = state.userImage?.uri,
                imageLabel = state.userImage?.label,
                fallbackRes = com.nice.cataloguevastra.R.drawable.ic_profile,
                target = binding.profileImagePreview,
                fileNameView = binding.profileImageFileName,
                emptyText = "No profile image selected"
            )
            renderSelectedImage(
                imageUri = state.businessLogo?.uri,
                imageLabel = state.businessLogo?.label,
                fallbackRes = com.nice.cataloguevastra.R.drawable.business_logo,
                target = binding.businessLogoPreview,
                fileNameView = binding.businessLogoFileName,
                emptyText = "No business logo selected"
            )
        }

        collectLatestLifecycleFlow(viewModel.apiState) { state ->
            when (state) {
                UiState.Idle, is UiState.Success -> {
                    hideLoader()
                    binding.createAccountButton.isEnabled = true
                    viewModel.consumeApiState()
                }

                UiState.Loading -> {
                    showLoader()
                    binding.createAccountButton.isEnabled = false
                }

                is UiState.Error -> {
                    hideLoader()
                    binding.createAccountButton.isEnabled = true
                    showError(state.message)
                    viewModel.consumeApiState()
                }
            }
        }
    }

    private fun observeEvents() {
        collectLatestLifecycleFlow(viewModel.events) { event ->
            when (event) {
                is SignUpEvent.NavigateToLogin -> {
                    Toast.makeText(this, event.message, Toast.LENGTH_SHORT).show()
                    startActivity(
                        Intent(this, LoginActivity::class.java).apply {
                            putExtra(LoginActivity.EXTRA_USERNAME, event.username)
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                    finish()
                }
            }
        }
    }

    private fun bindSelectedUserImage(uri: Uri) {
        viewModel.onUserImageSelected(
            uri = uri.toString(),
            label = queryDisplayName(uri).orEmpty().ifBlank { "profile_image.jpg" }
        )
    }

    private fun bindSelectedBusinessLogo(uri: Uri) {
        viewModel.onBusinessLogoSelected(
            uri = uri.toString(),
            label = queryDisplayName(uri).orEmpty().ifBlank { "business_logo.jpg" }
        )
    }

    private fun renderSelectedImage(
        imageUri: String?,
        imageLabel: String?,
        fallbackRes: Int,
        target: ImageView,
        fileNameView: TextView,
        emptyText: String
    ) {
        val selectedUri = imageUri?.takeIf { it.isNotBlank() }?.toUri()
        if (selectedUri != null) {
            target.setImageURI(selectedUri)
            fileNameView.text = imageLabel.orEmpty().ifBlank {
                queryDisplayName(selectedUri).orEmpty().ifBlank { selectedUri.lastPathSegment ?: emptyText }
            }
        } else {
            target.setImageResource(fallbackRes)
            fileNameView.text = emptyText
        }
    }
}
