package com.nice.cataloguevastra.repositories

import android.net.Uri
import com.nice.cataloguevastra.model.AddBackgroundData
import com.nice.cataloguevastra.model.GarmentSubcategoryItem
import com.nice.cataloguevastra.model.CatalogueUiState
import com.nice.cataloguevastra.model.ModelProcessTypesData
import com.nice.cataloguevastra.model.ThemeListItem
import com.nice.cataloguevastra.model.UploadedModelItem

interface CatalogueRepository {
    fun getCatalogueUiState(): CatalogueUiState
    suspend fun getThemeList(): Result<List<ThemeListItem>>
    suspend fun getGarmentSubcategories(themeFor: String): Result<List<GarmentSubcategoryItem>>
    suspend fun getModelProcessTypes(themeFor: String, dressName: String): Result<ModelProcessTypesData>
    suspend fun uploadModel(name: String, category: String, imageUri: Uri): Result<UploadedModelItem>

    suspend fun uploadBackground(name : String, category : String, imageUri : Uri): Result<AddBackgroundData>
}
