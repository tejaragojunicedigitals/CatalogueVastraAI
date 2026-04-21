package com.nice.cataloguevastra.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nice.cataloguevastra.databinding.FragmentUpdatePasswordBinding
import com.nice.cataloguevastra.R

class UpdatePasswordFragment : Fragment() {

    private var _binding: FragmentUpdatePasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdatePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        listOf(
            binding.currentPasswordInput,
            binding.newPasswordInput,
            binding.confirmPasswordInput
        ).forEach { input ->
            input.doAfterTextChanged { clearErrors() }
        }

        binding.updatePasswordButton.setOnClickListener {
            submitPasswordUpdate()
        }
    }

    private fun submitPasswordUpdate() {
        clearErrors()

        val currentPassword = binding.currentPasswordInput.text?.toString()?.trim().orEmpty()
        val newPassword = binding.newPasswordInput.text?.toString()?.trim().orEmpty()
        val confirmPassword = binding.confirmPasswordInput.text?.toString()?.trim().orEmpty()

        var isValid = true

        if (currentPassword.isEmpty()) {
            binding.currentPasswordLayout.error = getString(R.string.update_password_error_current)
            isValid = false
        }

        if (newPassword.isEmpty()) {
            binding.newPasswordLayout.error = getString(R.string.update_password_error_new)
            isValid = false
        } else if (newPassword.length < 6) {
            binding.newPasswordLayout.error = getString(R.string.update_password_error_length)
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordLayout.error = getString(R.string.update_password_error_confirm)
            isValid = false
        } else if (newPassword != confirmPassword) {
            binding.confirmPasswordLayout.error = getString(R.string.update_password_error_mismatch)
            isValid = false
        }

        if (!isValid) return

        findNavController().previousBackStackEntry
            ?.savedStateHandle
            ?.set(
                AccountFragment.PASSWORD_UPDATED_MESSAGE_KEY,
                getString(R.string.update_password_success_message)
            )
        findNavController().popBackStack()
    }

    private fun clearErrors() = with(binding) {
        currentPasswordLayout.error = null
        newPasswordLayout.error = null
        confirmPasswordLayout.error = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
