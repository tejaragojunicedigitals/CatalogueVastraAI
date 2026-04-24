package com.nice.cataloguevastra.repositories

import android.net.Uri
import com.nice.cataloguevastra.model.AddBackgroundData
import com.nice.cataloguevastra.model.CatalogueCardUiModel
import com.nice.cataloguevastra.model.GarmentSubcategoryItem
import com.nice.cataloguevastra.model.CatalogueUiState
import com.nice.cataloguevastra.model.DeleteCatalogueResponse
import com.nice.cataloguevastra.model.GenerateCatalogueProgress
import com.nice.cataloguevastra.model.GenerateCatalogueResult
import com.nice.cataloguevastra.model.ModelProcessTypesData
import com.nice.cataloguevastra.model.ThemeListItem
import com.nice.cataloguevastra.model.UploadedModelItem

interface CatalogueRepository {
    fun getCatalogueUiState(): CatalogueUiState
    suspend fun getCatalogues(page: Int = 1, format: String = "json"): Result<List<CatalogueCardUiModel>>
    suspend fun getThemeList(): Result<List<ThemeListItem>>
    suspend fun getGarmentSubcategories(themeFor: String): Result<List<GarmentSubcategoryItem>>
    suspend fun getModelProcessTypes(themeFor: String, dressName: String): Result<ModelProcessTypesData>
    suspend fun uploadModel(name: String, category: String, imageUri: Uri): Result<UploadedModelItem>

    suspend fun uploadBackground(name : String, category : String, imageUri : Uri): Result<AddBackgroundData>

    suspend fun generateCatalogue(
        params: GenerateCatalogueParams,
        onProgress: suspend (GenerateCatalogueProgress) -> Unit = {}
    ): Result<GenerateCatalogueResult>

    suspend fun deleteCatalogue(catalogueId: String): Result<DeleteCatalogueResponse>
}

data class GenerateCatalogueParams(
    val catalogueFor: String,
    val dressType: String,
    val modelId: String,
    val backgroundId: String,
    val platform: String,
    val aspectRatio: String,
    val width: String,
    val height: String,
    val poseIds: List<String>,
    val productImageUri: Uri,
    val poseGarmentImages: Map<String, Uri>,
    val sameGarmentPoseIds: List<String>,
    val title: String
)
