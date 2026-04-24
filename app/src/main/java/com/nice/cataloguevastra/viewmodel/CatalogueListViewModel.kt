package com.nice.cataloguevastra.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cataloguevastra.CatalogueVastraApp
import com.nice.cataloguevastra.model.CatalogueCardUiModel
import com.nice.cataloguevastra.repositories.CatalogueRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CatalogueListViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: CatalogueRepository =
        (application as CatalogueVastraApp).appContainer.catalogueRepository

    private val _uiState = MutableStateFlow(
        CatalogueListUiState(
            catalogues = cachedCatalogues,
            isLoading = false,
            errorMessage = cachedErrorMessage
        )
    )
    val uiState: StateFlow<CatalogueListUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var hasAttemptedInitialLoad = cachedCatalogues.isNotEmpty() || cachedErrorMessage != null

    fun loadCatalogues(forceRefresh: Boolean = false) {
        if (_uiState.value.isLoading) return
        if (hasAttemptedInitialLoad && !forceRefresh) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            repository.getCatalogues(page = 1, format = "json")
                .onSuccess { catalogues ->
                    hasAttemptedInitialLoad = true
                    cachedCatalogues = catalogues
                    cachedErrorMessage = null
                    _uiState.value = CatalogueListUiState(
                        catalogues = catalogues,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                .onFailure { throwable ->
                    hasAttemptedInitialLoad = true
                    cachedCatalogues = emptyList()
                    cachedErrorMessage = throwable.message ?: "Unable to load catalogues right now."
                    _uiState.value = CatalogueListUiState(
                        catalogues = emptyList(),
                        isLoading = false,
                        errorMessage = cachedErrorMessage
                    )
                }
        }
    }

    fun deleteCatalogue(catalogueId: String) {
        viewModelScope.launch {
            repository.deleteCatalogue(catalogueId)
                .onSuccess { response ->
                    cachedCatalogues = _uiState.value.catalogues.filterNot { it.id == catalogueId }
                    _uiState.value = _uiState.value.copy(
                        catalogues = cachedCatalogues
                    )
                    _events.emit(response.message ?: "Catalogue deleted successfully.")
                }
                .onFailure { throwable ->
                    _events.emit(throwable.message ?: "Unable to delete catalogue.")
                }
        }
    }

    companion object {
        private var cachedCatalogues: List<CatalogueCardUiModel> = emptyList()
        private var cachedErrorMessage: String? = null

        fun clearCache() {
            cachedCatalogues = emptyList()
            cachedErrorMessage = null
        }
    }
}

data class CatalogueListUiState(
    val catalogues: List<CatalogueCardUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
