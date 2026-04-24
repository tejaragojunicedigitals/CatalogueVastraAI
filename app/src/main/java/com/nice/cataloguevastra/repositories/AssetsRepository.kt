package com.nice.cataloguevastra.repositories

import com.nice.cataloguevastra.BuildConfig
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.api.ApiServices
import com.nice.cataloguevastra.model.AssetApiItem
import com.nice.cataloguevastra.model.AssetCardUiModel
import com.nice.cataloguevastra.model.AssetImageUiModel
import com.nice.cataloguevastra.model.AssetTabType
import com.nice.cataloguevastra.model.ProductImageApiItem
import com.nice.cataloguevastra.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AssetsRepository(
    private val sessionManager: SessionManager,
    private val apiServices: ApiServices
) {

    suspend fun getAssets(category: String, dressName: String?): Result<List<AssetCardUiModel>> =
        withContext(Dispatchers.IO) {
            val apiToken = sessionManager.getToken().trim()
            if (apiToken.isBlank()) {
                return@withContext Result.failure(IOException("Login required. API token missing."))
            }

            runCatching {
                val response = apiServices.getAssets(
                    apiToken = apiToken,
                    category = category,
                    dressName = dressName?.takeIf { it.isNotBlank() }
                )
                val body = response.body()
                if (!response.isSuccessful || body == null || !body.status) {
                    throw IOException(body?.message ?: "Unable to load assets.")
                }

                buildList {
                    addAll(body.productImages.orEmpty().mapToProductCards())
                    addAll(body.models.orEmpty().mapToCards(AssetTabType.MODELS))
                    addAll(body.customModels.orEmpty().mapToCards(AssetTabType.MODELS))
                    addAll(body.backgrounds.orEmpty().mapToCards(AssetTabType.BACKGROUNDS))
                    addAll(body.customBackgrounds.orEmpty().mapToCards(AssetTabType.BACKGROUNDS))
                }.distinctBy { it.id }
            }
        }

    private fun List<AssetApiItem>.mapToCards(tabType: AssetTabType): List<AssetCardUiModel> {
        return mapNotNull { item ->
            val id = item.id?.trim().orEmpty()
            val title = item.name?.trim().orEmpty()
            if (id.isBlank() || title.isBlank()) return@mapNotNull null

            val placeholderRes = when (tabType) {
                AssetTabType.PRODUCTS, AssetTabType.MODELS -> R.drawable.model_img
                AssetTabType.BACKGROUNDS -> R.drawable.placeholder_bg_warm
            }
            val previewUrl = item.image.toAbsoluteImageUrl()
                ?: item.thumbnail.toAbsoluteImageUrl()
                ?: item.modelImage.toAbsoluteImageUrl()
            val uploadedDate = item.createdAt.toLocalDateOrToday()
            val subtitle = buildSubtitle(
                category = item.category.orEmpty(),
                uploadedDate = uploadedDate
            )

            AssetCardUiModel(
                id = "${tabType.name.lowercase(Locale.ENGLISH)}_$id",
                title = title,
                subtitle = subtitle,
                previewImageRes = placeholderRes,
                previewImageUrl = previewUrl,
                thumbnails = listOfNotNull(
                    previewUrl?.let { AssetImageUiModel(url = it, imageRes = placeholderRes) }
                ),
                tabType = tabType,
                uploadedDate = uploadedDate
            )
        }
    }

    private fun List<ProductImageApiItem>.mapToProductCards(): List<AssetCardUiModel> {
        return mapNotNull { item ->
            val id = item.id?.trim().orEmpty()
            if (id.isBlank()) return@mapNotNull null

            val uploadedDate = item.createdAt.toLocalDateOrToday()
            val uploadImageUrl = item.uploadImagePath.toAbsoluteImageUrl()
            val resultImageUrl = item.tryOnResultPath.toAbsoluteImageUrl()
            val title = buildProductTitle(
                item = item,
                uploadImageUrl = uploadImageUrl
            )

            AssetCardUiModel(
                id = "${AssetTabType.PRODUCTS.name.lowercase(Locale.ENGLISH)}_$id",
                title = title,
                subtitle = buildProductSubtitle(item, uploadedDate),
                previewImageRes = R.drawable.ic_gallery,
                previewImageUrl = uploadImageUrl ?: resultImageUrl,
                thumbnails = listOfNotNull(
                    uploadImageUrl?.let {
                        AssetImageUiModel(url = it, imageRes = R.drawable.model_img)
                    },
                    resultImageUrl?.takeIf { it != uploadImageUrl }?.let {
                        AssetImageUiModel(url = it, imageRes = R.drawable.model_img)
                    }
                ),
                tabType = AssetTabType.PRODUCTS,
                uploadedDate = uploadedDate
            )
        }
    }

    private fun buildSubtitle(category: String, uploadedDate: LocalDate): String {
        val readableCategory = category.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.ENGLISH) else char.toString()
        }.ifBlank { "Asset" }
        return "$readableCategory | ${uploadedDate.format(DISPLAY_DATE_FORMATTER)}"
    }

    private fun buildProductTitle(
        item: ProductImageApiItem,
        uploadImageUrl: String?
    ): String {
        val fileName = uploadImageUrl
            ?.substringAfterLast('/')
            ?.substringBeforeLast('.')
            ?.replace('_', ' ')
            ?.replace('-', ' ')
            ?.trim()
            .orEmpty()

        return when {
            fileName.isNotBlank() -> fileName.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.ENGLISH) else char.toString()
            }
            !item.garmentId.isNullOrBlank() -> "Product ${item.garmentId}"
            else -> "Product ${item.id.orEmpty()}"
        }
    }

    private fun buildProductSubtitle(item: ProductImageApiItem, uploadedDate: LocalDate): String {
        val parts = listOfNotNull(
            item.catalogueFor?.trim()?.takeIf { it.isNotBlank() }?.toReadableWord(),
            item.platform?.trim()?.takeIf { it.isNotBlank() }?.uppercase(Locale.ENGLISH),
            item.outputLabel?.trim()?.takeIf { it.isNotBlank() },
            item.aspectRatio?.trim()?.takeIf { it.isNotBlank() }
        )

        val primary = if (parts.isNotEmpty()) parts.joinToString(" | ") else "Product"
        return "$primary | ${uploadedDate.format(DISPLAY_DATE_FORMATTER)}"
    }

    private fun String.toReadableWord(): String {
        return replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .lowercase(Locale.ENGLISH)
            .replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.ENGLISH) else char.toString()
            }
    }

    private fun String?.toAbsoluteImageUrl(): String? {
        val value = this?.trim().orEmpty()
        if (value.isBlank()) return null
        return if (value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)) {
            value
        } else {
            "${BuildConfig.BASE_URL.trimEnd('/')}/${value.trimStart('/')}"
        }
    }

    private fun String?.toLocalDateOrToday(): LocalDate {
        val raw = this?.trim().orEmpty()
        if (raw.isBlank()) return LocalDate.now()

        return runCatching {
            LocalDateTime.parse(raw, API_DATE_FORMATTER).toLocalDate()
        }.getOrElse {
            runCatching { LocalDate.parse(raw.take(10)) }.getOrDefault(LocalDate.now())
        }
    }

    private companion object {
        val API_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        val DISPLAY_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    }
}
