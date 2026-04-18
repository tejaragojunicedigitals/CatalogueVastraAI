package com.nice.cataloguevastra.ui.catalogues

import android.net.Uri
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

    fun addUploadedModel(uri: Uri, label: String) = updateState {
        copy(
            modelRail = modelRail.addUploadedVisual(
                idPrefix = "uploaded_model",
                uri = uri,
                label = label
            ),
            modelSelection = modelSelection.copy(
                selectedModelId = "uploaded_model_${System.currentTimeMillis()}"
            )
        ).let { updated ->
            val selectedId = (updated.modelRail.items.firstOrNull { item ->
                item is RailItemUiModel.Visual && item.imageUri == uri
            } as? RailItemUiModel.Visual)?.id ?: updated.modelSelection.selectedModelId
            updated.copy(modelSelection = updated.modelSelection.copy(selectedModelId = selectedId))
        }
    }

    fun addUploadedBackground(uri: Uri, label: String) = updateState {
        copy(
            backgroundRail = backgroundRail.addUploadedVisual(
                idPrefix = "uploaded_background",
                uri = uri,
                label = label
            )
        )
    }

    fun selectPose(id: String) = updateState {
        copy(poseRail = poseRail.updateVisualSelection(id))
    }

    fun updateProductCode(code: String) = updateState {
        copy(productCode = code)
    }

    fun updateBusinessLogoName(name: String) = updateState {
        copy(businessLogoName = name)
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

    private fun RailSectionUiModel.addUploadedVisual(
        idPrefix: String,
        uri: Uri,
        label: String
    ): RailSectionUiModel {
        val newId = "${idPrefix}_${System.currentTimeMillis()}"
        val uploadItem = items.filterIsInstance<RailItemUiModel.Upload>().firstOrNull()
        val visuals = items.filterIsInstance<RailItemUiModel.Visual>().map { it.copy(isSelected = false) }
        val uploadedItem = RailItemUiModel.Visual(
            id = newId,
            imageRes = 0,
            imageUri = uri,
            label = label,
            isSelected = true
        )

        return copy(
            items = buildList {
                add(uploadedItem)
                addAll(visuals)
                if (uploadItem != null) add(uploadItem)
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
