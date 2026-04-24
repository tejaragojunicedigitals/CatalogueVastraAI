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
import com.nice.cataloguevastra.model.LoginResponse
import com.nice.cataloguevastra.model.ProfileResponse
import com.nice.cataloguevastra.model.ProfileUpdateParams
import com.nice.cataloguevastra.model.SignUpParams
import com.nice.cataloguevastra.model.SignUpResult
import com.nice.cataloguevastra.model.toToken
import com.nice.cataloguevastra.utils.createCompressedImageMultipartPart
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
            sessionManager.savePackageDetailsJson(body.packageDetails?.let(gson::toJson))
            sessionManager.saveCreditsBalance(resolveCreditsBalance(body))
        }
    }

    suspend fun logout(): Result<String> = withContext(Dispatchers.IO) {
        val apiToken = sessionManager.getToken().trim()
        if (apiToken.isBlank()) {
            sessionManager.clearSession()
            return@withContext Result.success("Logged out.")
        }

        runCatching {
            val response = apiServices.logout(apiToken)
            val body = response.body()
            if (!response.isSuccessful || body == null || !body.status) {
                throw IOException(parseErrorMessage(response, body?.message))
            }
            sessionManager.clearSession()
            body.message?.takeIf { it.isNotBlank() } ?: "Logged out."
        }
    }

    suspend fun getProfile(): Result<ProfileResponse> = withContext(Dispatchers.IO) {
        val apiToken = sessionManager.getToken().trim()
        if (apiToken.isBlank()) {
            return@withContext Result.failure(IOException("Please sign in again."))
        }

        runCatching {
            val response = apiServices.getProfile(apiToken)
            val body = response.body()

            if (!response.isSuccessful || body == null || body.status == false) {
                throw IOException(parseErrorMessage(response, body?.message))
            }

            sessionManager.saveCreditsBalance(resolveCreditsBalance(body))
            sessionManager.savePackageDetailsJson(resolvePackageDetailsJson(body))
            body
        }
    }

    suspend fun updateProfile(params: ProfileUpdateParams): Result<ProfileResponse> = withContext(Dispatchers.IO) {
        val apiToken = sessionManager.getToken().trim()
        if (apiToken.isBlank()) {
            return@withContext Result.failure(IOException("Please sign in again."))
        }

        val userImagePayload = params.userImageUri?.let {
            context.createCompressedImageMultipartPart("userimage", it.toUri())
        }
        val businessLogoPayload = params.businessLogoUri?.let {
            context.createCompressedImageMultipartPart("business_logo", it.toUri())
        }

        try {
            runCatching {
                if (BuildConfig.DEBUG) {
                    Log.d(
                        logTag,
                        "Updating profile. userImageSelected=${userImagePayload != null}, " +
                            "businessLogoSelected=${businessLogoPayload != null}, " +
                            "userImageUri=${params.userImageUri}, businessLogoUri=${params.businessLogoUri}, " +
                            "userImageFile=${userImagePayload?.fileName}, " +
                            "userImageBytes=${userImagePayload?.byteSize}, " +
                            "userImageSha=${userImagePayload?.sha256}, " +
                            "businessLogoFile=${businessLogoPayload?.fileName}, " +
                            "businessLogoBytes=${businessLogoPayload?.byteSize}, " +
                            "businessLogoSha=${businessLogoPayload?.sha256}"
                    )
                }
                val response = apiServices.updateProfile(
                    apiToken = apiToken,
                    username = params.username.toPlainTextRequestBody(),
                    email = params.email.toPlainTextRequestBody(),
                    phoneNumber = params.phoneNumber.toPlainTextRequestBody(),
                    businessName = params.businessName.toPlainTextRequestBody(),
                    userUniqueId = params.userUniqueId.toPlainTextRequestBody(),
                    currentPassword = params.currentPassword?.toPlainTextRequestBody(),
                    newPassword = params.newPassword?.toPlainTextRequestBody(),
                    newPasswordConfirm = params.newPasswordConfirm?.toPlainTextRequestBody(),
                    userImage = userImagePayload?.part,
                    businessLogo = businessLogoPayload?.part
                )
                val body = response.body()

                if (!response.isSuccessful || body == null || body.status == false) {
                    throw IOException(parseErrorMessage(response, body?.message))
                }

                sessionManager.saveCreditsBalance(resolveCreditsBalance(body))
                sessionManager.savePackageDetailsJson(resolvePackageDetailsJson(body))
                if (BuildConfig.DEBUG) {
                    Log.d(
                        logTag,
                        "Profile update response. userImage=${body.user?.userImage}, " +
                            "businessLogo=${body.user?.businessLogo}"
                    )
                }
                body
            }
        } finally {
            userImagePayload?.deleteTempFile()
            businessLogoPayload?.deleteTempFile()
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

    private fun resolveCreditsBalance(body: LoginResponse): Int {
        val topLevelBalance = body.creditsBalance ?: 0
        if (topLevelBalance > 0) return topLevelBalance

        val packageBalance = body.packageDetails?.creditsBalance ?: 0
        if (packageBalance > 0) return packageBalance

        return body.packageDetails?.welcomeOffer?.credits ?: 0
    }

    private fun resolveCreditsBalance(body: ProfileResponse): Int {
        val topLevelBalance = body.creditsBalance ?: 0
        if (topLevelBalance > 0) return topLevelBalance

        val nestedBalance = body.credits?.balance ?: 0
        if (nestedBalance > 0) return nestedBalance

        val usageBalance = body.creditUsage?.creditsAvailable ?: body.credits?.creditUsage?.creditsAvailable ?: 0
        if (usageBalance > 0) return usageBalance

        val packageBalance = body.packageDetails?.creditsBalance ?: body.credits?.packageDetails?.creditsBalance ?: 0
        if (packageBalance > 0) return packageBalance

        return body.packageDetails?.welcomeOffer?.credits
            ?: body.credits?.packageDetails?.welcomeOffer?.credits
            ?: 0
    }

    private fun resolvePackageDetailsJson(body: ProfileResponse): String? {
        val packageDetails = body.packageDetails ?: body.credits?.packageDetails
        return packageDetails?.let(gson::toJson)
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
