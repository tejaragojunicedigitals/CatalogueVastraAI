package com.nice.cataloguevastra.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_name") val deviceName: String
)

data class LoginResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("api_token") val apiToken: String?,
    @SerializedName("token_id") val tokenId: Int?,
    @SerializedName("user") val user: AuthUser?
)

data class SignUpResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("error") val error: String?,
    @SerializedName("data") val data: RegisteredUser?
)

data class AuthUser(
    @SerializedName("id") val id: Int?,
    @SerializedName("username") val username: String?,
    @SerializedName("gmail") val email: String?,
    @SerializedName("phoneno") val phoneNumber: String?,
    @SerializedName("businessname") val businessName: String?,
    @SerializedName("useruniqueid") val userUniqueId: String?,
    @SerializedName("userimage") val userImage: String?,
    @SerializedName("business_logo") val businessLogo: String?
)

data class RegisteredUser(
    @SerializedName("user_id") val userId: Int?,
    @SerializedName("username") val username: String?,
    @SerializedName("useruniqueid") val userUniqueId: String?,
    @SerializedName("userimage") val userImage: String?,
    @SerializedName("business_logo") val businessLogo: String?,
    @SerializedName("credits_balance") val creditsBalance: Int?
)

data class ApiErrorResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?
)

data class SignUpParams(
    val username: String,
    val password: String,
    val email: String,
    val phoneNumber: String,
    val businessName: String,
    val userImageUri: String?,
    val businessLogoUri: String?
)

data class SignUpResult(
    val message: String,
    val username: String
)

fun LoginResponse.toToken(): String {
    return apiToken.orEmpty()
}
