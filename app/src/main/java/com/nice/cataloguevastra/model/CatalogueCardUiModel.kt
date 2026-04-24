package com.nice.cataloguevastra.model

data class CatalogueCardUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val previewImageRes: Int,
    val previewImageUrl: String? = null,
    val thumbnails: List<CatalogueImageUiModel> = emptyList(),
    val createdDateIso: String,
    val categoryTag: String,
    val platformTag: String
)

data class CatalogueImageUiModel(
    val url: String? = null,
    val imageRes: Int? = null
)
