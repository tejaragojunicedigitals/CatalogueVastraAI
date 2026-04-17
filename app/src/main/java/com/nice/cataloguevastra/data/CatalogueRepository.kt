package com.nice.cataloguevastra.data

import com.nice.cataloguevastra.ui.catalogues.model.CatalogueUiState

interface CatalogueRepository {
    fun getCatalogueUiState(): CatalogueUiState
}
