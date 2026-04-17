package com.nice.cataloguevastra.ui.catalogues.model

data class CatalogueUiState(
    val catalogueForOptions: List<String>,
    val selectedCatalogueFor: String,
    val categoryOptions: List<String>,
    val selectedCategory: String,
    val outfitTypeOptions: List<String>,
    val selectedOutfitType: String,
    val platforms: List<ChipUiModel>,
    val aspectRatios: List<ChipUiModel>,
    val modelRail: RailSectionUiModel,
    val backgroundRail: RailSectionUiModel,
    val poseRail: RailSectionUiModel,
    val businessLogoName: String,
    val productCode: String,
    val modelSelection: ModelSelectionUiModel
)

data class ChipUiModel(
    val id: String,
    val label: String,
    val isSelected: Boolean
)

data class RailSectionUiModel(
    val items: List<RailItemUiModel>
)

sealed interface RailItemUiModel {
    val id: String

    data class Visual(
        override val id: String,
        val imageRes: Int,
        val label: String,
        val isSelected: Boolean
    ) : RailItemUiModel

    data class Upload(
        override val id: String,
        val title: String,
        val subtitle: String
    ) : RailItemUiModel
}

data class ModelSelectionUiModel(
    val tabs: List<ModelTabType>,
    val selectedModelId: String,
    val libraryFilters: List<ChipUiModel>,
    val libraryItemsByFilter: Map<ModelLibraryFilterType, List<ModelSheetItemUiModel>>,
    val yourModels: List<ModelSheetItemUiModel>
)

data class ModelSheetItemUiModel(
    val id: String,
    val label: String,
    val imageRes: Int,
    val isSelected: Boolean = false
)

enum class ModelTabType(val label: String) {
    LIBRARY("Library"),
    YOUR_MODELS("Your Models")
}

enum class ModelLibraryFilterType(val id: String, val label: String) {
    ALL("all", "All"),
    SOUTH_INDIAN("south_indian", "South Indian"),
    NORTH_INDIAN("north_indian", "North Indian"),
    INTERNATIONAL("international", "International")
}
