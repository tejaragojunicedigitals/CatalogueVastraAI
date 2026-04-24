package com.nice.cataloguevastra.repositories

import android.net.Uri
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.model.AddBackgroundData
import com.nice.cataloguevastra.model.CatalogueCardUiModel
import com.nice.cataloguevastra.model.CatalogueImageUiModel
import com.nice.cataloguevastra.model.CatalogueUiState
import com.nice.cataloguevastra.model.CatalogueThemeUiModel
import com.nice.cataloguevastra.model.DeleteCatalogueResponse
import com.nice.cataloguevastra.model.GenerateCatalogueResult
import com.nice.cataloguevastra.model.GarmentSubcategoryItem
import com.nice.cataloguevastra.model.ImageRatioItem
import com.nice.cataloguevastra.model.ChipUiModel
import com.nice.cataloguevastra.model.ModelProcessTypesData
import com.nice.cataloguevastra.model.ModelLibraryFilterType
import com.nice.cataloguevastra.model.ModelSelectionUiModel
import com.nice.cataloguevastra.model.ModelSheetItemUiModel
import com.nice.cataloguevastra.model.ModelTabType
import com.nice.cataloguevastra.model.ProcessVisualItem
import com.nice.cataloguevastra.model.RailItemUiModel
import com.nice.cataloguevastra.model.RailSectionUiModel
import com.nice.cataloguevastra.model.ThemeListItem
import com.nice.cataloguevastra.model.UploadedModelItem
import java.time.LocalDate
import kotlin.collections.plus

class DummyCatalogueRepository : CatalogueRepository {

    override fun getCatalogueUiState(): CatalogueUiState {
        val themeItems = listOf(
            CatalogueThemeUiModel("1", "women", "Women", "Women's Fashion", ""),
            CatalogueThemeUiModel("2", "men", "Men", "Men's Fashion", ""),
            CatalogueThemeUiModel("3", "girl", "Girls", "Girls Fashion", ""),
            CatalogueThemeUiModel("4", "boy", "Boys", "Boys Fashion", "")
        )
        val libraryModels = listOf(
            ModelSheetItemUiModel("model_riya", "Riya", R.drawable.model_img),
            ModelSheetItemUiModel("model_anika", "Anika", R.drawable.model_img),
            ModelSheetItemUiModel("model_meher", "Meher", R.drawable.model_img),
            ModelSheetItemUiModel("model_tara", "Tara", R.drawable.model_img),
            ModelSheetItemUiModel("model_sana", "Sana", R.drawable.model_img),
            ModelSheetItemUiModel("model_naina", "Naina", R.drawable.model_img),
            ModelSheetItemUiModel("model_isha", "Isha", R.drawable.model_img),
            ModelSheetItemUiModel("model_raina", "Raina", R.drawable.model_img)
        )

        val selectedModelId = libraryModels.first().id
        val southIndianModels = listOf(
            ModelSheetItemUiModel("south_anika", "Anika", R.drawable.model_img),
            ModelSheetItemUiModel("south_meher", "Meher", R.drawable.model_img),
            ModelSheetItemUiModel("south_sana", "Sana", R.drawable.model_img),
            ModelSheetItemUiModel("south_naina", "Naina", R.drawable.model_img)
        )
        val northIndianModels = listOf(
            ModelSheetItemUiModel("north_tara", "Tara", R.drawable.model_img),
            ModelSheetItemUiModel("north_riya", "Riya", R.drawable.model_img),
            ModelSheetItemUiModel("north_isha", "Isha", R.drawable.model_img),
            ModelSheetItemUiModel("north_raina", "Raina", R.drawable.model_img)
        )
        val internationalModels = listOf(
            ModelSheetItemUiModel("intl_ava", "Ava", R.drawable.model_img),
            ModelSheetItemUiModel("intl_mia", "Mia", R.drawable.model_img),
            ModelSheetItemUiModel("intl_lina", "Lina", R.drawable.model_img),
            ModelSheetItemUiModel("intl_zoe", "Zoe", R.drawable.model_img)
        )

        return CatalogueUiState(
            catalogueThemes = themeItems,
            catalogueForOptions = themeItems.map { it.name },
            selectedCatalogueFor = "Women",
            garmentSubcategories = emptyList(),
            categoryOptions = emptyList(),
            selectedCategory = "Select",
            outfitTypeOptions = listOf("Saree", "Kurta Set", "Lehenga", "Gown"),
            selectedOutfitType = "Saree",
            platforms = listOf(
                ChipUiModel("amazon", "Amazon", true),
                ChipUiModel("flipkart", "Flipkart", false),
                ChipUiModel("ebay", "EBay", false),
                ChipUiModel("shopify", "Shopify", false),
                ChipUiModel("myntra", "Myntra", false),
                ChipUiModel("nykaa fashion", "Nykaa Fashion", false),
                ChipUiModel("etsy", "Etsy", false),
                ChipUiModel("custom", "Custom", false)
            ),
            aspectRatios = listOf(
                ChipUiModel("1:1", "1:1", true),
                ChipUiModel("3:4", "3:4", false),
                ChipUiModel("4:3", "4:3", false),
                ChipUiModel("4:5", "4:5", false),
                ChipUiModel("16:9", "16:9", false),
                ChipUiModel("9:16", "9:16", false),
                ChipUiModel("Custom", "Custom", false)
            ),
            resolutionWidth = "2000",
            resolutionHeight = "2000",
            isResolutionEditable = false,
            modelRail = RailSectionUiModel(
                items = listOf(
                    RailItemUiModel.Upload(
                    id = "upload_model",
                    title = "Upload\nModel",
                    subtitle = "Use your own"
                )
                )
            ),
            backgroundRail = RailSectionUiModel(
                items = listOf(
                    RailItemUiModel.Upload(
                        id = "upload_background",
                        title = "Upload\nBackground",
                        subtitle = "Add your set"
                    )
                )
            ),
            poseRail = RailSectionUiModel(
                items = emptyList()
            ),
            businessLogoName = "logo.jpg",
            productCode = "Saree-556",
            modelSelection = ModelSelectionUiModel(
                tabs = ModelTabType.entries.toList(),
                selectedModelId = selectedModelId,
                libraryFilters = listOf(
                    ChipUiModel(
                        ModelLibraryFilterType.ALL.id,
                        ModelLibraryFilterType.ALL.label,
                        true
                    ),
                    ChipUiModel(
                        ModelLibraryFilterType.SOUTH_INDIAN.id,
                        ModelLibraryFilterType.SOUTH_INDIAN.label,
                        false
                    ),
                    ChipUiModel(
                        ModelLibraryFilterType.NORTH_INDIAN.id,
                        ModelLibraryFilterType.NORTH_INDIAN.label,
                        false
                    ),
                    ChipUiModel(
                        ModelLibraryFilterType.INTERNATIONAL.id,
                        ModelLibraryFilterType.INTERNATIONAL.label,
                        false
                    )
                ),
                libraryItemsByFilter = mapOf(
                    ModelLibraryFilterType.ALL to libraryModels,
                    ModelLibraryFilterType.SOUTH_INDIAN to southIndianModels,
                    ModelLibraryFilterType.NORTH_INDIAN to northIndianModels,
                    ModelLibraryFilterType.INTERNATIONAL to internationalModels
                ),
                yourModels = listOf(
                    ModelSheetItemUiModel("your_riya", "Own 1", R.drawable.model_img),
                    ModelSheetItemUiModel("your_tara", "Own 2", R.drawable.model_img),
                    ModelSheetItemUiModel("your_sana", "Own 3", R.drawable.model_img),
                    ModelSheetItemUiModel("your_isha", "Own 4", R.drawable.model_img)
                )
            )
        )
    }

    override suspend fun getCatalogues(page: Int, format: String): Result<List<CatalogueCardUiModel>> {
        val today = LocalDate.now()
        return Result.success(
            listOf(
                CatalogueCardUiModel(
                    id = "5824",
                    title = "Catalogue_5824 - 3 Images",
                    subtitle = "${today} | Amazon | 1:1",
                    previewImageRes = R.drawable.ic_gallery,
                    previewImageUrl = null,
                    thumbnails = listOf(
                        CatalogueImageUiModel(imageRes = R.drawable.ic_gallery),
                        CatalogueImageUiModel(imageRes = R.drawable.model),
                        CatalogueImageUiModel(imageRes = R.drawable.ic_gallery)
                    ),
                    createdDateIso = today.toString(),
                    categoryTag = "Men's",
                    platformTag = "Amazon"
                ),
                CatalogueCardUiModel(
                    id = "5823",
                    title = "Catalogue_5823 - 4 Images",
                    subtitle = "${today.minusDays(1)} | Amazon | 1:1",
                    previewImageRes = R.drawable.model,
                    previewImageUrl = null,
                    thumbnails = listOf(
                        CatalogueImageUiModel(imageRes = R.drawable.model),
                        CatalogueImageUiModel(imageRes = R.drawable.ic_gallery)
                    ),
                    createdDateIso = today.minusDays(1).toString(),
                    categoryTag = "Men's",
                    platformTag = "Amazon"
                )
            )
        )
    }

    override suspend fun getThemeList(): Result<List<ThemeListItem>> {
        return Result.success(
            listOf(
                ThemeListItem(1, "", "Women's Fashion", "women", "Women"),
                ThemeListItem(2, "", "Men's Fashion", "men", "Men"),
                ThemeListItem(3, "", "Girls Fashion", "girl", "Girls"),
                ThemeListItem(4, "", "Boys Fashion", "boy", "Boys")
            )
        )
    }

    override suspend fun getGarmentSubcategories(themeFor: String): Result<List<GarmentSubcategoryItem>> {
        return Result.success(
            listOf(
                GarmentSubcategoryItem("37", themeFor, "Half hands T-shirts", "upper", null),
                GarmentSubcategoryItem("38", themeFor, "Full hands T-shirts", "upper", null),
                GarmentSubcategoryItem("39", themeFor, "Kurtis", "upper", null)
            )
        )
    }

    override suspend fun getModelProcessTypes(
        themeFor: String,
        dressName: String
    ): Result<ModelProcessTypesData> {
        return Result.success(
            ModelProcessTypesData(
                modelTypes = listOf(
                    ProcessVisualItem("22", "", "${themeFor} model 1"),
                    ProcessVisualItem("23", "", "${themeFor} model 2"),
                    ProcessVisualItem("24", "", "${themeFor} model 3")
                ),
                backgrounds = listOf(
                    ProcessVisualItem("23", "", "white"),
                    ProcessVisualItem("24", "", "white room"),
                    ProcessVisualItem("25", "", "abstract")
                ),
                fullCatalogue = listOf(
                    ProcessVisualItem("105", "", "front view"),
                    ProcessVisualItem("106", "", "left view"),
                    ProcessVisualItem("107", "", "back view")
                ),
                subcategories = listOf(
                    GarmentSubcategoryItem(dressName, themeFor, "Half hands T-shirts", "upper", null)
                ),
                imageRatios = listOf(
                    ImageRatioItem(1, "", "1.1"),
                    ImageRatioItem(2, "", "4.5"),
                    ImageRatioItem(3, "", "9.16")
                )
            )
        )
    }

    override suspend fun uploadModel(
        name: String,
        category: String,
        imageUri: Uri
    ): Result<UploadedModelItem> {
        return Result.success(
            UploadedModelItem(
                id = 999,
                image = imageUri.toString(),
                modelImage = imageUri.toString()
            )
        )
    }

    override suspend fun uploadBackground(
        name: String,
        category: String,
        imageUri: Uri
    ): Result<AddBackgroundData> {
        return Result.success(
            AddBackgroundData(
                id = 998,
                image = imageUri.toString(),
                modelimage = imageUri.toString()
            )
        )
    }

    override suspend fun generateCatalogue(
        params: GenerateCatalogueParams,
        onProgress: suspend (com.nice.cataloguevastra.model.GenerateCatalogueProgress) -> Unit
    ): Result<GenerateCatalogueResult> {
        onProgress(
            com.nice.cataloguevastra.model.GenerateCatalogueProgress(
                completed = 1,
                total = 1,
                message = "Generated."
            )
        )
        return Result.success(
            GenerateCatalogueResult(
                title = params.title.ifBlank { "Generated Catalogue" },
                imageUrls = listOf(params.productImageUri.toString()),
                garmentId = 0,
                creditsBalance = null
            )
        )
    }

    override suspend fun deleteCatalogue(catalogueId: String): Result<DeleteCatalogueResponse> {
        return Result.success(
            DeleteCatalogueResponse(
                status = true,
                message = "Catalogues deleted successfully",
                deletedGarmentIds = listOfNotNull(catalogueId.toIntOrNull()),
                removedRows = 1
            )
        )
    }
}
