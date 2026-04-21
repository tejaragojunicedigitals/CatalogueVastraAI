package com.nice.cataloguevastra.repositories

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.google.gson.Gson
import com.nice.cataloguevastra.BuildConfig
import com.nice.cataloguevastra.api.ApiServices
import com.nice.cataloguevastra.utils.SessionManager
import com.nice.cataloguevastra.model.ApiErrorResponse
import com.nice.cataloguevastra.model.LoginRequest
import com.nice.cataloguevastra.model.SignUpParams
import com.nice.cataloguevastra.model.SignUpResult
import com.nice.cataloguevastra.model.toToken
import com.nice.cataloguevastra.utils.createMultipartPart
import com.nice.cataloguevastra.utils.toPlainTextRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

class AuthRepository(
    private val apiServices: ApiServices,
    private val sessionManager: SessionManager,
    private val context: Context,
    private val gson: Gson = Gson()
) {

    private val logTag = "AuthRepository"

    suspend fun login(request: LoginRequest): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = apiServices.login(request)
            val body = response.body()

            if (!response.isSuccessful || body == null || !body.status || body.apiToken.isNullOrBlank()) {
                throw IOException(parseErrorMessage(response, body?.message))
            }

            if (BuildConfig.DEBUG) {
                Log.d(logTag, "Login API token: ${body.apiToken}")
            }
            sessionManager.saveToken(body.toToken())
        }
    }

    suspend fun signUp(params: SignUpParams): Result<SignUpResult> = withContext(Dispatchers.IO) {
        val userImagePayload = params.userImageUri?.let {
            context.createMultipartPart("userimage", it.toUri())
        }
        val businessLogoPayload = params.businessLogoUri?.let {
            context.createMultipartPart("business_logo", it.toUri())
        }

        try {
            runCatching {
                val response = apiServices.signUp(
                    username = params.username.toPlainTextRequestBody(),
                    password = params.password.toPlainTextRequestBody(),
                    email = params.email.toPlainTextRequestBody(),
                    phoneNumber = params.phoneNumber.toPlainTextRequestBody(),
                    businessName = params.businessName.toPlainTextRequestBody(),
                    userImage = userImagePayload?.part,
                    businessLogo = businessLogoPayload?.part
                )
                val body = response.body()

                if (!response.isSuccessful || body == null || !body.status) {
                    throw IOException(
                        parseErrorMessage(
                            response = response,
                            fallbackMessage = body?.message,
                            fallbackError = body?.error
                        )
                    )
                }

                SignUpResult(
                    message = body.message,
                    username = body.data?.username?.takeIf { it.isNotBlank() } ?: params.username
                )
            }
        } finally {
            userImagePayload?.deleteTempFile()
            businessLogoPayload?.deleteTempFile()
        }
    }

    private fun parseErrorMessage(
        response: Response<*>,
        fallbackMessage: String?,
        fallbackError: String? = null
    ): String {
        val apiError = response.errorBody()
            ?.charStream()
            ?.use { reader ->
                runCatching {
                    gson.fromJson(reader, ApiErrorResponse::class.java)
                }.getOrNull()
            }

        return apiError?.error
            ?.takeIf { it.isNotBlank() }
            ?: apiError?.message
                ?.takeIf { it.isNotBlank() }
            ?: fallbackError?.takeIf { it.isNotBlank() }
            ?: fallbackMessage?.takeIf { it.isNotBlank() }
            ?: "Something went wrong. Please try again."
    }
}
