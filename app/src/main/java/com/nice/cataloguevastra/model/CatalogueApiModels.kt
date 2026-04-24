package com.nice.cataloguevastra.model

import com.google.gson.annotations.SerializedName

data class ThemeListResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("data") val data: List<ThemeListItem>?,
    @SerializedName("message") val message: String?
)

data class ThemeListItem(
    @SerializedName("id") val id: Int?,
    @SerializedName("theme_image") val themeImage: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("themefor") val themeFor: String?,
    @SerializedName("name") val name: String?
)

data class GarmentSubcategoriesResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("data") val data: List<GarmentSubcategoryItem>?,
    @SerializedName("message") val message: String?
)

data class GarmentSubcategoryItem(
    @SerializedName("id") val id: String?,
    @SerializedName("themefor") val themeFor: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("g_type") val garmentType: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class ModelProcessTypesResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("data") val data: ModelProcessTypesData?,
    @SerializedName("message") val message: String?
)

data class ModelProcessTypesData(
    @SerializedName("model_type") val modelTypes: List<ProcessVisualItem>?,
    @SerializedName("models") val models: List<ProcessVisualItem>? = null,
    @SerializedName("cmodels") val customModels: List<ProcessVisualItem>? = null,
    @SerializedName("backgrounds") val backgrounds: List<ProcessVisualItem>?,
    @SerializedName("cbackgrounds") val customBackgrounds: List<ProcessVisualItem>? = null,
    @SerializedName("full_catalogue") val fullCatalogue: List<ProcessVisualItem>?,
    @SerializedName("poses") val poses: List<ProcessVisualItem>? = null,
    @SerializedName("cposes") val customPoses: List<ProcessVisualItem>? = null,
    @SerializedName("subcategories") val subcategories: List<GarmentSubcategoryItem>?,
    @SerializedName("image_ratios") val imageRatios: List<ImageRatioItem>?
)

data class ProcessVisualItem(
    @SerializedName("id") val id: String?,
    @SerializedName("model_image") val modelImage: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("modelimage") val modelImageLegacy: String? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("dress_name") val dressName: String? = null
)

data class ImageRatioItem(
    @SerializedName("id") val id: Int?,
    @SerializedName("model_image") val modelImage: String?,
    @SerializedName("title") val title: String?
)

data class UploadModelResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: UploadedModelItem?
)

data class UploadedModelItem(
    @SerializedName("id") val id: Int?,
    @SerializedName("image") val image: String?,
    @SerializedName("modelimage") val modelImage: String?
)

data class CataloguesResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("catalogues") val catalogues: List<CatalogueApiItem>?,
    @SerializedName("pagination") val pagination: CataloguePaginationApi?
)

data class CatalogueApiItem(
    @SerializedName("garment_id") val garmentId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("platform") val platform: String?,
    @SerializedName("aspect_ratio") val aspectRatio: String?,
    @SerializedName("catalogue_pose_ids") val cataloguePoseIds: String?,
    @SerializedName("poses") val poses: List<Int>?,
    @SerializedName("model_id") val modelId: Int?,
    @SerializedName("bg_id") val bgId: Int?,
    @SerializedName("width") val width: Int?,
    @SerializedName("height") val height: Int?,
    @SerializedName("generated_images") val generatedImages: List<String>?,
    @SerializedName("image_items") val imageItems: List<CatalogueImageItemApi>?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("garment_image") val garmentImage: String?
)

data class CatalogueImageItemApi(
    @SerializedName("url") val url: String?,
    @SerializedName("pose_id") val poseId: Int?,
    @SerializedName("output_tier") val outputTier: String?,
    @SerializedName("output_label") val outputLabel: String?,
    @SerializedName("credits_charged") val creditsCharged: Int?
)

data class CataloguePaginationApi(
    @SerializedName("current_page") val currentPage: Int?,
    @SerializedName("per_page") val perPage: Int?,
    @SerializedName("total_rows") val totalRows: Int?,
    @SerializedName("total_pages") val totalPages: Int?
)

data class GenerateCatalogueResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("images") val images: List<String>?,
    @SerializedName("job_id") val jobId: String?,
    @SerializedName("garment_id") val garmentId: Int?,
    @SerializedName("expected_total") val expectedTotal: Int?,
    @SerializedName("poll_interval_ms") val pollIntervalMs: Long?,
    @SerializedName("credits_balance") val creditsBalance: Int?,
    @SerializedName("details") val details: GenerateCatalogueDetails?
)

data class GeneratePollResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("job_id") val jobId: String?,
    @SerializedName("garment_id") val garmentId: Int?,
    @SerializedName("completed") val completed: Int?,
    @SerializedName("result_count") val resultCount: Int?,
    @SerializedName("total") val total: Int?,
    @SerializedName("expected_total") val expectedTotal: Int?,
    @SerializedName("images") val images: List<String>?,
    @SerializedName("credits_balance") val creditsBalance: Int?,
    @SerializedName("poll_interval_ms") val pollIntervalMs: Long?
)

data class GenerateCatalogueDetails(
    @SerializedName("poses") val poses: Int?,
    @SerializedName("uploaded_images") val uploadedImages: Int?,
    @SerializedName("credits_per_image") val creditsPerImage: Int?,
    @SerializedName("credits_charged_total") val creditsChargedTotal: Int?,
    @SerializedName("output_tier") val outputTier: String?,
    @SerializedName("output_label") val outputLabel: String?,
    @SerializedName("pose_garment_comfy_names") val poseGarmentComfyNames: Map<String, String>?
)

data class GenerateCatalogueResult(
    val title: String,
    val imageUrls: List<String>,
    val garmentId: Int?,
    val creditsBalance: Int?
)

data class GenerateCatalogueProgress(
    val completed: Int,
    val total: Int,
    val message: String
) {
    val percent: Int = if (total <= 0) {
        0
    } else {
        ((completed.coerceIn(0, total) * 100f) / total).toInt().coerceIn(0, 100)
    }
}
