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
    @SerializedName("credits_balance") val creditsBalance: Int?,
    @SerializedName("package_details") val packageDetails: LoginPackageDetails?,
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
    @SerializedName("business_logo") val businessLogo: String?,
    @SerializedName("credits_balance") val creditsBalance: Int?
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

data class LogoutResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String?
)

data class LoginPackageDetails(
    @SerializedName("credits_balance") val creditsBalance: Int?,
    @SerializedName("welcome_offer") val welcomeOffer: WelcomeOffer?,
    @SerializedName("paid_plans") val paidPlans: List<PaidPlan>?,
    @SerializedName("credit_grants") val creditGrants: List<CreditGrant>?,
    @SerializedName("package_requests") val packageRequests: List<PackageRequest>?,
    @SerializedName("per_image_credits") val perImageCredits: List<PerImageCredit>?
)

data class WelcomeOffer(
    @SerializedName("plan_code") val planCode: String?,
    @SerializedName("credits") val credits: Int?,
    @SerializedName("price_inr") val priceInr: Int?,
    @SerializedName("grant_type") val grantType: String?,
    @SerializedName("one_time") val oneTime: Boolean?,
    @SerializedName("claimed") val claimed: Boolean?
)

data class PaidPlan(
    @SerializedName("plan_code") val planCode: String?,
    @SerializedName("credits") val credits: Int?,
    @SerializedName("price_inr") val priceInr: Int?,
    @SerializedName("grant_type") val grantType: String?,
    @SerializedName("note") val note: String?
)

data class CreditGrant(
    @SerializedName("id") val id: Int?,
    @SerializedName("plan_code") val planCode: String?,
    @SerializedName("grant_type") val grantType: String?,
    @SerializedName("credit_pack") val creditPack: Int?,
    @SerializedName("credits_granted") val creditsGranted: Int?,
    @SerializedName("created_at") val createdAt: String?
)

data class PackageRequest(
    @SerializedName("id") val id: Int?,
    @SerializedName("plan_code") val planCode: String?,
    @SerializedName("credit_pack") val creditPack: Int?,
    @SerializedName("status") val status: String?,
    @SerializedName("admin_note") val adminNote: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("reviewed_at") val reviewedAt: String?,
    @SerializedName("request_type") val requestType: String?
)

data class PerImageCredit(
    @SerializedName("tier") val tier: String?,
    @SerializedName("label") val label: String?,
    @SerializedName("credits") val credits: Int?,
    @SerializedName("longest_edge_px") val longestEdgePx: String?
)

fun LoginResponse.toToken(): String {
    return apiToken.orEmpty()
}
