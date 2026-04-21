package com.nice.cataloguevastra.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cataloguevastra.CatalogueVastraApp
import com.nice.cataloguevastra.repositories.AuthRepository
import com.nice.cataloguevastra.model.SignUpParams
import com.nice.cataloguevastra.model.SignUpResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignUpViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val authRepository: AuthRepository =
        (application as CatalogueVastraApp).appContainer.authRepository

    private val _formState = MutableStateFlow(SignUpFormState())
    val formState: StateFlow<SignUpFormState> = _formState.asStateFlow()

    private val _apiState = MutableStateFlow<UiState<SignUpResult>>(UiState.Idle)
    val apiState: StateFlow<UiState<SignUpResult>> = _apiState.asStateFlow()

    private val _events = MutableSharedFlow<SignUpEvent>()
    val events: SharedFlow<SignUpEvent> = _events.asSharedFlow()

    fun onUserImageSelected(uri: String, label: String) {
        _formState.update { it.copy(userImage = SelectedImageUi(uri, label)) }
    }

    fun onBusinessLogoSelected(uri: String, label: String) {
        _formState.update { it.copy(businessLogo = SelectedImageUi(uri, label)) }
    }

    fun signUp(
        username: String,
        businessName: String,
        email: String,
        phoneNumber: String,
        password: String
    ) {
        val trimmedUsername = username.trim()
        val trimmedBusinessName = businessName.trim()
        val trimmedEmail = email.trim()
        val trimmedPhoneNumber = phoneNumber.trim()

        val validationState = _formState.value.copy(
            usernameError = if (trimmedUsername.isBlank()) "Enter your username." else null,
            businessNameError = if (trimmedBusinessName.isBlank()) "Enter your business name." else null,
            emailError = when {
                trimmedEmail.isBlank() -> "Enter your email address."
                !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches() -> "Enter a valid email address."
                else -> null
            },
            phoneNumberError = when {
                trimmedPhoneNumber.isBlank() -> "Enter your mobile number."
                !trimmedPhoneNumber.matches(PHONE_REGEX.toRegex()) -> "Enter a valid mobile number."
                else -> null
            },
            passwordError = when {
                password.isBlank() -> "Enter a password."
                password.length < 6 -> "Password must be at least 6 characters."
                else -> null
            }
        )

        if (
            validationState.usernameError != null ||
            validationState.businessNameError != null ||
            validationState.emailError != null ||
            validationState.phoneNumberError != null ||
            validationState.passwordError != null
        ) {
            _formState.value = validationState
            return
        }

        viewModelScope.launch {
            _formState.update {
                it.copy(
                    usernameError = null,
                    businessNameError = null,
                    emailError = null,
                    phoneNumberError = null,
                    passwordError = null
                )
            }
            _apiState.value = UiState.Loading

            authRepository.signUp(
                SignUpParams(
                    username = trimmedUsername,
                    password = password,
                    email = trimmedEmail,
                    phoneNumber = trimmedPhoneNumber,
                    businessName = trimmedBusinessName,
                    userImageUri = _formState.value.userImage?.uri,
                    businessLogoUri = _formState.value.businessLogo?.uri
                )
            ).onSuccess { result ->
                _apiState.value = UiState.Success(result)
                _events.emit(
                    SignUpEvent.NavigateToLogin(
                        username = result.username,
                        message = result.message
                    )
                )
            }.onFailure { throwable ->
                _apiState.value = UiState.Error(
                    throwable.message ?: "Unable to create your account right now."
                )
            }
        }
    }

    fun consumeApiState() {
        if (_apiState.value !is UiState.Loading) {
            _apiState.value = UiState.Idle
        }
    }

    companion object {
        private const val PHONE_REGEX = "^[+]?[0-9]{10,15}$"
    }
}

sealed interface SignUpEvent {
    data class NavigateToLogin(
        val username: String,
        val message: String
    ) : SignUpEvent
}
