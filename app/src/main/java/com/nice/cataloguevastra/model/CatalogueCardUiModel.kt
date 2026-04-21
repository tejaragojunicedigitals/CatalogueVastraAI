package com.nice.cataloguevastra.model

data class CatalogueCardUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val previewImageRes: Int,
    val thumbnails: List<Int>,
    val createdDateIso: String,
    val categoryTag: String,
    val platformTag: String
)