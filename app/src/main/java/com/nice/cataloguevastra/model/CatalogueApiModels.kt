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
    @SerializedName("backgrounds") val backgrounds: List<ProcessVisualItem>?,
    @SerializedName("full_catalogue") val fullCatalogue: List<ProcessVisualItem>?,
    @SerializedName("subcategories") val subcategories: List<GarmentSubcategoryItem>?,
    @SerializedName("image_ratios") val imageRatios: List<ImageRatioItem>?
)

data class ProcessVisualItem(
    @SerializedName("id") val id: String?,
    @SerializedName("model_image") val modelImage: String?,
    @SerializedName("title") val title: String?
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
