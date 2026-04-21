package com.nice.cataloguevastra.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nice.cataloguevastra.BuildConfig
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.model.CatalogueThemeUiModel
import com.nice.cataloguevastra.model.CatalogueUiState
import com.nice.cataloguevastra.model.ChipUiModel
import com.nice.cataloguevastra.model.GarmentSubcategoryUiModel
import com.nice.cataloguevastra.model.ImageRatioItem
import com.nice.cataloguevastra.model.ProcessVisualItem
import com.nice.cataloguevastra.model.RailItemUiModel
import com.nice.cataloguevastra.model.RailSectionUiModel
import com.nice.cataloguevastra.repositories.CatalogueRepository
import com.nice.cataloguevastra.repositories.DummyCatalogueRepository
import kotlinx.coroutines.launch

class CataloguesViewModel(
    private val repository: CatalogueRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(repository.getCatalogueUiState())
    val uiState: LiveData<CatalogueUiState> = _uiState
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    init {
        loadThemes()
    }

    fun ensureInitialStudioData() {
        val currentState = _uiState.value ?: return
        val hasCategories = currentState.categoryOptions.isNotEmpty()
        val hasLoadedVisuals = currentState.modelRail.visualItems().isNotEmpty() ||
            currentState.backgroundRail.visualItems().isNotEmpty() ||
            currentState.poseRail.visualItems().isNotEmpty()

        if (hasCategories && hasLoadedVisuals) return

        val selectedTheme = currentState.catalogueThemes.firstOrNull { theme ->
            theme.name.equals(currentState.selectedCatalogueFor, ignoreCase = true) ||
                theme.themeFor.equals(currentState.selectedCatalogueFor, ignoreCase = true)
        } ?: currentState.catalogueThemes.firstOrNull { theme ->
            theme.name.equals(DEFAULT_THEME_NAME, ignoreCase = true) ||
                theme.themeFor.equals(DEFAULT_THEME_FOR, ignoreCase = true)
        }

        if (selectedTheme != null) {
            loadThemeContent(selectedTheme, clearExistingContent = false)
        } else {
            loadThemes()
        }
    }

    fun updateCatalogueFor(value: String) {
        val currentState = _uiState.value ?: return
        val selectedTheme = currentState.catalogueThemes.firstOrNull { theme ->
            theme.name.equals(value, ignoreCase = true) ||
                theme.title.equals(value, ignoreCase = true) ||
                theme.themeFor.equals(value, ignoreCase = true)
        }

        if (selectedTheme != null) {
            loadThemeContent(selectedTheme, clearExistingContent = true)
        } else {
            _uiState.value = currentState.copy(selectedCatalogueFor = value)
        }
    }

    fun updateCategory(value: String) {
        val currentState = _uiState.value ?: return
        val selectedSubcategory = currentState.garmentSubcategories.firstOrNull { subcategory ->
            subcategory.name.equals(value, ignoreCase = true) || subcategory.id == value
        }

        if (selectedSubcategory != null) {
            _uiState.value = currentState.copy(
                selectedCategory = selectedSubcategory.name,
                modelRail = currentState.modelRail.replaceVisuals(emptyList()),
                backgroundRail = currentState.backgroundRail.replaceVisuals(emptyList()),
                poseRail = currentState.poseRail.replaceVisuals(emptyList())
            )
            loadModelProcessTypes(selectedSubcategory)
        } else {
            _uiState.value = currentState.copy(selectedCategory = value)
        }
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

    fun uploadPickedModel(uri: Uri, fileName: String) {
        val currentState = _uiState.value ?: return
        val selectedTheme = currentState.catalogueThemes.firstOrNull { theme ->
            theme.name.equals(currentState.selectedCatalogueFor, ignoreCase = true) ||
                theme.themeFor.equals(currentState.selectedCatalogueFor, ignoreCase = true)
        }
        val category = selectedTheme?.themeFor
            ?.takeIf { it.isNotBlank() }
            ?: currentState.selectedCatalogueFor.lowercase()

        val modelName = fileName
            .substringBeforeLast('.', fileName)
            .trim()
            .ifBlank { "Custom Model" }

        viewModelScope.launch {
            repository.uploadModel(
                name = modelName,
                category = category,
                imageUri = uri
            ).onSuccess { uploadedModel ->
                val latestState = _uiState.value ?: return@onSuccess
                val uploadedVisual = RailItemUiModel.Visual(
                    id = uploadedModel.id?.toString().orEmpty().ifBlank { "uploaded_model_${System.currentTimeMillis()}" },
                    imageRes = R.drawable.model_img,
                    imageUrl = uploadedModel.image
                        ?.takeIf { it.isNotBlank() }
                        ?: uploadedModel.modelImage.toAbsoluteImageUrl(),
                    imageUri = null,
                    label = modelName,
                    isSelected = true
                )

                _uiState.value = latestState.copy(
                    modelRail = latestState.modelRail.addApiUploadedVisual(uploadedVisual),
                    modelSelection = latestState.modelSelection.copy(
                        selectedModelId = uploadedVisual.id,
                        yourModels = latestState.modelSelection.yourModels.map { it.copy(isSelected = false) } +
                            com.nice.cataloguevastra.model.ModelSheetItemUiModel(
                                id = uploadedVisual.id,
                                label = uploadedVisual.label,
                                imageRes = uploadedVisual.imageRes,
                                imageUrl = uploadedVisual.imageUrl,
                                isSelected = true
                            )
                    )
                )
                _message.value = "Model uploaded successfully."
            }.onFailure { throwable ->
                _message.value = throwable.message ?: "Unable to upload model."
            }
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

    fun uploadPickedBackground(uri: Uri, fileName: String) {
        val currentState = _uiState.value ?: return
        val selectedTheme = currentState.catalogueThemes.firstOrNull { theme ->
            theme.name.equals(currentState.selectedCatalogueFor, ignoreCase = true) ||
                theme.themeFor.equals(currentState.selectedCatalogueFor, ignoreCase = true)
        }
        val category = selectedTheme?.themeFor
            ?.takeIf { it.isNotBlank() }
            ?: currentState.selectedCatalogueFor.lowercase()

        val backgroundName = fileName
            .substringBeforeLast('.', fileName)
            .trim()
            .ifBlank { "Custom Background" }

        viewModelScope.launch {
            repository.uploadBackground(
                name = backgroundName,
                category = category,
                imageUri = uri
            ).onSuccess { uploadedBackground ->
                val latestState = _uiState.value ?: return@onSuccess
                val uploadedVisual = RailItemUiModel.Visual(
                    id = uploadedBackground.id.toString().ifBlank { "uploaded_background_${System.currentTimeMillis()}" },
                    imageRes = R.drawable.placeholder_bg_warm,
                    imageUrl = uploadedBackground.image.takeIf { it.isNotBlank() }
                        ?: uploadedBackground.modelimage.toAbsoluteImageUrl(),
                    imageUri = null,
                    label = backgroundName,
                    isSelected = true
                )

                _uiState.value = latestState.copy(
                    backgroundRail = latestState.backgroundRail.addApiUploadedVisual(uploadedVisual)
                )
                _message.value = "Background uploaded successfully."
            }.onFailure { throwable ->
                _message.value = throwable.message ?: "Unable to upload background."
            }
        }
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

    fun consumeMessage() {
        _message.value = null
    }

    private inline fun updateState(transform: CatalogueUiState.() -> CatalogueUiState) {
        val currentState = _uiState.value ?: return
        _uiState.value = currentState.transform()
    }

    private fun loadThemes() {
        viewModelScope.launch {
            repository.getThemeList()
                .onSuccess { themes ->
                    val themeUiModels = themes.mapNotNull { item ->
                        val themeFor = item.themeFor?.trim().orEmpty()
                        val name = item.name?.trim().orEmpty()
                        if (themeFor.isBlank() || name.isBlank()) return@mapNotNull null

                        CatalogueThemeUiModel(
                            id = item.id?.toString().orEmpty(),
                            themeFor = themeFor,
                            name = name,
                            title = item.title.orEmpty(),
                            imageUrl = item.themeImage.orEmpty()
                        )
                    }

                    if (themeUiModels.isEmpty()) {
                        return@onSuccess
                    }

                    val currentState = _uiState.value ?: return@onSuccess
                    val defaultTheme = themeUiModels.firstOrNull { theme ->
                        theme.name.equals(DEFAULT_THEME_NAME, ignoreCase = true) ||
                            theme.themeFor.equals(DEFAULT_THEME_FOR, ignoreCase = true)
                    } ?: themeUiModels.first()

                    _uiState.value = currentState.copy(
                        catalogueThemes = themeUiModels,
                        catalogueForOptions = themeUiModels.map { it.name },
                        selectedCatalogueFor = defaultTheme.name,
                        garmentSubcategories = emptyList(),
                        categoryOptions = emptyList(),
                        selectedCategory = DROPDOWN_PLACEHOLDER
                    )
                    loadThemeContent(defaultTheme, clearExistingContent = false)
                }
                .onFailure { throwable ->
                    _message.value = throwable.message ?: "Unable to load catalogue themes."
                }
        }
    }

    private fun loadThemeContent(
        selectedTheme: CatalogueThemeUiModel,
        clearExistingContent: Boolean
    ) {
        viewModelScope.launch {
            val baseState = _uiState.value ?: return@launch
            if (clearExistingContent) {
                _uiState.value = baseState.copy(
                    selectedCatalogueFor = selectedTheme.name,
                    garmentSubcategories = emptyList(),
                    categoryOptions = emptyList(),
                    selectedCategory = DROPDOWN_PLACEHOLDER,
                    modelRail = baseState.modelRail.replaceVisuals(emptyList()),
                    backgroundRail = baseState.backgroundRail.replaceVisuals(emptyList()),
                    poseRail = baseState.poseRail.replaceVisuals(emptyList())
                )
            }

            repository.getGarmentSubcategories(selectedTheme.themeFor)
                .onSuccess { items ->
                    val subcategoryUiModels = items.mapNotNull { item ->
                        val name = item.name?.trim().orEmpty()
                        if (name.isBlank()) return@mapNotNull null

                        GarmentSubcategoryUiModel(
                            id = item.id.orEmpty(),
                            themeFor = item.themeFor.orEmpty(),
                            name = name,
                            garmentType = item.garmentType.orEmpty()
                        )
                    }
                    val defaultSubcategory = subcategoryUiModels.firstOrNull()
                    val currentState = _uiState.value ?: return@onSuccess

                    if (defaultSubcategory == null) {
                        _uiState.value = currentState.copy(
                            selectedCatalogueFor = selectedTheme.name,
                            garmentSubcategories = emptyList(),
                            categoryOptions = emptyList(),
                            selectedCategory = DROPDOWN_PLACEHOLDER,
                            modelRail = currentState.modelRail.replaceVisuals(emptyList()),
                            backgroundRail = currentState.backgroundRail.replaceVisuals(emptyList()),
                            poseRail = currentState.poseRail.replaceVisuals(emptyList())
                        )
                        return@onSuccess
                    }

                    _uiState.value = currentState.copy(
                        selectedCatalogueFor = selectedTheme.name,
                        garmentSubcategories = subcategoryUiModels,
                        categoryOptions = subcategoryUiModels.map { it.name },
                        selectedCategory = defaultSubcategory.name,
                        modelRail = currentState.modelRail.replaceVisuals(emptyList()),
                        backgroundRail = currentState.backgroundRail.replaceVisuals(emptyList()),
                        poseRail = currentState.poseRail.replaceVisuals(emptyList())
                    )

                    repository.getModelProcessTypes(
                        themeFor = defaultSubcategory.themeFor,
                        dressName = defaultSubcategory.id
                    ).onSuccess { data ->
                        val latestState = _uiState.value ?: return@onSuccess
                        _uiState.value = latestState.withCatalogueProcessData(
                            selectedTheme = selectedTheme,
                            subcategories = subcategoryUiModels,
                            selectedSubcategory = defaultSubcategory,
                            data = data
                        )
                    }.onFailure { throwable ->
                        val latestState = _uiState.value ?: return@onFailure
                        _uiState.value = latestState.copy(
                            selectedCatalogueFor = selectedTheme.name,
                            garmentSubcategories = subcategoryUiModels,
                            categoryOptions = subcategoryUiModels.map { it.name },
                            selectedCategory = defaultSubcategory.name,
                            modelRail = latestState.modelRail.replaceVisuals(emptyList()),
                            backgroundRail = latestState.backgroundRail.replaceVisuals(emptyList()),
                            poseRail = latestState.poseRail.replaceVisuals(emptyList())
                        )
                        _message.value = throwable.message ?: "Unable to load models and backgrounds."
                    }
                }
                .onFailure { throwable ->
                    val currentState = _uiState.value ?: return@onFailure
                    _uiState.value = currentState.copy(
                        selectedCatalogueFor = selectedTheme.name,
                        garmentSubcategories = emptyList(),
                        categoryOptions = emptyList(),
                        selectedCategory = DROPDOWN_PLACEHOLDER,
                        modelRail = currentState.modelRail.replaceVisuals(emptyList()),
                        backgroundRail = currentState.backgroundRail.replaceVisuals(emptyList()),
                        poseRail = currentState.poseRail.replaceVisuals(emptyList())
                    )
                    _message.value = throwable.message ?: "Unable to load garment subcategories."
                }
        }
    }

    private fun loadModelProcessTypes(selectedSubcategory: GarmentSubcategoryUiModel) {
        viewModelScope.launch {
            repository.getModelProcessTypes(
                themeFor = selectedSubcategory.themeFor,
                dressName = selectedSubcategory.id
            ).onSuccess { data ->
                val currentState = _uiState.value ?: return@onSuccess
                _uiState.value = currentState.withCatalogueProcessData(
                    selectedTheme = currentState.catalogueThemes.firstOrNull {
                        it.themeFor.equals(selectedSubcategory.themeFor, ignoreCase = true)
                    } ?: CatalogueThemeUiModel("", selectedSubcategory.themeFor, currentState.selectedCatalogueFor, "", ""),
                    subcategories = currentState.garmentSubcategories,
                    selectedSubcategory = selectedSubcategory,
                    data = data
                )
            }.onFailure { throwable ->
                _message.value = throwable.message ?: "Unable to load models and backgrounds."
            }
        }
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

    private fun RailSectionUiModel.addApiUploadedVisual(
        visual: RailItemUiModel.Visual
    ): RailSectionUiModel {
        val uploadItems = items.filterIsInstance<RailItemUiModel.Upload>()
        val existingVisuals = items.filterIsInstance<RailItemUiModel.Visual>()
            .map { it.copy(isSelected = false) }
            .filterNot { it.id == visual.id }

        return copy(items = existingVisuals + visual + uploadItems)
    }

    private fun RailSectionUiModel.replaceVisuals(visuals: List<RailItemUiModel.Visual>): RailSectionUiModel {
        val uploadItems = items.filterIsInstance<RailItemUiModel.Upload>()
        return copy(items = visuals + uploadItems)
    }

    private fun RailSectionUiModel.visualItems(): List<RailItemUiModel.Visual> {
        return items.filterIsInstance<RailItemUiModel.Visual>()
    }

    private fun CatalogueUiState.withCatalogueProcessData(
        selectedTheme: CatalogueThemeUiModel,
        subcategories: List<GarmentSubcategoryUiModel>,
        selectedSubcategory: GarmentSubcategoryUiModel,
        data: com.nice.cataloguevastra.model.ModelProcessTypesData
    ): CatalogueUiState {
        val modelVisuals = data.modelTypes.orEmpty().toRailVisuals(
            defaultImageRes = R.drawable.model_img,
            selectedId = modelSelection.selectedModelId
        )
        val backgroundVisuals = data.backgrounds.orEmpty().toRailVisuals(
            defaultImageRes = R.drawable.placeholder_bg_warm
        )
        val poseVisuals = data.fullCatalogue.orEmpty().toRailVisuals(
            defaultImageRes = R.drawable.model_img
        )
        val ratioChips = data.imageRatios.orEmpty().toAspectRatioChips(aspectRatios)
        val libraryItems = if (data.modelTypes.isNullOrEmpty()) {
            modelSelection.libraryItemsByFilter
        } else {
            val apiModelItems = modelVisuals.map { visual ->
                com.nice.cataloguevastra.model.ModelSheetItemUiModel(
                    id = visual.id,
                    label = visual.label,
                    imageRes = visual.imageRes,
                    imageUrl = visual.imageUrl,
                    isSelected = visual.isSelected
                )
            }
            modelSelection.libraryItemsByFilter.mapValues { apiModelItems }
        }

        return copy(
            selectedCatalogueFor = selectedTheme.name.ifBlank { selectedCatalogueFor },
            garmentSubcategories = subcategories,
            categoryOptions = subcategories.map { it.name },
            selectedCategory = selectedSubcategory.name,
            modelRail = modelRail.replaceVisuals(modelVisuals),
            backgroundRail = backgroundRail.replaceVisuals(backgroundVisuals),
            poseRail = poseRail.replaceVisuals(poseVisuals),
            aspectRatios = ratioChips,
            modelSelection = modelSelection.copy(
                selectedModelId = modelVisuals.firstOrNull()?.id ?: modelSelection.selectedModelId,
                libraryItemsByFilter = libraryItems
            )
        )
    }

    private fun List<ProcessVisualItem>.toRailVisuals(
        defaultImageRes: Int,
        selectedId: String? = null
    ): List<RailItemUiModel.Visual> {
        val availableIds = mapNotNull { it.id?.trim()?.takeIf(String::isNotBlank) }
        val fallbackSelectedId = if (!selectedId.isNullOrBlank() && availableIds.contains(selectedId)) {
            selectedId
        } else {
            availableIds.firstOrNull().orEmpty()
        }
        return mapNotNull { item ->
            val id = item.id?.trim().orEmpty()
            if (id.isBlank()) return@mapNotNull null

            RailItemUiModel.Visual(
                id = id,
                imageRes = defaultImageRes,
                imageUrl = item.modelImage.toAbsoluteImageUrl(),
                label = item.title.orEmpty().ifBlank { "Item $id" },
                isSelected = id == fallbackSelectedId
            )
        }
    }

    private fun List<ImageRatioItem>.toAspectRatioChips(
        existingChips: List<ChipUiModel>
    ): List<ChipUiModel> {
        if (isEmpty()) return existingChips

        val selectedId = existingChips.firstOrNull { it.isSelected }?.id ?: firstOrNull()?.id?.toString().orEmpty()
        return mapNotNull { item ->
            val id = item.id?.toString().orEmpty()
            val title = item.title?.trim().orEmpty()
            if (id.isBlank() || title.isBlank()) return@mapNotNull null

            ChipUiModel(
                id = id,
                label = title.replace('.', ':'),
                isSelected = id == selectedId
            )
        }
    }

    private fun String?.toAbsoluteImageUrl(): String? {
        val value = this?.trim().orEmpty()
        if (value.isBlank()) return null
        return if (value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)) {
            value
        } else {
            BuildConfig.BASE_URL.trimEnd('/').plus("/").plus(value.trimStart('/'))
        }
    }

    companion object {
        const val DROPDOWN_PLACEHOLDER = "Select"
        private const val DEFAULT_THEME_NAME = "Women"
        private const val DEFAULT_THEME_FOR = "women"

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
