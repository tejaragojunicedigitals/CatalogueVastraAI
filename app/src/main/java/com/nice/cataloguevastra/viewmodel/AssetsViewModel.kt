package com.nice.cataloguevastra.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nice.cataloguevastra.CatalogueVastraApp
import com.nice.cataloguevastra.model.AssetCardUiModel
import com.nice.cataloguevastra.model.AssetSortType
import com.nice.cataloguevastra.model.AssetTabType
import com.nice.cataloguevastra.repositories.AssetsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AssetsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: AssetsRepository =
        (application as CatalogueVastraApp).appContainer.assetsRepository

    private val _uiState = MutableStateFlow(AssetsUiState())
    val uiState: StateFlow<AssetsUiState> = _uiState.asStateFlow()

    private var allAssets: List<AssetCardUiModel> = emptyList()
    private var lastRequestKey: String? = null

    fun loadAssets(category: String, dressName: String?, forceRefresh: Boolean = false) {
        val normalizedCategory = category.trim().lowercase()
        if (normalizedCategory.isBlank()) return

        val normalizedDressName = dressName?.trim()?.takeIf { it.isNotBlank() }
        val requestKey = "$normalizedCategory|${normalizedDressName.orEmpty()}"
        if (_uiState.value.isLoading && requestKey == lastRequestKey) return
        if (!forceRefresh && requestKey == lastRequestKey && allAssets.isNotEmpty()) return

        val requestCategories = if (normalizedCategory == ALL_CATEGORIES_KEY) {
            CATEGORY_API_VALUES
        } else {
            listOf(normalizedCategory)
        }

        lastRequestKey = requestKey
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            val loadedAssets = mutableListOf<AssetCardUiModel>()
            var firstError: Throwable? = null
            requestCategories.forEach { apiCategory ->
                repository.getAssets(
                    category = apiCategory,
                    dressName = normalizedDressName
                ).onSuccess { assets ->
                    loadedAssets += assets
                }.onFailure { throwable ->
                    if (firstError == null) firstError = throwable
                }
            }

            if (loadedAssets.isNotEmpty() || firstError == null) {
                allAssets = loadedAssets.distinctBy { it.id }
                val baseState = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                _uiState.value = withContext(Dispatchers.Default) {
                    baseState.withVisibleAssets(allAssets)
                }   
            } else {
                allAssets = emptyList()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    visibleAssets = emptyList(),
                    errorMessage = firstError?.message ?: "Unable to load assets right now."
                )
            }
        }
    }

    fun selectTab(tabType: AssetTabType) {
        if (_uiState.value.selectedTab == tabType) return
        viewModelScope.launch {
            val baseState = _uiState.value.copy(selectedTab = tabType)
            _uiState.value = withContext(Dispatchers.Default) {
                baseState.withVisibleAssets(allAssets)
            }
        }
    }

    fun selectSort(sortType: AssetSortType) {
        if (_uiState.value.selectedSort == sortType) return
        viewModelScope.launch {
            val baseState = _uiState.value.copy(selectedSort = sortType)
            _uiState.value = withContext(Dispatchers.Default) {
                baseState.withVisibleAssets(allAssets)
            }
        }
    }

    private fun AssetsUiState.withVisibleAssets(allAssets: List<AssetCardUiModel>): AssetsUiState {
        val filteredAssets = allAssets
            .filter { it.tabType == selectedTab }
            .sortedWith(
                if (selectedSort == AssetSortType.LATEST) {
                    compareByDescending<AssetCardUiModel> { it.uploadedDate }.thenBy { it.title }
                } else {
                    compareBy<AssetCardUiModel> { it.uploadedDate }.thenBy { it.title }
                }
            )

        return copy(
            visibleAssets = filteredAssets,
            errorMessage = if (filteredAssets.isNotEmpty()) null else errorMessage
        )
    }

    private companion object {
        const val ALL_CATEGORIES_KEY = "all"
        val CATEGORY_API_VALUES = listOf("women", "men", "girl", "boy")
    }
}

data class AssetsUiState(
    val selectedTab: AssetTabType = AssetTabType.PRODUCTS,
    val selectedSort: AssetSortType = AssetSortType.LATEST,
    val visibleAssets: List<AssetCardUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
