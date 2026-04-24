package com.nice.cataloguevastra.model

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    @SerializedName("status") val status: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("user") val user: AuthUser?,
    @SerializedName("billing") val billing: BillingDetails?,
    @SerializedName("credits_balance") val creditsBalance: Int?,
    @SerializedName("package_details") val packageDetails: LoginPackageDetails?,
    @SerializedName("credit_usage") val creditUsage: CreditUsage?,
    @SerializedName("credit_pending") val creditPending: CreditPending?,
    @SerializedName("credits") val credits: CreditsDetails?
)

data class BillingDetails(
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("company") val company: String?,
    @SerializedName("address_line_1") val addressLine1: String?,
    @SerializedName("address_line_2") val addressLine2: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("postal_code") val postalCode: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("gstin") val gstin: String?
)

data class CreditUsage(
    @SerializedName("credits_available") val creditsAvailable: Int?,
    @SerializedName("credits_used_total") val creditsUsedTotal: Int?,
    @SerializedName("credits_total_granted") val creditsTotalGranted: Int?,
    @SerializedName("completed_renders_count") val completedRendersCount: Int?
)

data class CreditPending(
    @SerializedName("pending_package_requests_count") val pendingPackageRequestsCount: Int?,
    @SerializedName("pending_credits_requested_total") val pendingCreditsRequestedTotal: Int?
)

data class CreditsDetails(
    @SerializedName("balance") val balance: Int?,
    @SerializedName("credit_usage") val creditUsage: CreditUsage?,
    @SerializedName("package_details") val packageDetails: LoginPackageDetails?,
    @SerializedName("pending") val pending: CreditPending?
)

data class ProfileUpdateResponse(
    @SerializedName("status") val status: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("user") val user: AuthUser?
)

data class ProfileUpdateParams(
    val username: String,
    val email: String,
    val phoneNumber: String,
    val businessName: String,
    val userUniqueId: String,
    val userImageUri: String?,
    val businessLogoUri: String?,
    val currentPassword: String? = null,
    val newPassword: String? = null,
    val newPasswordConfirm: String? = null
)
