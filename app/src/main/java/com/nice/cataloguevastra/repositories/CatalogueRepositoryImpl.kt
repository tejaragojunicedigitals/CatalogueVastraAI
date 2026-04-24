package com.nice.cataloguevastra.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.nice.cataloguevastra.BuildConfig
import com.nice.cataloguevastra.api.ApiServices
import com.nice.cataloguevastra.model.AddBackgroundData
import com.nice.cataloguevastra.model.ApiErrorResponse
import com.nice.cataloguevastra.model.CatalogueCardUiModel
import com.nice.cataloguevastra.model.CatalogueImageItemApi
import com.nice.cataloguevastra.model.CatalogueImageUiModel
import com.nice.cataloguevastra.model.DeleteCatalogueResponse
import com.nice.cataloguevastra.model.DeleteCataloguesRequest
import com.nice.cataloguevastra.model.GenerateCatalogueProgress
import com.nice.cataloguevastra.model.GenerateCatalogueResponse
import com.nice.cataloguevastra.model.GenerateCatalogueResult
import com.nice.cataloguevastra.model.GeneratePollResponse
import com.nice.cataloguevastra.model.GarmentSubcategoryItem
import com.nice.cataloguevastra.model.ModelProcessTypesData
import com.nice.cataloguevastra.model.ThemeListItem
import com.nice.cataloguevastra.model.UploadedModelItem
import com.nice.cataloguevastra.utils.SessionManager
import com.nice.cataloguevastra.utils.createMultipartPart
import com.nice.cataloguevastra.utils.toPlainTextRequestBody
import com.nice.cataloguevastra.viewmodel.CatalogueListViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class CatalogueRepositoryImpl(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val apiServices: ApiServices,
    private val fallbackRepository: CatalogueRepository = DummyCatalogueRepository()
) : CatalogueRepository by fallbackRepository {

    private val gson = Gson()

    override suspend fun getCatalogues(
        page: Int,
        format: String
    ): Result<List<CatalogueCardUiModel>> = withContext(Dispatchers.IO) {
        val apiToken = sessionManager.getToken().trim()
        if (apiToken.isBlank()) {
            return@withContext Result.failure(IOException("Login required. API token missing."))
        }

        runCatching {
            val response = apiServices.getCatalogues(
                apiToken = apiToken,
                page = page,
                format = format
            )
            val body = response.body()
            if (!response.isSuccessful || body == null || !body.status) {
                throw IOException("Unable to load catalogues right now.")
            }

            body.catalogues.orEmpty().mapNotNull { item ->
                val garmentId = item.garmentId?.trim().orEmpty()
                if (garmentId.isBlank()) return@mapNotNull null

                val generatedImageUrls = item.imageItems.orEmpty()
                    .mapNotNull { imageItem -> imageItem.toUiImage() }
                    .ifEmpty {
                        item.generatedImages.orEmpty()
                            .map { CatalogueImageUiModel(url = it) }
                    }
                val thumbnailUrls = generatedImageUrls + listOfNotNull(
                    item.garmentImage
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { CatalogueImageUiModel(url = it) }
                )
                val previewImageUrl = generatedImageUrls.firstOrNull()?.url
                    ?: item.generatedImages.orEmpty().firstOrNull()
                    ?: item.garmentImage
                val createdDate = item.createdAt.toCatalogueLocalDate()
                val imageCount = item.imageItems?.size
                    ?: item.generatedImages?.size
                    ?: 0
                val imageLabel = if (imageCount == 1) "1 Image" else "$imageCount Images"
                val displayName = item.name?.trim().orEmpty().ifBlank { "Catalogue_$garmentId" }
                val subtitleParts = buildList {
                    add(createdDate.format(SUBTITLE_DATE_FORMATTER))
                    item.platform?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
                    item.aspectRatio?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
                }

                CatalogueCardUiModel(
                    id = garmentId,
                    title = "$displayName - $imageLabel",
                    subtitle = subtitleParts.joinToString(" | "),
                    previewImageRes = com.nice.cataloguevastra.R.drawable.ic_gallery,
                    previewImageUrl = previewImageUrl,
                    thumbnails = thumbnailUrls,
                    createdDateIso = createdDate.toString(),
                    categoryTag = item.category.toFilterCategoryLabel(),
                    platformTag = item.platform?.trim().orEmpty()
                )
            }
        }
    }

    override suspend fun getThemeList(): Result<List<ThemeListItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiServices.getThemeList()
            val body = response.body()
            if (!response.isSuccessful || body == null || !body.status) {
                throw IOException(body?.message ?: "Unable to load catalogue themes.")
            }
            body.data.orEmpty()
        }
    }

    override suspend fun getGarmentSubcategories(themeFor: String): Result<List<GarmentSubcategoryItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = apiServices.getGarmentSubcategories(themeFor)
                val body = response.body()
                if (!response.isSuccessful || body == null || !body.status) {
                    throw IOException(body?.message ?: "Unable to load garment subcategories.")
                }
                body.data.orEmpty()
            }
        }

    override suspend fun getModelProcessTypes(
        themeFor: String,
        dressName: String
    ): Result<ModelProcessTypesData> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiServices.getModelProcessTypes(themeFor, dressName)
            val body = response.body()
            if (!response.isSuccessful || body == null || !body.status || body.data == null) {
                throw IOException(body?.message ?: "Unable to load model process types.")
            }
            val data = body.data
            if (!data.hasGenerateAssets()) {
                throw IOException("No generate assets found for $themeFor dress $dressName.")
            }
            data
        }
    }

    private fun ModelProcessTypesData.hasGenerateAssets(): Boolean {
        val hasModels = !modelTypes.isNullOrEmpty() || !models.isNullOrEmpty() || !customModels.isNullOrEmpty()
        val hasBackgrounds = !backgrounds.isNullOrEmpty() || !customBackgrounds.isNullOrEmpty()
        val hasPoses = !fullCatalogue.isNullOrEmpty() || !poses.isNullOrEmpty() || !customPoses.isNullOrEmpty()
        return hasModels && hasBackgrounds && hasPoses
    }

    override suspend fun uploadModel(
        name: String,
        category: String,
        imageUri: Uri
    ): Result<UploadedModelItem> = withContext(Dispatchers.IO) {
        val apiToken = sessionManager.getToken().trim()
        if (apiToken.isBlank()) {
            return@withContext Result.failure(IOException("Login required. API token missing."))
        }

        val uploadPayload = context.createMultipartPart("modelimage", imageUri)
            ?: return@withContext Result.failure(IOException("Unable to read selected model image."))

        try {
            runCatching {
                val response = apiServices.uploadModel(
                    apiToken = apiToken,
                    name = name.toPlainTextRequestBody(),
                    category = category.toPlainTextRequestBody(),
                    modelImage = uploadPayload.part
                )
                val body = response.body()
                if (!response.isSuccessful || body == null || !body.status || body.data == null) {
                    throw IOException(body?.message ?: "Unable to upload model.")
                }
                body.data
            }
        } finally {
            uploadPayload.deleteTempFile()
        }
    }

    override suspend fun uploadBackground(
        name: String,
        category: String,
        imageUri: Uri
    ): Result<AddBackgroundData> = withContext(Dispatchers.IO) {
        val apiToken = sessionManager.getToken().trim()
        if (apiToken.isBlank()) {
            return@withContext Result.failure(IOException("Login required. API token missing."))
        }
        val uploadPayload = context.createMultipartPart("modelimage", imageUri)
            ?: return@withContext Result.failure(IOException("Unable to read selected background image."))

        try {
            runCatching {
                val response = apiServices.uploadBackground(
                    apiToken = apiToken,
                    name = name.toPlainTextRequestBody(),
                    category = category.toPlainTextRequestBody(),
                    modelImage = uploadPayload.part
                )
                val body = response.body()
                if (!response.isSuccessful || body == null || !body.status) {
                    throw IOException(body?.message ?: "Unable to upload background.")
                }
                body.data
            }
        } finally {
            uploadPayload.deleteTempFile()
        }
    }

    override suspend fun generateCatalogue(
        params: GenerateCatalogueParams,
        onProgress: suspend (GenerateCatalogueProgress) -> Unit
    ): Result<GenerateCatalogueResult> = withContext(Dispatchers.IO) {
        val apiToken = sessionManager.getToken().trim()
        if (apiToken.isBlank()) {
            return@withContext Result.failure(IOException("Login required. API token missing."))
        }

        val productPayload = context.createMultipartPart("product_images[]", params.productImageUri)
            ?: return@withContext Result.failure(IOException("Unable to read selected product image."))
        val posePayloads = params.poseGarmentImages.mapNotNull { (poseId, uri) ->
            context.createMultipartPart("pose_garment_$poseId", uri)
        }
        val sameGarmentPoseIdsBody = params.sameGarmentPoseIds
            .takeIf { it.isNotEmpty() && it.size < params.poseIds.size }
            ?.toJsonArrayString()
            ?.toPlainTextRequestBody()

        try {
            runCatching {
                if (BuildConfig.DEBUG) {
                    Log.d(
                        LOG_TAG,
                        """
                        GENERATE API REQUEST
                        endpoint=POST /api/webtool/generate
                        catalogue_for=${params.catalogueFor}
                        dress_type=${params.dressType}
                        model_id=${params.modelId}
                        bg_id=${params.backgroundId}
                        platform=${params.platform}
                        aspect_ratio=${params.aspectRatio}
                        width=${params.width}
                        height=${params.height}
                        pose_id=${params.poseIds.toJsonArrayString()}
                        product_images[]=file(name=${productPayload.fileName}, bytes=${productPayload.byteSize}, sha=${productPayload.sha256})
                        pose_garment_same_as_product=${params.sameGarmentPoseIds.toJsonArrayString()}
                        pose_garment_same_as_product_sent=${sameGarmentPoseIdsBody != null}
                        pose_garment_parts=${posePayloads.map { "file(name=${it.fileName}, bytes=${it.byteSize}, sha=${it.sha256})" }}
                        """.trimIndent()
                    )
                }
                val response = apiServices.generateCatalogue(
                    apiToken = apiToken,
                    catalogueFor = params.catalogueFor.toPlainTextRequestBody(),
                    dressType = params.dressType.toPlainTextRequestBody(),
                    modelId = params.modelId.toPlainTextRequestBody(),
                    backgroundId = params.backgroundId.toPlainTextRequestBody(),
                    platform = params.platform.toPlainTextRequestBody(),
                    aspectRatio = params.aspectRatio.toPlainTextRequestBody(),
                    width = params.width.toPlainTextRequestBody(),
                    height = params.height.toPlainTextRequestBody(),
                    poseIds = params.poseIds.toJsonArrayString().toPlainTextRequestBody(),
                    sameGarmentPoseIds = sameGarmentPoseIdsBody,
                    productImages = listOf(productPayload.part),
                    poseGarmentImages = posePayloads.map { it.part }
                )
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    throw IOException(
                        parseErrorMessage(
                            response = response,
                            fallbackMessage = body?.message,
                            defaultMessage = "Unable to generate catalogue."
                        )
                    )
                }

                val finalBody = when (body.status?.lowercase(Locale.ENGLISH)) {
                    "success", "complete" -> body.toPollResponse()
                    "queued", "processing" -> pollGenerateJob(
                        apiToken = apiToken,
                        jobId = body.jobId?.takeIf { it.isNotBlank() }
                            ?: throw IOException("Generate job id missing."),
                        garmentId = body.garmentId,
                        expectedTotal = body.expectedTotal ?: params.poseIds.size,
                        pollIntervalMs = body.pollIntervalMs ?: DEFAULT_GENERATE_POLL_INTERVAL_MS,
                        onProgress = onProgress
                    )
                    else -> throw IOException(body.message ?: "Unable to generate catalogue.")
                }

                val imageUrls = finalBody.images.orEmpty().filter { it.isNotBlank() }
                if (imageUrls.isEmpty()) {
                    throw IOException("Catalogue generated without image output.")
                }

                finalBody.creditsBalance?.let(sessionManager::saveCreditsBalance)
                CatalogueListViewModel.clearCache()
                onProgress(
                    GenerateCatalogueProgress(
                        completed = imageUrls.size,
                        total = (finalBody.expectedTotal ?: finalBody.total ?: imageUrls.size).coerceAtLeast(imageUrls.size),
                        message = finalBody.message ?: "All images are ready."
                    )
                )
                GenerateCatalogueResult(
                    title = params.title.ifBlank {
                        "Catalogue_${finalBody.garmentId ?: System.currentTimeMillis()}"
                    },
                    imageUrls = imageUrls,
                    garmentId = finalBody.garmentId,
                    creditsBalance = finalBody.creditsBalance
                )
            }
        } finally {
            productPayload.deleteTempFile()
            posePayloads.forEach { it.deleteTempFile() }
        }
    }

    private suspend fun pollGenerateJob(
        apiToken: String,
        jobId: String,
        garmentId: Int?,
        expectedTotal: Int,
        pollIntervalMs: Long,
        onProgress: suspend (GenerateCatalogueProgress) -> Unit
    ): GeneratePollResponse {
        onProgress(
            GenerateCatalogueProgress(
                completed = 0,
                total = expectedTotal.coerceAtLeast(1),
                message = "Generation queued."
            )
        )

        var nextDelay = pollIntervalMs.coerceIn(MIN_GENERATE_POLL_INTERVAL_MS, MAX_GENERATE_POLL_INTERVAL_MS)
        repeat(MAX_GENERATE_POLL_ATTEMPTS) {
            delay(nextDelay)
            val response = if (garmentId != null) {
                apiServices.watchGenerateCatalogue(
                    apiToken = apiToken,
                    garmentId = garmentId,
                    jobId = jobId,
                    expectedTotal = expectedTotal
                )
            } else {
                apiServices.pollGenerateCatalogue(apiToken = apiToken, jobId = jobId)
            }
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                throw IOException(
                    parseErrorMessage(
                        response = response,
                        fallbackMessage = body?.message,
                        defaultMessage = "Unable to check generation progress."
                    )
                )
            }

            val total = body.expectedTotal ?: body.total ?: expectedTotal
            val completed = body.resultCount ?: body.completed ?: body.images.orEmpty().size
            onProgress(
                GenerateCatalogueProgress(
                    completed = completed,
                    total = total.coerceAtLeast(1),
                    message = body.message ?: "Still rendering..."
                )
            )

            if (body.status.equals("complete", ignoreCase = true) ||
                body.status.equals("success", ignoreCase = true)
            ) {
                return body
            }

            if (body.status.equals("error", ignoreCase = true) ||
                body.status.equals("failed", ignoreCase = true)
            ) {
                throw IOException(body.message ?: "Generation failed.")
            }

            nextDelay = (body.pollIntervalMs ?: nextDelay)
                .coerceIn(MIN_GENERATE_POLL_INTERVAL_MS, MAX_GENERATE_POLL_INTERVAL_MS)
        }

        throw IOException("Generation is taking longer than expected. Please check Catalogues later.")
    }

    private fun GenerateCatalogueResponse.toPollResponse(): GeneratePollResponse {
        return GeneratePollResponse(
            status = status,
            message = message,
            jobId = jobId,
            garmentId = garmentId,
            completed = images.orEmpty().size,
            resultCount = images.orEmpty().size,
            total = expectedTotal ?: images.orEmpty().size,
            expectedTotal = expectedTotal,
            images = images,
            creditsBalance = creditsBalance,
            pollIntervalMs = pollIntervalMs
        )
    }

    private fun parseErrorMessage(
        response: Response<*>,
        fallbackMessage: String?,
        defaultMessage: String
    ): String {
        val apiError = response.errorBody()
            ?.charStream()
            ?.use { reader ->
                runCatching {
                    gson.fromJson(reader, ApiErrorResponse::class.java)
                }.getOrNull()
            }

        return apiError?.message
            ?.takeIf { it.isNotBlank() }
            ?: apiError?.error?.takeIf { it.isNotBlank() }
            ?: fallbackMessage?.takeIf { it.isNotBlank() }
            ?: defaultMessage
    }

    override suspend fun deleteCatalogue(catalogueId: String): Result<DeleteCatalogueResponse> =
        withContext(Dispatchers.IO) {
            val apiToken = sessionManager.getToken().trim()
            if (apiToken.isBlank()) {
                return@withContext Result.failure(IOException("Login required. API token missing."))
            }

            val garmentId = catalogueId.toIntOrNull()
                ?: return@withContext Result.failure(IOException("Invalid catalogue id."))

            runCatching {
                val response = apiServices.deleteCatalogue(
                    apiToken = apiToken,
                    request = DeleteCataloguesRequest(garmentIds = listOf(garmentId))
                )
                val body = response.body()
                if (!response.isSuccessful || body == null || !body.status) {
                    throw IOException(body?.message ?: "Unable to delete catalogue.")
                }
                body
            }
        }

    private fun CatalogueImageItemApi.toUiImage(): CatalogueImageUiModel? {
        val value = url?.trim().orEmpty()
        if (value.isBlank()) return null
        return CatalogueImageUiModel(url = value)
    }

    private fun String?.toCatalogueLocalDate(): LocalDate {
        val rawValue = this?.trim().orEmpty()
        if (rawValue.isBlank()) return LocalDate.now()
        return runCatching {
            LocalDateTime.parse(rawValue, API_DATE_FORMATTER).toLocalDate()
        }.getOrElse {
            runCatching { LocalDate.parse(rawValue.take(10)) }.getOrDefault(LocalDate.now())
        }
    }

    private fun String?.toFilterCategoryLabel(): String {
        return when (this?.trim()?.lowercase(Locale.ENGLISH)) {
            "women", "woman", "women's", "womens" -> "Women's"
            "men", "man", "men's", "mens" -> "Men's"
            "boy", "boys", "boy's", "boys'" -> "Boy's"
            "girl", "girls", "girl's", "girls'" -> "Girl's"
            else -> this?.trim().orEmpty()
        }
    }

    private fun List<String>.toJsonArrayString(): String {
        return joinToString(prefix = "[", postfix = "]") { value ->
            value.toIntOrNull()?.toString() ?: "\"${value.replace("\"", "\\\"")}\""
        }
    }

    private companion object {
        const val LOG_TAG = "CatalogueRepository"
        const val MAX_GENERATE_POLL_ATTEMPTS = 120
        const val DEFAULT_GENERATE_POLL_INTERVAL_MS = 2500L
        const val MIN_GENERATE_POLL_INTERVAL_MS = 1000L
        const val MAX_GENERATE_POLL_INTERVAL_MS = 5000L
        val API_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        val SUBTITLE_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    }
}
