package com.nice.cataloguevastra.model

import com.google.gson.annotations.SerializedName

data class AssetsResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("models") val models: List<AssetApiItem>?,
    @SerializedName("backgrounds") val backgrounds: List<AssetApiItem>?,
    @SerializedName("poses") val poses: List<AssetApiItem>?,
    @SerializedName("cmodels") val customModels: List<AssetApiItem>?,
    @SerializedName("cbackgrounds") val customBackgrounds: List<AssetApiItem>?,
    @SerializedName("cposes") val customPoses: List<AssetApiItem>?,
    @SerializedName("product_images") val productImages: List<ProductImageApiItem>?,
    @SerializedName("message") val message: String?
)

data class AssetApiItem(
    @SerializedName("id") val id: String?,
    @SerializedName("modelimage") val modelImage: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("thumbnail") val thumbnail: String?,
    @SerializedName("create_at") val createdAt: String?,
    @SerializedName("user_id") val userId: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("prompt") val prompt: String?,
    @SerializedName("dress_name") val dressName: String?
)

data class ProductImageApiItem(
    @SerializedName("id") val id: String?,
    @SerializedName("garment_id") val garmentId: String?,
    @SerializedName("upload_image_path") val uploadImagePath: String?,
    @SerializedName("tryon_result_path") val tryOnResultPath: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("credits_charged") val creditsCharged: String?,
    @SerializedName("output_label") val outputLabel: String?,
    @SerializedName("platform") val platform: String?,
    @SerializedName("aspect_ratio") val aspectRatio: String?,
    @SerializedName("catalogue_for") val catalogueFor: String?
)
