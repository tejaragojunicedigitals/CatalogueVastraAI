package com.nice.cataloguevastra.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import coil.request.CachePolicy
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.BottomSheetUpdatePasswordBinding
import com.nice.cataloguevastra.databinding.DialogLogoutConfirmationBinding
import com.nice.cataloguevastra.ui.activities.LoginActivity
import com.nice.cataloguevastra.databinding.FragmentAccountBinding
import com.nice.cataloguevastra.ui.base.BaseFragment
import com.nice.cataloguevastra.utils.ImagePickerManager
import com.nice.cataloguevastra.utils.queryDisplayName
import com.nice.cataloguevastra.viewmodel.AccountCreditSummary
import com.nice.cataloguevastra.viewmodel.AccountEvent
import com.nice.cataloguevastra.viewmodel.AccountProfileUiModel
import com.nice.cataloguevastra.viewmodel.AccountViewModel
import com.nice.cataloguevastra.viewmodel.UiState
import com.nice.cataloguevastra.model.ProfileUpdateParams
import kotlinx.coroutines.launch
import java.text.NumberFormat

class AccountFragment : BaseFragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AccountViewModel by viewModels()
    private lateinit var profileImagePicker: ImagePickerManager
    private lateinit var businessLogoPicker: ImagePickerManager
    private var currentProfile: AccountProfileUiModel? = null
    private var selectedProfileImageUri: Uri? = null
    private var selectedBusinessLogoUri: Uri? = null
    private var passwordSheetDialog: BottomSheetDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupImagePickers()
        setupImageActions()
        setupActions()
        observeViewModel()
        viewModel.loadProfile()

        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>(PASSWORD_UPDATED_MESSAGE_KEY)
            ?.observe(viewLifecycleOwner) { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                findNavController().currentBackStackEntry
                    ?.savedStateHandle
                    ?.remove<String>(PASSWORD_UPDATED_MESSAGE_KEY)
        }
    }

    override fun loaderViewId(): Int = R.id.progressIndicator

    private fun setupImagePickers() {
        profileImagePicker = ImagePickerManager(this, requireContext(), ::bindSelectedProfileImage)
        businessLogoPicker = ImagePickerManager(this, requireContext(), ::bindSelectedBusinessLogo)
    }

    private fun setupImageActions() = with(binding) {
        profileImageSection.setOnClickListener { profileImagePicker.openPicker() }
        profileImage.setOnClickListener { profileImagePicker.openPicker() }
        profileImageCameraButton.setOnClickListener { profileImagePicker.openPicker() }
        profileImageHint.setOnClickListener { profileImagePicker.openPicker() }

        businessLogoCard.setOnClickListener { businessLogoPicker.openPicker() }
        businessLogoBrowse.setOnClickListener { businessLogoPicker.openPicker() }
    }

    private fun setupActions() = with(binding) {
        cancelBtn.setOnClickListener {
            selectedProfileImageUri = null
            selectedBusinessLogoUri = null
            viewModel.loadProfile()
        }
        saveBtn.setOnClickListener {
            submitProfileUpdate()
        }
        updatePasswordText.setOnClickListener {
            showUpdatePasswordSheet()
        }
        logoutButton.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        val dialogBinding = DialogLogoutConfirmationBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        dialogBinding.logoutButton.setOnClickListener {
            dialog.dismiss()
            viewModel.logout()
        }

        dialog.setOnShowListener {
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        dialog.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.creditSummary.collect(::renderCreditSummary)
                }
                launch {
                    viewModel.profileState.collect(::renderProfileState)
                }
                launch {
                    viewModel.updateState.collect(::renderUpdateState)
                }
                launch {
                    viewModel.logoutState.collect(::renderLogoutState)
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is AccountEvent.LoggedOut -> openLogin(event.message)
                        }
                    }
                }
            }
        }
    }

    private fun bindSelectedProfileImage(uri: Uri) {
        selectedProfileImageUri = uri
        Log.d(LOG_TAG, "Profile image picked from camera/gallery: $uri")
        binding.profileImage.load(uri) {
            placeholder(R.drawable.ic_profile)
            error(R.drawable.ic_profile)
            fallback(R.drawable.ic_profile)
        }
    }

    private fun bindSelectedBusinessLogo(uri: Uri) {
        selectedBusinessLogoUri = uri
        Log.d(LOG_TAG, "Business logo picked from camera/gallery: $uri")
        binding.businessLogoImage.load(uri) {
            placeholder(R.drawable.business_logo)
            error(R.drawable.business_logo)
            fallback(R.drawable.business_logo)
        }
        binding.businessLogoValue.text = requireContext().queryDisplayName(uri)
            .orEmpty()
            .ifBlank { getString(com.nice.cataloguevastra.R.string.account_business_logo_fallback_name) }
    }

    private fun renderCreditSummary(summary: AccountCreditSummary) {
        val formatter = NumberFormat.getIntegerInstance()
        binding.currentBalanceValue.text = formatter.format(summary.currentBalance)
        binding.totalAddedValue.text = formatter.format(summary.totalCreditsAdded)
        binding.totalUsedValue.text = formatter.format(summary.totalCreditsUsed)
        binding.imagesChargedValue.text = formatter.format(summary.imagesCharged)
    }

    private fun renderProfileState(state: UiState<AccountProfileUiModel>) {
        when (state) {
            UiState.Idle -> Unit
            UiState.Loading -> {
                showLoader()
                setProfileInputsEnabled(false)
            }
            is UiState.Success -> {
                hideLoader()
                setProfileInputsEnabled(true)
                bindProfile(state.data)
                viewModel.consumeProfileState()
            }
            is UiState.Error -> {
                hideLoader()
                setProfileInputsEnabled(true)
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                viewModel.consumeProfileState()
            }
        }
    }

    private fun bindProfile(profile: AccountProfileUiModel) = with(binding) {
        currentProfile = profile
        fullNameEt.setText(profile.fullName)
        businessEt.setText(profile.businessName)
        emailEt.setText(profile.email)
        mobileEt.setText(profile.phoneNumber)

        val profileImageSource = selectedProfileImageUri ?: profile.userImageUrl.toFreshImageUrl()
        val businessLogoSource = selectedBusinessLogoUri ?: profile.businessLogoUrl.toFreshImageUrl()

        profileImage.load(profileImageSource) {
            placeholder(R.drawable.ic_profile)
            error(R.drawable.ic_profile)
            fallback(R.drawable.ic_profile)
            memoryCachePolicy(CachePolicy.DISABLED)
            diskCachePolicy(CachePolicy.DISABLED)
        }
        businessLogoImage.load(businessLogoSource) {
            placeholder(R.drawable.business_logo)
            error(R.drawable.business_logo)
            fallback(R.drawable.business_logo)
            memoryCachePolicy(CachePolicy.DISABLED)
            diskCachePolicy(CachePolicy.DISABLED)
        }
        businessLogoValue.text = if (profile.businessLogoUrl.isNullOrBlank()) {
            getString(R.string.account_no_business_logo)
        } else {
            profile.businessLogoUrl.substringAfterLast('/')
                .ifBlank { getString(R.string.account_business_logo_available) }
        }
    }

    private fun renderUpdateState(state: UiState<String>) {
        when (state) {
            UiState.Idle -> Unit
            UiState.Loading -> {
                showLoader()
                setProfileInputsEnabled(false)
                binding.saveBtn.text = getString(R.string.account_saving_changes)
            }
            is UiState.Success -> {
                hideLoader()
                selectedProfileImageUri = null
                selectedBusinessLogoUri = null
                passwordSheetDialog?.dismiss()
                viewModel.loadProfile()
                setProfileInputsEnabled(true)
                binding.saveBtn.text = getString(R.string.account_save_changes)
                Snackbar.make(binding.root, state.data, Snackbar.LENGTH_SHORT).show()
                viewModel.consumeUpdateState()
            }
            is UiState.Error -> {
                hideLoader()
                setProfileInputsEnabled(true)
                binding.saveBtn.text = getString(R.string.account_save_changes)
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                viewModel.consumeUpdateState()
            }
        }
    }

    private fun submitProfileUpdate() = with(binding) {
        val name = fullNameEt.text?.toString()?.trim().orEmpty()
        val businessName = businessEt.text?.toString()?.trim().orEmpty()
        val email = emailEt.text?.toString()?.trim().orEmpty()
        val phone = mobileEt.text?.toString()?.trim().orEmpty()
        val userUniqueId = currentProfile?.userUniqueId.orEmpty()

        fullNameEt.error = null
        businessEt.error = null
        emailEt.error = null
        mobileEt.error = null

        Log.d(
            LOG_TAG,
            "Save profile tapped. selectedProfileImageUri=$selectedProfileImageUri, " +
                "selectedBusinessLogoUri=$selectedBusinessLogoUri"
        )

        when {
            name.isBlank() -> fullNameEt.error = getString(R.string.account_error_name_required)
            businessName.isBlank() -> businessEt.error = getString(R.string.account_error_business_required)
            email.isBlank() -> emailEt.error = getString(R.string.account_error_email_required)
            phone.isBlank() -> mobileEt.error = getString(R.string.account_error_phone_required)
            userUniqueId.isBlank() -> Snackbar.make(
                root,
                getString(R.string.account_error_profile_not_loaded),
                Snackbar.LENGTH_SHORT
            ).show()
            else -> viewModel.updateProfile(
                ProfileUpdateParams(
                    username = name,
                    email = email,
                    phoneNumber = phone,
                    businessName = businessName,
                    userUniqueId = userUniqueId,
                    userImageUri = selectedProfileImageUri?.toString(),
                    businessLogoUri = selectedBusinessLogoUri?.toString()
                )
            )
        }
    }

    private fun showUpdatePasswordSheet() {
        val sheetBinding = BottomSheetUpdatePasswordBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(sheetBinding.root)
        dialog.setOnShowListener {
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.background = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.bg_bottom_sheet_white_rounded
                )
        }
        passwordSheetDialog = dialog

        sheetBinding.updatePasswordButton.setOnClickListener {
            submitPasswordUpdate(sheetBinding)
        }
        dialog.setOnDismissListener {
            if (passwordSheetDialog === dialog) {
                passwordSheetDialog = null
            }
        }
        dialog.show()
    }

    private fun submitPasswordUpdate(sheetBinding: BottomSheetUpdatePasswordBinding) = with(sheetBinding) {
        val currentPassword = currentPasswordEt.text?.toString().orEmpty()
        val newPassword = newPasswordEt.text?.toString().orEmpty()
        val confirmPassword = confirmPasswordEt.text?.toString().orEmpty()

        currentPasswordEt.error = null
        newPasswordEt.error = null
        confirmPasswordEt.error = null

        when {
            currentPassword.isBlank() -> currentPasswordEt.error =
                getString(R.string.update_password_error_current)
            newPassword.isBlank() -> newPasswordEt.error =
                getString(R.string.update_password_error_new)
            newPassword.length < MIN_PASSWORD_LENGTH -> newPasswordEt.error =
                getString(R.string.update_password_error_length)
            confirmPassword.isBlank() -> confirmPasswordEt.error =
                getString(R.string.update_password_error_confirm)
            newPassword != confirmPassword -> confirmPasswordEt.error =
                getString(R.string.update_password_error_mismatch)
            else -> submitProfileUpdate(
                currentPassword = currentPassword,
                newPassword = newPassword,
                newPasswordConfirm = confirmPassword
            )
        }
    }

    private fun submitProfileUpdate(
        currentPassword: String,
        newPassword: String,
        newPasswordConfirm: String
    ) = with(binding) {
        val name = fullNameEt.text?.toString()?.trim().orEmpty()
        val businessName = businessEt.text?.toString()?.trim().orEmpty()
        val email = emailEt.text?.toString()?.trim().orEmpty()
        val phone = mobileEt.text?.toString()?.trim().orEmpty()
        val userUniqueId = currentProfile?.userUniqueId.orEmpty()

        when {
            userUniqueId.isBlank() -> Snackbar.make(
                root,
                getString(R.string.account_error_profile_not_loaded),
                Snackbar.LENGTH_SHORT
            ).show()
            else -> viewModel.updateProfile(
                ProfileUpdateParams(
                    username = name,
                    email = email,
                    phoneNumber = phone,
                    businessName = businessName,
                    userUniqueId = userUniqueId,
                    userImageUri = selectedProfileImageUri?.toString(),
                    businessLogoUri = selectedBusinessLogoUri?.toString(),
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    newPasswordConfirm = newPasswordConfirm
                )
            )
        }
    }

    private fun setProfileInputsEnabled(isEnabled: Boolean) = with(binding) {
        fullNameEt.isEnabled = isEnabled
        businessEt.isEnabled = isEnabled
        emailEt.isEnabled = isEnabled
        mobileEt.isEnabled = isEnabled
        updatePasswordText.isEnabled = isEnabled
        cancelBtn.isEnabled = isEnabled
        saveBtn.isEnabled = isEnabled
        profileImageSection.isEnabled = isEnabled
        businessLogoCard.isEnabled = isEnabled
        businessLogoBrowse.isEnabled = isEnabled
    }

    private fun renderLogoutState(state: UiState<String>) {
        when (state) {
            UiState.Idle -> {
                hideLoader()
                binding.logoutProgress.visibility = View.GONE
                binding.logoutButton.isEnabled = true
                binding.logoutButton.text = getString(com.nice.cataloguevastra.R.string.account_logout_button)
            }
            UiState.Loading -> {
                showLoader()
                binding.logoutProgress.visibility = View.GONE
                binding.logoutButton.isEnabled = false
                binding.logoutButton.text = getString(com.nice.cataloguevastra.R.string.account_logging_out)
            }
            is UiState.Success -> {
                hideLoader()
                binding.logoutProgress.visibility = View.GONE
                binding.logoutButton.isEnabled = true
                binding.logoutButton.text = getString(com.nice.cataloguevastra.R.string.account_logout_button)
                viewModel.consumeLogoutState()
            }
            is UiState.Error -> {
                hideLoader()
                binding.logoutProgress.visibility = View.GONE
                binding.logoutButton.isEnabled = true
                binding.logoutButton.text = getString(com.nice.cataloguevastra.R.string.account_logout_button)
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                viewModel.consumeLogoutState()
            }
        }
    }

    private fun openLogin(message: String) {
        startActivity(
            Intent(requireContext(), LoginActivity::class.java).apply {
                putExtra(LoginActivity.EXTRA_MESSAGE, message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        passwordSheetDialog?.dismiss()
        passwordSheetDialog = null
        _binding = null
    }

    companion object {
        const val PASSWORD_UPDATED_MESSAGE_KEY = "password_updated_message"
        private const val LOG_TAG = "AccountFragment"
        private const val MIN_PASSWORD_LENGTH = 6
    }
}

private fun String?.toFreshImageUrl(): String? {
    if (isNullOrBlank()) return null
    val separator = if (contains("?")) "&" else "?"
    return "$this${separator}v=${System.currentTimeMillis()}"
}
