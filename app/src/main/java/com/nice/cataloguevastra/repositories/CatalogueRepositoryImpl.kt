package com.nice.cataloguevastra.repositories

import android.content.Context
import android.net.Uri
import com.nice.cataloguevastra.api.ApiServices
import com.nice.cataloguevastra.model.AddBackgroundData
import com.nice.cataloguevastra.model.GarmentSubcategoryItem
import com.nice.cataloguevastra.model.ModelProcessTypesData
import com.nice.cataloguevastra.model.ThemeListItem
import com.nice.cataloguevastra.model.UploadedModelItem
import com.nice.cataloguevastra.utils.SessionManager
import com.nice.cataloguevastra.utils.createMultipartPart
import com.nice.cataloguevastra.utils.toPlainTextRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class CatalogueRepositoryImpl(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val apiServices: ApiServices,
    private val fallbackRepository: CatalogueRepository = DummyCatalogueRepository()
) : CatalogueRepository by fallbackRepository {

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
            body.data
        }
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
}
