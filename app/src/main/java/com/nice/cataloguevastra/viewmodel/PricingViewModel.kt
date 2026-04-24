package com.nice.cataloguevastra.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.gson.Gson
import com.nice.cataloguevastra.CatalogueVastraApp
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.model.LoginPackageDetails
import com.nice.cataloguevastra.model.PaidPlan
import com.nice.cataloguevastra.model.PerImageCredit
import com.nice.cataloguevastra.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.NumberFormat
import java.util.Locale

class PricingViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val app = application
    private val sessionManager: SessionManager =
        (application as CatalogueVastraApp).appContainer.sessionManager
    private val gson = Gson()

    private val _uiState = MutableStateFlow(buildUiState())
    val uiState: StateFlow<PricingUiState> = _uiState.asStateFlow()

    fun refresh() {
        _uiState.value = buildUiState()
    }

    private fun buildUiState(): PricingUiState {
        val packageDetails = parsePackageDetails()
        val plans = buildPlans(packageDetails?.paidPlans.orEmpty())
        val perImageCredits = buildPerImageCredits(packageDetails?.perImageCredits.orEmpty())

        return PricingUiState(
            currentCredits = sessionManager.getCreditsBalance(),
            plans = plans,
            comparisonSections = buildComparisonSections(plans, perImageCredits)
        )
    }

    private fun parsePackageDetails(): LoginPackageDetails? {
        val packageDetailsJson = sessionManager.getPackageDetailsJson()
        if (packageDetailsJson.isBlank()) return null

        return runCatching {
            gson.fromJson(packageDetailsJson, LoginPackageDetails::class.java)
        }.getOrNull()
    }

    private fun buildPlans(paidPlans: List<PaidPlan>): List<PricingPlanCardUiModel> {
        val apiPlansById = paidPlans.associateBy { normalizePlanCode(it.planCode) }
        return DEFAULT_PLAN_SPECS.map { spec ->
            val apiPlan = apiPlansById[spec.id]
            val credits = apiPlan?.credits ?: spec.defaultCredits
            val priceInr = apiPlan?.priceInr ?: spec.defaultPriceInr

            PricingPlanCardUiModel(
                id = spec.id,
                title = app.getString(spec.titleRes),
                subtitle = app.getString(spec.subtitleRes),
                creditsCount = credits,
                creditsLabel = app.getString(
                    R.string.pricing_card_credits_format,
                    formatNumber(credits)
                ),
                actionLabel = app.getString(
                    R.string.pricing_buy_for_format,
                    formatPriceInr(priceInr)
                ),
                badgeLabel = if (spec.isHighlighted) app.getString(R.string.pricing_best_value) else null,
                isHighlighted = spec.isHighlighted
            )
        }
    }

    private fun buildPerImageCredits(apiCredits: List<PerImageCredit>): List<PerImageCreditUiModel> {
        val apiCreditsByTier = apiCredits.associateBy { it.tier?.trim()?.lowercase(Locale.ENGLISH) }
        return DEFAULT_PER_IMAGE_CREDITS.map { fallback ->
            val apiValue = apiCreditsByTier[fallback.tier]
            PerImageCreditUiModel(
                tier = fallback.tier,
                label = apiValue?.label?.takeIf { !it.isNullOrBlank() } ?: fallback.label,
                credits = apiValue?.credits ?: fallback.credits
            )
        }
    }

    private fun buildComparisonSections(
        plans: List<PricingPlanCardUiModel>,
        perImageCredits: List<PerImageCreditUiModel>
    ): List<PricingComparisonSectionUiModel> {
        val starterPlan = plans.first { it.id == PLAN_STARTER }
        val growthPlan = plans.first { it.id == PLAN_GROWTH }
        val proPlan = plans.first { it.id == PLAN_PRO }

        val outputRows = perImageCredits.map { outputTier ->
            PricingComparisonRowUiModel(
                feature = "${outputTier.label} Images (${outputTier.credits} credits)",
                starterValue = calculateImageCount(starterPlan.creditsCount, outputTier.credits).toString(),
                growthValue = calculateImageCount(growthPlan.creditsCount, outputTier.credits).toString(),
                proValue = calculateImageCount(proPlan.creditsCount, outputTier.credits).toString()
            )
        }

        return listOf(
            PricingComparisonSectionUiModel(
                title = app.getString(R.string.pricing_section_essential),
                rows = listOf(
                    PricingComparisonRowUiModel(
                        feature = app.getString(R.string.pricing_row_brand_safe),
                        starterValue = app.getString(R.string.pricing_value_yes),
                        growthValue = app.getString(R.string.pricing_value_yes),
                        proValue = app.getString(R.string.pricing_value_yes)
                    ),
                    PricingComparisonRowUiModel(
                        feature = app.getString(R.string.pricing_row_no_watermark),
                        starterValue = app.getString(R.string.pricing_value_yes),
                        growthValue = app.getString(R.string.pricing_value_yes),
                        proValue = app.getString(R.string.pricing_value_yes)
                    ),
                    PricingComparisonRowUiModel(
                        feature = app.getString(R.string.pricing_row_ai_photoshoot),
                        starterValue = app.getString(R.string.pricing_value_yes),
                        growthValue = app.getString(R.string.pricing_value_yes),
                        proValue = app.getString(R.string.pricing_value_yes)
                    ),
                    PricingComparisonRowUiModel(
                        feature = app.getString(R.string.pricing_row_model_library),
                        starterValue = app.getString(R.string.pricing_value_yes),
                        growthValue = app.getString(R.string.pricing_value_yes),
                        proValue = app.getString(R.string.pricing_value_yes)
                    ),
                    PricingComparisonRowUiModel(
                        feature = app.getString(R.string.pricing_row_background_library),
                        starterValue = app.getString(R.string.pricing_value_yes),
                        growthValue = app.getString(R.string.pricing_value_yes),
                        proValue = app.getString(R.string.pricing_value_yes)
                    ),
                    PricingComparisonRowUiModel(
                        feature = app.getString(R.string.pricing_row_template_library),
                        starterValue = app.getString(R.string.pricing_value_yes),
                        growthValue = app.getString(R.string.pricing_value_yes),
                        proValue = app.getString(R.string.pricing_value_yes)
                    ),
                    PricingComparisonRowUiModel(
                        feature = app.getString(R.string.pricing_row_premium_models),
                        starterValue = app.getString(R.string.pricing_value_no),
                        growthValue = app.getString(R.string.pricing_value_limited),
                        proValue = app.getString(R.string.pricing_value_full)
                    ),
                    PricingComparisonRowUiModel(
                        feature = app.getString(R.string.pricing_row_premium_backgrounds),
                        starterValue = app.getString(R.string.pricing_value_no),
                        growthValue = app.getString(R.string.pricing_value_limited),
                        proValue = app.getString(R.string.pricing_value_full)
                    )
                )
            ),
            PricingComparisonSectionUiModel(
                title = app.getString(R.string.pricing_section_output),
                rows = outputRows
            ),
            PricingComparisonSectionUiModel(
                title = app.getString(R.string.pricing_section_scaling),
                rows = listOf(
                    PricingComparisonRowUiModel(
                        feature = app.getString(R.string.pricing_row_bulk_upload),
                        starterValue = app.getString(R.string.pricing_value_no),
                        growthValue = app.getString(R.string.pricing_value_yes),
                        proValue = app.getString(R.string.pricing_value_yes)
                    ),
                    PricingComparisonRowUiModel(
                        feature = app.getString(R.string.pricing_row_rendering_priority),
                        starterValue = app.getString(R.string.pricing_value_standard),
                        growthValue = app.getString(R.string.pricing_value_faster),
                        proValue = app.getString(R.string.pricing_value_fastest)
                    )
                )
            ),
            PricingComparisonSectionUiModel(
                title = app.getString(R.string.pricing_section_limits),
                rows = listOf(
                    PricingComparisonRowUiModel(
                        feature = app.getString(R.string.pricing_row_max_upload),
                        starterValue = "10 MB",
                        growthValue = "20 MB",
                        proValue = "50 MB"
                    ),
                    PricingComparisonRowUiModel(
                        feature = app.getString(R.string.pricing_row_turnaround),
                        starterValue = app.getString(R.string.pricing_value_immediate),
                        growthValue = app.getString(R.string.pricing_value_immediate),
                        proValue = app.getString(R.string.pricing_value_immediate_priority)
                    )
                )
            ),
            PricingComparisonSectionUiModel(
                title = app.getString(R.string.pricing_section_support),
                rows = listOf(
                    PricingComparisonRowUiModel(
                        feature = app.getString(R.string.pricing_row_support),
                        starterValue = app.getString(R.string.pricing_value_email),
                        growthValue = app.getString(R.string.pricing_value_email_chat),
                        proValue = app.getString(R.string.pricing_value_email_chat_priority)
                    )
                )
            )
        )
    }

    private fun normalizePlanCode(planCode: String?): String {
        val value = planCode?.trim()?.lowercase(Locale.ENGLISH).orEmpty()
        return when {
            value.contains(PLAN_GROWTH) -> PLAN_GROWTH
            value.contains(PLAN_PRO) -> PLAN_PRO
            else -> PLAN_STARTER
        }
    }

    private fun formatNumber(value: Int): String {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("en-IN")).format(value)
    }

    private fun formatPriceInr(value: Int): String {
        return "\u20B9${formatNumber(value)}"
    }

    private fun calculateImageCount(planCredits: Int, costPerImage: Int): Int {
        if (costPerImage <= 0) return 0
        return planCredits / costPerImage
    }

    private companion object {
        const val PLAN_STARTER = "starter"
        const val PLAN_GROWTH = "growth"
        const val PLAN_PRO = "pro"

        val DEFAULT_PLAN_SPECS = listOf(
            PlanSpec(
                id = PLAN_STARTER,
                titleRes = R.string.pricing_plan_starter,
                subtitleRes = R.string.pricing_plan_starter_subtitle,
                defaultCredits = 2500,
                defaultPriceInr = 2500,
                isHighlighted = false
            ),
            PlanSpec(
                id = PLAN_GROWTH,
                titleRes = R.string.pricing_plan_growth,
                subtitleRes = R.string.pricing_plan_growth_subtitle,
                defaultCredits = 5000,
                defaultPriceInr = 5000,
                isHighlighted = true
            ),
            PlanSpec(
                id = PLAN_PRO,
                titleRes = R.string.pricing_plan_pro,
                subtitleRes = R.string.pricing_plan_pro_subtitle,
                defaultCredits = 10000,
                defaultPriceInr = 10000,
                isHighlighted = false
            )
        )

        val DEFAULT_PER_IMAGE_CREDITS = listOf(
            PerImageCreditUiModel("hd", "HD", 25),
            PerImageCreditUiModel("2k", "2K", 35),
            PerImageCreditUiModel("4k", "4K", 40)
        )
    }
}

data class PricingUiState(
    val currentCredits: Int,
    val plans: List<PricingPlanCardUiModel>,
    val comparisonSections: List<PricingComparisonSectionUiModel>
)

data class PricingPlanCardUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val creditsCount: Int,
    val creditsLabel: String,
    val actionLabel: String,
    val badgeLabel: String? = null,
    val isHighlighted: Boolean = false
)

data class PricingComparisonSectionUiModel(
    val title: String,
    val rows: List<PricingComparisonRowUiModel>
)

data class PricingComparisonRowUiModel(
    val feature: String,
    val starterValue: String,
    val growthValue: String,
    val proValue: String
)

private data class PerImageCreditUiModel(
    val tier: String,
    val label: String,
    val credits: Int
)

private data class PlanSpec(
    val id: String,
    val titleRes: Int,
    val subtitleRes: Int,
    val defaultCredits: Int,
    val defaultPriceInr: Int,
    val isHighlighted: Boolean
)
