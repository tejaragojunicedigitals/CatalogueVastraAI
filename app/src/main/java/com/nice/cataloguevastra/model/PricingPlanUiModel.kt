package com.nice.cataloguevastra.model

data class PricingPlanUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val priceLabel: String,
    val creditsLabel: String,
    val actionLabel: String,
    val badgeLabel: String? = null,
    val isHighlighted: Boolean = false,
    val quickHighlights: List<String>,
    val snapshotItems: List<String>,
    val outputItems: List<String>,
    val featureItems: List<String>,
    val isExpanded: Boolean = false
)

data class CreditHistoryUiModel(
    val dateLabel: String,
    val planLabel: String,
    val packLabel: String,
    val creditsLabel: String
)
