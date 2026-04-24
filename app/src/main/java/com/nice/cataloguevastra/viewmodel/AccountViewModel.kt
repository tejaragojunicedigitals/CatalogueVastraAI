package com.nice.cataloguevastra.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cataloguevastra.CatalogueVastraApp
import com.nice.cataloguevastra.model.ProfileResponse
import com.nice.cataloguevastra.model.ProfileUpdateParams
import com.nice.cataloguevastra.repositories.AuthRepository
import com.nice.cataloguevastra.utils.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val appContainer = (application as CatalogueVastraApp).appContainer
    private val authRepository: AuthRepository =
        appContainer.authRepository
    private val sessionManager: SessionManager =
        appContainer.sessionManager

    private val _creditSummary = MutableStateFlow(
        AccountCreditSummary(
            currentBalance = sessionManager.getCreditsBalance()
        )
    )
    val creditSummary: StateFlow<AccountCreditSummary> = _creditSummary.asStateFlow()

    private val _profileState = MutableStateFlow<UiState<AccountProfileUiModel>>(UiState.Idle)
    val profileState: StateFlow<UiState<AccountProfileUiModel>> = _profileState.asStateFlow()

    private val _updateState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val updateState: StateFlow<UiState<String>> = _updateState.asStateFlow()

    private val _logoutState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val logoutState: StateFlow<UiState<String>> = _logoutState.asStateFlow()

    private val _events = MutableSharedFlow<AccountEvent>()
    val events: SharedFlow<AccountEvent> = _events.asSharedFlow()

    fun refreshSummary() {
        _creditSummary.value = _creditSummary.value.copy(
            currentBalance = sessionManager.getCreditsBalance()
        )
    }

    fun loadProfile() {
        if (_profileState.value is UiState.Loading) return

        viewModelScope.launch {
            _profileState.value = UiState.Loading
            authRepository.getProfile()
                .onSuccess { response ->
                    val profile = response.toAccountProfileUiModel()
                    _creditSummary.value = response.toAccountCreditSummary(
                        fallbackBalance = sessionManager.getCreditsBalance()
                    )
                    _profileState.value = UiState.Success(profile)
                }
                .onFailure { throwable ->
                    refreshSummary()
                    _profileState.value = UiState.Error(
                        throwable.message ?: "Unable to load profile right now."
                    )
                }
        }
    }

    fun updateProfile(params: ProfileUpdateParams) {
        if (_updateState.value is UiState.Loading) return

        viewModelScope.launch {
            _updateState.value = UiState.Loading
            authRepository.updateProfile(params)
                .onSuccess { response ->
                    _creditSummary.value = response.toAccountCreditSummary(
                        fallbackBalance = sessionManager.getCreditsBalance()
                    )
                    _profileState.value = UiState.Success(response.toAccountProfileUiModel())
                    _updateState.value = UiState.Success(
                        response.message?.takeIf { it.isNotBlank() } ?: "Profile updated."
                    )
                }
                .onFailure { throwable ->
                    _updateState.value = UiState.Error(
                        throwable.message ?: "Unable to update profile right now."
                    )
                }
        }
    }

    fun logout() {
        if (_logoutState.value is UiState.Loading) return

        viewModelScope.launch {
            _logoutState.value = UiState.Loading
            authRepository.logout()
                .onSuccess { message ->
                    CatalogueListViewModel.clearCache()
                    _logoutState.value = UiState.Success(message)
                    _events.emit(AccountEvent.LoggedOut(message))
                }
                .onFailure { throwable ->
                    _logoutState.value = UiState.Error(
                        throwable.message ?: "Unable to logout right now."
                    )
                }
        }
    }

    fun consumeLogoutState() {
        if (_logoutState.value !is UiState.Loading) {
            _logoutState.value = UiState.Idle
        }
    }

    fun consumeProfileState() {
        if (_profileState.value !is UiState.Loading) {
            _profileState.value = UiState.Idle
        }
    }

    fun consumeUpdateState() {
        if (_updateState.value !is UiState.Loading) {
            _updateState.value = UiState.Idle
        }
    }
}

data class AccountCreditSummary(
    val currentBalance: Int,
    val totalCreditsAdded: Int = 0,
    val totalCreditsUsed: Int = 0,
    val imagesCharged: Int = 0
)

data class AccountProfileUiModel(
    val fullName: String,
    val businessName: String,
    val email: String,
    val phoneNumber: String,
    val userUniqueId: String,
    val userImageUrl: String?,
    val businessLogoUrl: String?
)

sealed interface AccountEvent {
    data class LoggedOut(val message: String) : AccountEvent
}

private fun ProfileResponse.toAccountProfileUiModel(): AccountProfileUiModel {
    return AccountProfileUiModel(
        fullName = user?.username.orEmpty(),
        businessName = user?.businessName.orEmpty(),
        email = user?.email.orEmpty(),
        phoneNumber = user?.phoneNumber.orEmpty(),
        userUniqueId = user?.userUniqueId.orEmpty(),
        userImageUrl = user?.userImage?.takeIf { it.isNotBlank() },
        businessLogoUrl = user?.businessLogo?.takeIf { it.isNotBlank() }
    )
}

private fun ProfileResponse.toAccountCreditSummary(fallbackBalance: Int): AccountCreditSummary {
    val usage = creditUsage ?: credits?.creditUsage
    val balance = creditsBalance
        ?: credits?.balance
        ?: usage?.creditsAvailable
        ?: packageDetails?.creditsBalance
        ?: credits?.packageDetails?.creditsBalance
        ?: fallbackBalance

    return AccountCreditSummary(
        currentBalance = balance,
        totalCreditsAdded = usage?.creditsTotalGranted ?: 0,
        totalCreditsUsed = usage?.creditsUsedTotal ?: 0,
        imagesCharged = usage?.completedRendersCount ?: 0
    )
}
