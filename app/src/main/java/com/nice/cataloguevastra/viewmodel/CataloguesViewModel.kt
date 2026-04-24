package com.nice.cataloguevastra.viewmodel

import android.net.Uri
import android.util.Log
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
import com.nice.cataloguevastra.model.GenerateCatalogueResult
import com.nice.cataloguevastra.model.GenerateCatalogueProgress
import com.nice.cataloguevastra.model.GarmentSubcategoryUiModel
import com.nice.cataloguevastra.model.ProcessVisualItem
import com.nice.cataloguevastra.model.RailItemUiModel
import com.nice.cataloguevastra.model.RailSectionUiModel
import com.nice.cataloguevastra.repositories.CatalogueRepository
import com.nice.cataloguevastra.repositories.DummyCatalogueRepository
import com.nice.cataloguevastra.repositories.GenerateCatalogueParams
import kotlinx.coroutines.launch
import kotlin.math.abs

class CataloguesViewModel(
    private val repository: CatalogueRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(repository.getCatalogueUiState())
    val uiState: LiveData<CatalogueUiState> = _uiState
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message
    private val _generatedCatalogue = MutableLiveData<GenerateCatalogueResult?>()
    val generatedCatalogue: LiveData<GenerateCatalogueResult?> = _generatedCatalogue
    private val _generateProgress = MutableLiveData<GenerateCatalogueProgress?>()
    val generateProgress: LiveData<GenerateCatalogueProgress?> = _generateProgress
    private val _generateError = MutableLiveData<String?>()
    val generateError: LiveData<String?> = _generateError

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
        val normalizedPlatformId = id.trim().lowercase()
        val defaultRatio = PLATFORM_DEFAULT_RATIO[normalizedPlatformId] ?: DEFAULT_ASPECT_RATIO_LABEL
        syncResolution(
            selectedPlatforms = platforms.singleSelect(normalizedPlatformId),
            selectedAspectRatios = buildAspectRatioChips(defaultRatio)
        )
    }

    fun selectAspectRatio(id: String) = updateState {
        syncResolution(selectedAspectRatios = buildAspectRatioChips(id.normalizeAspectRatioLabel()))
    }

    fun updateResolutionWidth(value: String) = updateState {
        if (!isResolutionEditable) return@updateState this
        copy(resolutionWidth = value.onlyDigits())
    }

    fun updateResolutionHeight(value: String) = updateState {
        if (!isResolutionEditable) return@updateState this
        copy(resolutionHeight = value.onlyDigits())
    }

    fun swapResolution() = updateState {
        val width = resolutionWidth.toIntOrNull() ?: return@updateState this
        val height = resolutionHeight.toIntOrNull() ?: return@updateState this
        val platformId = selectedPlatformId()
        val swappedWidth = height
        val swappedHeight = width
        val matchedRatio = findRatioForDimensions(platformId, swappedWidth, swappedHeight)

        if (matchedRatio != null) {
            syncResolution(selectedAspectRatios = buildAspectRatioChips(matchedRatio))
        } else {
            copy(
                aspectRatios = buildAspectRatioChips(CUSTOM_RATIO_LABEL),
                resolutionWidth = swappedWidth.toString(),
                resolutionHeight = swappedHeight.toString(),
                isResolutionEditable = true
            )
        }
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
        copy(poseRail = poseRail.toggleVisualSelection(id))
    }

    fun updateProductCode(code: String) = updateState {
        copy(productCode = code)
    }

    fun updateBusinessLogoName(name: String) = updateState {
        copy(businessLogoName = name)
    }

    fun generateCatalogue(params: GenerateCatalogueParams) {
        viewModelScope.launch {
            updateLoading(true)
            _generatedCatalogue.value = null
            _generateError.value = null
            _generateProgress.value = GenerateCatalogueProgress(
                completed = 0,
                total = params.poseIds.size.coerceAtLeast(1),
                message = "Preparing generation..."
            )
            repository.generateCatalogue(params) { progress ->
                _generateProgress.postValue(progress)
            }
                .onSuccess { result ->
                    _generateProgress.value = GenerateCatalogueProgress(
                        completed = result.imageUrls.size,
                        total = result.imageUrls.size.coerceAtLeast(1),
                        message = "All images are ready."
                    )
                    updateLoading(false)
                    _generatedCatalogue.value = result
                }
                .onFailure { throwable ->
                    updateLoading(false)
                    _generateProgress.value = null
                    val errorMessage = throwable.message ?: "Unable to generate catalogue."
                    _generateError.value = errorMessage
                    _message.value = errorMessage
                }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun consumeGeneratedCatalogue() {
        _generatedCatalogue.value = null
        _generateProgress.value = null
    }

    fun consumeGenerateError() {
        _generateError.value = null
    }

    private inline fun updateState(transform: CatalogueUiState.() -> CatalogueUiState) {
        val currentState = _uiState.value ?: return
        _uiState.value = currentState.transform()
    }

    private fun updateLoading(isLoading: Boolean) {
        val currentState = _uiState.value ?: return
        if (currentState.isLoading != isLoading) {
            _uiState.value = currentState.copy(isLoading = isLoading)
        }
    }

    private fun loadThemes() {
        viewModelScope.launch {
            updateLoading(true)
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
                        updateLoading(false)
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
                    updateLoading(false)
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
            _uiState.value = if (clearExistingContent) {
                baseState.copy(
                    selectedCatalogueFor = selectedTheme.name,
                    garmentSubcategories = emptyList(),
                    categoryOptions = emptyList(),
                    selectedCategory = DROPDOWN_PLACEHOLDER,
                    modelRail = baseState.modelRail.replaceVisuals(emptyList()),
                    backgroundRail = baseState.backgroundRail.replaceVisuals(emptyList()),
                    poseRail = baseState.poseRail.replaceVisuals(emptyList()),
                    isLoading = true
                )
            } else {
                baseState.copy(isLoading = true)
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
                            poseRail = currentState.poseRail.replaceVisuals(emptyList()),
                            isLoading = false
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
                        poseRail = currentState.poseRail.replaceVisuals(emptyList()),
                        isLoading = true
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
                        ).copy(isLoading = false)
                    }.onFailure { throwable ->
                        val latestState = _uiState.value ?: return@onFailure
                        _uiState.value = latestState.copy(
                            selectedCatalogueFor = selectedTheme.name,
                            garmentSubcategories = subcategoryUiModels,
                            categoryOptions = subcategoryUiModels.map { it.name },
                            selectedCategory = defaultSubcategory.name,
                            modelRail = latestState.modelRail.replaceVisuals(emptyList()),
                            backgroundRail = latestState.backgroundRail.replaceVisuals(emptyList()),
                            poseRail = latestState.poseRail.replaceVisuals(emptyList()),
                            isLoading = false
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
                        poseRail = currentState.poseRail.replaceVisuals(emptyList()),
                        isLoading = false
                    )
                    _message.value = throwable.message ?: "Unable to load garment subcategories."
                }
        }
    }

    private fun loadModelProcessTypes(selectedSubcategory: GarmentSubcategoryUiModel) {
        viewModelScope.launch {
            updateLoading(true)
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
                ).copy(isLoading = false)
            }.onFailure { throwable ->
                updateLoading(false)
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

    private fun RailSectionUiModel.toggleVisualSelection(selectedId: String): RailSectionUiModel {
        return copy(
            items = items.map { item ->
                when (item) {
                    is RailItemUiModel.Upload -> item
                    is RailItemUiModel.Visual -> {
                        if (item.id == selectedId) {
                            item.copy(isSelected = !item.isSelected)
                        } else {
                            item
                        }
                    }
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
        val modelSource = (
            data.models.orEmpty() +
                data.modelTypes.orEmpty() +
                data.customModels.orEmpty()
            ).distinctVisualsById()
        val backgroundSource = (
            data.backgrounds.orEmpty() +
                data.customBackgrounds.orEmpty()
            ).distinctVisualsById()
        val poseSource = (
            data.poses.orEmpty() +
                data.fullCatalogue.orEmpty() +
                data.customPoses.orEmpty()
            ).distinctVisualsById()
        val hasDressSpecificPoses = poseSource.any { item -> !item.dressName.isNullOrBlank() }
        val dressFilteredPoses = if (hasDressSpecificPoses) {
            poseSource.filter { item -> item.dressName == selectedSubcategory.id }
        } else {
            poseSource
        }

        if (BuildConfig.DEBUG) {
            Log.d(
                LOG_TAG,
                "Studio assets: catalogue_for=${selectedTheme.themeFor}, dress_type=${selectedSubcategory.id}, " +
                    "models=${modelSource.debugIds()}, backgrounds=${backgroundSource.debugIds()}, " +
                    "poses=${dressFilteredPoses.debugIdsWithDress()}"
            )
        }

        val modelVisuals = modelSource.toRailVisuals(
            defaultImageRes = R.drawable.model_img,
            selectedId = modelSelection.selectedModelId
        )
        val backgroundVisuals = backgroundSource.toRailVisuals(
            defaultImageRes = R.drawable.placeholder_bg_warm
        )
        val poseVisuals = dressFilteredPoses.toRailVisuals(
            defaultImageRes = R.drawable.model_img,
            selectFirstByDefault = false
        )
        val ratioChips = buildAspectRatioChips(selectedAspectRatioLabel())
        val libraryItems = if (modelSource.isEmpty()) {
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
                selectedModelId = modelVisuals.firstOrNull { it.isSelected }?.id
                    ?: modelVisuals.firstOrNull()?.id
                    ?: modelSelection.selectedModelId,
                libraryItemsByFilter = libraryItems
            )
        ).syncResolution(selectedAspectRatios = ratioChips)
    }

    private fun List<ProcessVisualItem>.toRailVisuals(
        defaultImageRes: Int,
        selectedId: String? = null,
        selectFirstByDefault: Boolean = true
    ): List<RailItemUiModel.Visual> {
        val availableIds = mapNotNull { it.id?.trim()?.takeIf(String::isNotBlank) }
        val fallbackSelectedId = if (!selectedId.isNullOrBlank() && availableIds.contains(selectedId)) {
            selectedId
        } else if (selectFirstByDefault) {
            availableIds.firstOrNull().orEmpty()
        } else {
            ""
        }
        return mapNotNull { item ->
            val id = item.id?.trim().orEmpty()
            if (id.isBlank()) return@mapNotNull null

            RailItemUiModel.Visual(
                id = id,
                imageRes = defaultImageRes,
                imageUrl = item.bestImagePath().toAbsoluteImageUrl(),
                label = item.displayName().ifBlank { "Item $id" },
                isSelected = id == fallbackSelectedId
            )
        }
    }

    private fun List<ProcessVisualItem>.distinctVisualsById(): List<ProcessVisualItem> {
        return distinctBy { item -> item.id?.trim().orEmpty() }
    }

    private fun List<ProcessVisualItem>.debugIds(): String {
        return joinToString(prefix = "[", postfix = "]") { item ->
            item.id?.trim().orEmpty()
        }
    }

    private fun List<ProcessVisualItem>.debugIdsWithDress(): String {
        return joinToString(prefix = "[", postfix = "]") { item ->
            val id = item.id?.trim().orEmpty()
            val dress = item.dressName?.trim().orEmpty()
            if (dress.isBlank()) id else "$id(dress=$dress)"
        }
    }

    private fun ProcessVisualItem.bestImagePath(): String? {
        return image
            ?: thumbnail
            ?: modelImageLegacy
            ?: modelImage
    }

    private fun ProcessVisualItem.displayName(): String {
        return name?.trim().orEmpty()
            .ifBlank { title?.trim().orEmpty() }
    }

    private fun buildAspectRatioChips(selectedRatioLabel: String): List<ChipUiModel> {
        val normalizedSelectedRatio = selectedRatioLabel.normalizeAspectRatioLabel()
            .takeIf { ASPECT_RATIO_ORDER.contains(it) }
            ?: DEFAULT_ASPECT_RATIO_LABEL

        return ASPECT_RATIO_ORDER.map { label ->
            ChipUiModel(
                id = label,
                label = label,
                isSelected = label == normalizedSelectedRatio
            )
        }
    }

    private fun String?.normalizeAspectRatioLabel(): String {
        return this?.trim().orEmpty().replace('.', ':')
    }

    private fun String.onlyDigits(): String {
        return filter(Char::isDigit).take(5)
    }

    private fun CatalogueUiState.selectedPlatformId(): String {
        return platforms.firstOrNull { it.isSelected }?.id?.trim()?.lowercase() ?: DEFAULT_PLATFORM_ID
    }

    private fun CatalogueUiState.selectedAspectRatioLabel(): String {
        return aspectRatios.firstOrNull { it.isSelected }
            ?.label
            ?.normalizeAspectRatioLabel()
            ?.takeIf { it.isNotBlank() }
            ?: (PLATFORM_DEFAULT_RATIO[selectedPlatformId()] ?: DEFAULT_ASPECT_RATIO_LABEL)
    }

    private fun CatalogueUiState.syncResolution(
        selectedPlatforms: List<ChipUiModel> = platforms,
        selectedAspectRatios: List<ChipUiModel> = aspectRatios
    ): CatalogueUiState {
        val platformId = selectedPlatforms.firstOrNull { it.isSelected }?.id?.trim()?.lowercase()
            ?: DEFAULT_PLATFORM_ID
        val ratioLabel = selectedAspectRatios.firstOrNull { it.isSelected }
            ?.label
            ?.normalizeAspectRatioLabel()
            ?: DEFAULT_ASPECT_RATIO_LABEL
        val preset = resolutionFor(platformId, ratioLabel)

        return copy(
            platforms = selectedPlatforms,
            aspectRatios = selectedAspectRatios,
            resolutionWidth = preset.w.toString(),
            resolutionHeight = preset.h.toString(),
            isResolutionEditable = ratioLabel == CUSTOM_RATIO_LABEL
        )
    }

    private fun resolutionFor(platformId: String, ratioLabel: String): ResolutionPreset {
        return PLATFORM_RESOLUTION_OVERRIDES[platformId]?.get(ratioLabel)
            ?: RESOLUTION_PRESETS[ratioLabel]
            ?: RESOLUTION_PRESETS.getValue(CUSTOM_RATIO_LABEL)
    }

    private fun findRatioForDimensions(platformId: String, width: Int, height: Int): String? {
        return ASPECT_RATIO_ORDER
            .filterNot { it == CUSTOM_RATIO_LABEL }
            .firstOrNull { ratioLabel ->
                val preset = resolutionFor(platformId, ratioLabel)
                preset.w == width && preset.h == height
            } ?: ASPECT_RATIO_ORDER
            .filterNot { it == CUSTOM_RATIO_LABEL }
            .firstOrNull { ratioLabel ->
                val preset = resolutionFor(platformId, ratioLabel)
                abs((preset.w.toLong() * height) - (preset.h.toLong() * width)) == 0L
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
        private const val DEFAULT_ASPECT_RATIO_LABEL = "1:1"
        private const val CUSTOM_RATIO_LABEL = "Custom"
        private const val DEFAULT_PLATFORM_ID = "amazon"
        private const val DEFAULT_THEME_NAME = "Women"
        private const val DEFAULT_THEME_FOR = "women"
        private const val LOG_TAG = "CataloguesViewModel"
        private val ASPECT_RATIO_ORDER = listOf(
            "1:1",
            "3:4",
            "4:3",
            "4:5",
            "16:9",
            "9:16",
            CUSTOM_RATIO_LABEL
        )
        private val RESOLUTION_PRESETS = mapOf(
            "1:1" to ResolutionPreset(2000, 2000),
            "3:4" to ResolutionPreset(1500, 2000),
            "4:3" to ResolutionPreset(2000, 1500),
            "4:5" to ResolutionPreset(1600, 2000),
            "16:9" to ResolutionPreset(2048, 1152),
            "9:16" to ResolutionPreset(1152, 2048),
            CUSTOM_RATIO_LABEL to ResolutionPreset(2000, 2000)
        )
        private val PLATFORM_DEFAULT_RATIO = mapOf(
            "amazon" to "1:1",
            "flipkart" to "1:1",
            "ebay" to "1:1",
            "shopify" to "1:1",
            "myntra" to "3:4",
            "nykaa fashion" to "3:4",
            "etsy" to "1:1",
            "custom" to "1:1"
        )
        private val PLATFORM_RESOLUTION_OVERRIDES = mapOf(
            "amazon" to mapOf(
                "1:1" to ResolutionPreset(2000, 2000),
                "3:4" to ResolutionPreset(1500, 2000),
                "4:3" to ResolutionPreset(2000, 1500),
                "4:5" to ResolutionPreset(1600, 2000),
                "16:9" to ResolutionPreset(2000, 1125),
                "9:16" to ResolutionPreset(1125, 2000),
                CUSTOM_RATIO_LABEL to ResolutionPreset(2000, 2000)
            ),
            "flipkart" to mapOf(
                "1:1" to ResolutionPreset(1500, 1500),
                "3:4" to ResolutionPreset(1125, 1500),
                "4:3" to ResolutionPreset(1500, 1125),
                "4:5" to ResolutionPreset(1200, 1500),
                "16:9" to ResolutionPreset(1500, 844),
                "9:16" to ResolutionPreset(844, 1500),
                CUSTOM_RATIO_LABEL to ResolutionPreset(1500, 1500)
            ),
            "ebay" to mapOf(
                "1:1" to ResolutionPreset(1600, 1600),
                "3:4" to ResolutionPreset(1200, 1600),
                "4:3" to ResolutionPreset(1600, 1200),
                "4:5" to ResolutionPreset(1280, 1600),
                "16:9" to ResolutionPreset(1600, 900),
                "9:16" to ResolutionPreset(900, 1600),
                CUSTOM_RATIO_LABEL to ResolutionPreset(1600, 1600)
            ),
            "shopify" to mapOf(
                "1:1" to ResolutionPreset(2048, 2048),
                "3:4" to ResolutionPreset(1536, 2048),
                "4:3" to ResolutionPreset(2048, 1536),
                "4:5" to ResolutionPreset(1638, 2048),
                "16:9" to ResolutionPreset(2048, 1152),
                "9:16" to ResolutionPreset(1152, 2048),
                CUSTOM_RATIO_LABEL to ResolutionPreset(2048, 2048)
            ),
            "myntra" to mapOf(
                "1:1" to ResolutionPreset(2000, 2000),
                "3:4" to ResolutionPreset(1500, 2000),
                "4:3" to ResolutionPreset(2000, 1500),
                "4:5" to ResolutionPreset(1600, 2000),
                "16:9" to ResolutionPreset(2000, 1125),
                "9:16" to ResolutionPreset(1125, 2000),
                CUSTOM_RATIO_LABEL to ResolutionPreset(1500, 2000)
            ),
            "nykaa fashion" to mapOf(
                "1:1" to ResolutionPreset(2000, 2000),
                "3:4" to ResolutionPreset(1500, 2000),
                "4:3" to ResolutionPreset(2000, 1500),
                "4:5" to ResolutionPreset(1600, 2000),
                "16:9" to ResolutionPreset(2000, 1125),
                "9:16" to ResolutionPreset(1125, 2000),
                CUSTOM_RATIO_LABEL to ResolutionPreset(1500, 2000)
            ),
            "etsy" to mapOf(
                "1:1" to ResolutionPreset(2000, 2000),
                "3:4" to ResolutionPreset(1500, 2000),
                "4:3" to ResolutionPreset(2000, 1500),
                "4:5" to ResolutionPreset(1600, 2000),
                "16:9" to ResolutionPreset(2000, 1125),
                "9:16" to ResolutionPreset(1125, 2000),
                CUSTOM_RATIO_LABEL to ResolutionPreset(2000, 2000)
            ),
            "custom" to mapOf(
                "1:1" to ResolutionPreset(2000, 2000),
                "3:4" to ResolutionPreset(1500, 2000),
                "4:3" to ResolutionPreset(2000, 1500),
                "4:5" to ResolutionPreset(1600, 2000),
                "16:9" to ResolutionPreset(2048, 1152),
                "9:16" to ResolutionPreset(1152, 2048),
                CUSTOM_RATIO_LABEL to ResolutionPreset(2000, 2000)
            )
        )

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

    private data class ResolutionPreset(
        val w: Int,
        val h: Int
    )
}
