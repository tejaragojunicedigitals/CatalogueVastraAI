package com.nice.cataloguevastra.model

import java.time.LocalDate

data class AssetCardUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val previewImageRes: Int,
    val previewImageUrl: String? = null,
    val thumbnails: List<AssetImageUiModel> = emptyList(),
    val tabType: AssetTabType,
    val uploadedDate: LocalDate
)

enum class AssetTabType(val label: String) {
    PRODUCTS("Products"),
    MODELS("Models"),
    BACKGROUNDS("Backgrounds")
}

enum class AssetSortType {
    LATEST,
    OLDEST
}

data class AssetImageUiModel(
    val url: String? = null,
    val imageRes: Int? = null
)
