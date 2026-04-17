package com.nice.cataloguevastra.ui.catalogues

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nice.cataloguevastra.data.CatalogueRepository
import com.nice.cataloguevastra.data.DummyCatalogueRepository
import com.nice.cataloguevastra.ui.catalogues.model.CatalogueUiState
import com.nice.cataloguevastra.ui.catalogues.model.ChipUiModel
import com.nice.cataloguevastra.ui.catalogues.model.RailItemUiModel
import com.nice.cataloguevastra.ui.catalogues.model.RailSectionUiModel

class CataloguesViewModel(
    private val repository: CatalogueRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(repository.getCatalogueUiState())
    val uiState: LiveData<CatalogueUiState> = _uiState

    fun updateCatalogueFor(value: String) = updateState {
        copy(selectedCatalogueFor = value)
    }

    fun updateCategory(value: String) = updateState {
        copy(selectedCategory = value)
    }

    fun updateOutfitType(value: String) = updateState {
        copy(selectedOutfitType = value)
    }

    fun selectPlatform(id: String) = updateState {
        copy(platforms = platforms.singleSelect(id))
    }

    fun selectAspectRatio(id: String) = updateState {
        copy(aspectRatios = aspectRatios.singleSelect(id))
    }

    fun selectModel(id: String) = updateState {
        copy(
            modelRail = modelRail.updateVisualSelection(id),
            modelSelection = modelSelection.copy(selectedModelId = id)
        )
    }

    fun selectBackground(id: String) = updateState {
        copy(backgroundRail = backgroundRail.updateVisualSelection(id))
    }

    fun selectPose(id: String) = updateState {
        copy(poseRail = poseRail.updateVisualSelection(id))
    }

    fun updateProductCode(code: String) = updateState {
        copy(productCode = code)
    }

    private inline fun updateState(transform: CatalogueUiState.() -> CatalogueUiState) {
        val currentState = _uiState.value ?: return
        _uiState.value = currentState.transform()
    }

    private fun List<ChipUiModel>.singleSelect(selectedId: String): List<ChipUiModel> {
        return map { item -> item.copy(isSelected = item.id == selectedId) }
    }

    private fun RailSectionUiModel.updateVisualSelection(selectedId: String): RailSectionUiModel {
        return copy(
            items = items.map { item ->
                when (item) {
                    is RailItemUiModel.Upload -> item
                    is RailItemUiModel.Visual -> item.copy(isSelected = item.id == selectedId)
                }
            }
        )
    }

    companion object {
        fun factory(
            repository: CatalogueRepository = DummyCatalogueRepository()
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CataloguesViewModel(repository) as T
                }
            }
        }
    }
}
