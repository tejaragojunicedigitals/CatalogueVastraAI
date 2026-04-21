package com.nice.cataloguevastra.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cataloguevastra.CatalogueVastraApp
import com.nice.cataloguevastra.repositories.AuthRepository
import com.nice.cataloguevastra.model.LoginRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val authRepository: AuthRepository =
        (application as CatalogueVastraApp).appContainer.authRepository

    private val _formState = MutableStateFlow(LoginFormState())
    val formState: StateFlow<LoginFormState> = _formState.asStateFlow()

    private val _apiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val apiState: StateFlow<UiState<Unit>> = _apiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    fun login(
        username: String,
        password: String,
        deviceId: String,
        deviceName: String
    ) {
        val trimmedUsername = username.trim()
        val validationState = LoginFormState(
            usernameError = if (trimmedUsername.isBlank()) "Enter your username." else null,
            passwordError = when {
                password.isBlank() -> "Enter your password."
                password.length < 6 -> "Password must be at least 6 characters."
                else -> null
            }
        )

        if (validationState.usernameError != null || validationState.passwordError != null) {
            _formState.value = validationState
            return
        }

        viewModelScope.launch {
            _formState.value = LoginFormState()
            _apiState.value = UiState.Loading

            authRepository.login(
                LoginRequest(
                    username = trimmedUsername,
                    password = password,
                    deviceId = deviceId,
                    deviceName = deviceName
                )
            ).onSuccess {
                _apiState.value = UiState.Success(Unit)
                _events.emit(LoginEvent.NavigateToHome)
            }.onFailure { throwable ->
                _apiState.value = UiState.Error(
                    throwable.message ?: "Unable to sign in right now."
                )
            }
        }
    }

    fun consumeApiState() {
        if (_apiState.value !is UiState.Loading) {
            _apiState.value = UiState.Idle
        }
    }
}

sealed interface LoginEvent {
    data object NavigateToHome : LoginEvent
}
