package com.nice.cataloguevastra.data

import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.ui.catalogues.model.CatalogueUiState
import com.nice.cataloguevastra.ui.catalogues.model.ChipUiModel
import com.nice.cataloguevastra.ui.catalogues.model.ModelSelectionUiModel
import com.nice.cataloguevastra.ui.catalogues.model.ModelSheetItemUiModel
import com.nice.cataloguevastra.ui.catalogues.model.ModelLibraryFilterType
import com.nice.cataloguevastra.ui.catalogues.model.ModelTabType
import com.nice.cataloguevastra.ui.catalogues.model.RailItemUiModel
import com.nice.cataloguevastra.ui.catalogues.model.RailSectionUiModel

class DummyCatalogueRepository : CatalogueRepository {

    override fun getCatalogueUiState(): CatalogueUiState {
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
            catalogueForOptions = listOf("Women", "Men", "Kids", "Unisex"),
            selectedCatalogueFor = "Women",
            categoryOptions = listOf("Clothing", "Jewellery", "Footwear", "Accessories"),
            selectedCategory = "Clothing",
            outfitTypeOptions = listOf("Saree", "Kurta Set", "Lehenga", "Gown"),
            selectedOutfitType = "Saree",
            platforms = listOf(
                ChipUiModel("amazon", "Amazon", true),
                ChipUiModel("flipkart", "Flipkart", false),
                ChipUiModel("myntra", "Myntra", false),
                ChipUiModel("shopify", "Shopify", false),
                ChipUiModel("meesho", "Meesho", false)
            ),
            aspectRatios = listOf(
                ChipUiModel("1_1", "1:1", true),
                ChipUiModel("3_4", "3:4", false),
                ChipUiModel("4_3", "4:3", false),
                ChipUiModel("4_5", "4:5", false),
                ChipUiModel("16_9", "16:9", false),
                ChipUiModel("9_16", "9:16", false)
            ),
            modelRail = RailSectionUiModel(
                items = libraryModels.take(4).mapIndexed { index, item ->
                    RailItemUiModel.Visual(
                        id = item.id,
                        imageRes = item.imageRes,
                        label = item.label,
                        isSelected = index == 0
                    )
                } + RailItemUiModel.Upload(
                    id = "upload_model",
                    title = "Upload\nModel",
                    subtitle = "Use your own"
                )
            ),
            backgroundRail = RailSectionUiModel(
                items = listOf(
                    RailItemUiModel.Visual(
                        id = "bg_warm",
                        imageRes = R.drawable.placeholder_bg_warm,
                        label = "Studio Warm",
                        isSelected = true
                    ),
                    RailItemUiModel.Visual(
                        id = "bg_arch",
                        imageRes = R.drawable.placeholder_bg_arch,
                        label = "Arch Loft",
                        isSelected = false
                    ),
                    RailItemUiModel.Visual(
                        id = "bg_window",
                        imageRes = R.drawable.placeholder_bg_window,
                        label = "Window Light",
                        isSelected = false
                    ),
                    RailItemUiModel.Upload(
                        id = "upload_background",
                        title = "Upload\nBackground",
                        subtitle = "Add your set"
                    )
                )
            ),
            poseRail = RailSectionUiModel(
                items = listOf(
                    RailItemUiModel.Visual(
                        id = "pose_one",
                        imageRes = R.drawable.model_img,
                        label = "Front",
                        isSelected = true
                    ),
                    RailItemUiModel.Visual(
                        id = "pose_two",
                        imageRes = R.drawable.model_img,
                        label = "Three Fourth",
                        isSelected = false
                    ),
                    RailItemUiModel.Visual(
                        id = "pose_three",
                        imageRes = R.drawable.model_img,
                        label = "Walk",
                        isSelected = false
                    ),
                    RailItemUiModel.Visual(
                        id = "pose_four",
                        imageRes = R.drawable.model_img,
                        label = "Drape",
                        isSelected = false
                    )
                )
            ),
            businessLogoName = "logo.jpg",
            productCode = "Saree-556",
            modelSelection = ModelSelectionUiModel(
                tabs = ModelTabType.entries.toList(),
                selectedModelId = selectedModelId,
                libraryFilters = listOf(
                    ChipUiModel(ModelLibraryFilterType.ALL.id, ModelLibraryFilterType.ALL.label, true),
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
}
