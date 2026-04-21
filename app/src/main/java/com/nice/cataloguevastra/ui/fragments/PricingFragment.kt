package com.nice.cataloguevastra.ui.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.FragmentPricingBinding
import com.nice.cataloguevastra.databinding.ItemCreditHistoryBinding
import com.nice.cataloguevastra.adapters.PricingPlansAdapter
import com.nice.cataloguevastra.model.CreditHistoryUiModel
import com.nice.cataloguevastra.model.PricingPlanUiModel

class PricingFragment : Fragment() {

    private var _binding: FragmentPricingBinding? = null
    private val binding get() = _binding!!

    private lateinit var plansAdapter: PricingPlansAdapter

    private val countries = listOf("India", "United States", "UAE")
    private var selectedCountry = "India"
    private val expandedPlanIds = linkedSetOf<String>()
    private var basePlans: List<PricingPlanUiModel> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPricingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCountrySelector()
        setupPlansRecyclerView()
        bindSummary()
        bindPlans()
        bindHistory()
    }

    private fun setupCountrySelector() = with(binding.countryButton) {
        text = selectedCountry
        setOnClickListener { anchor ->
            PopupMenu(requireContext(), anchor, Gravity.END).apply {
                countries.forEachIndexed { index, country ->
                    menu.add(0, index, index, country)
                }
                setOnMenuItemClickListener { menuItem ->
                    selectedCountry = countries[menuItem.itemId]
                    text = selectedCountry
                    showMessage(getString(R.string.pricing_country_message, selectedCountry))
                    true
                }
            }.show()
        }
    }

    private fun setupPlansRecyclerView() = with(binding.pricingPlansRecyclerView) {
        plansAdapter = PricingPlansAdapter(
            onActionClick = { plan ->
                showMessage(getString(R.string.pricing_plan_action_message, plan.title))
            },
            onToggleDetailsClick = { plan ->
                togglePlanDetails(plan.id)
            }
        )
        layoutManager = StaggeredGridLayoutManager(
            calculateSpanCount(),
            RecyclerView.VERTICAL
        ).apply {
            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        }
        adapter = plansAdapter
        setHasFixedSize(false)
        itemAnimator = null
        isNestedScrollingEnabled = false
    }

    private fun bindSummary() = with(binding) {
        creditsValue.text = getString(R.string.pricing_credits_value, 2518)
        creditsUsage.text = getString(R.string.pricing_credit_note)
        creditsProgress.max = 3000
        creditsProgress.progress = 2518
    }

    private fun bindPlans() {
        if (basePlans.isEmpty()) {
            basePlans = listOf(
                PricingPlanUiModel(
                    id = "free",
                    title = getString(R.string.pricing_plan_free),
                    subtitle = getString(R.string.pricing_plan_free_subtitle),
                    priceLabel = getString(R.string.pricing_price_free),
                    creditsLabel = getString(R.string.pricing_credits_line_free),
                    actionLabel = getString(R.string.pricing_current_plan),
                    quickHighlights = listOf(
                        getString(R.string.pricing_highlight_free_credits),
                        getString(R.string.pricing_highlight_free_quality),
                        getString(R.string.pricing_feature_turnaround)
                    ),
                    snapshotItems = listOf(
                        getString(R.string.pricing_snapshot_free_credits),
                        getString(R.string.pricing_snapshot_paid_unlock)
                    ),
                    outputItems = listOf(
                        getString(R.string.pricing_output_free_hd),
                        getString(R.string.pricing_output_free_2k),
                        getString(R.string.pricing_output_free_4k)
                    ),
                    featureItems = listOf(
                        getString(R.string.pricing_feature_upload_10),
                        getString(R.string.pricing_feature_turnaround)
                    )
                ),
                PricingPlanUiModel(
                    id = "starter",
                    title = getString(R.string.pricing_plan_starter),
                    subtitle = getString(R.string.pricing_plan_starter_subtitle),
                    priceLabel = getString(R.string.pricing_price_starter),
                    creditsLabel = getString(R.string.pricing_credits_line_starter),
                    actionLabel = getString(R.string.pricing_upgrade_plan),
                    quickHighlights = listOf(
                        getString(R.string.pricing_snapshot_credits_2500),
                        getString(R.string.pricing_highlight_hd_100),
                        getString(R.string.pricing_feature_no_watermark)
                    ),
                    snapshotItems = listOf(
                        getString(R.string.pricing_snapshot_credits_2500),
                        getString(R.string.pricing_snapshot_price_2500)
                    ),
                    outputItems = listOf(
                        getString(R.string.pricing_output_hd_100),
                        getString(R.string.pricing_output_2k_71),
                        getString(R.string.pricing_output_4k_62)
                    ),
                    featureItems = listOf(
                        getString(R.string.pricing_feature_brand_safe),
                        getString(R.string.pricing_feature_no_watermark),
                        getString(R.string.pricing_feature_ai_outputs),
                        getString(R.string.pricing_feature_model_background),
                        getString(R.string.pricing_feature_bulk_upload),
                        getString(R.string.pricing_feature_support_email),
                        getString(R.string.pricing_feature_upload_10),
                        getString(R.string.pricing_feature_turnaround)
                    )
                ),
                PricingPlanUiModel(
                    id = "growth",
                    title = getString(R.string.pricing_plan_growth),
                    subtitle = getString(R.string.pricing_plan_growth_subtitle),
                    priceLabel = getString(R.string.pricing_price_growth),
                    creditsLabel = getString(R.string.pricing_credits_line_growth),
                    actionLabel = getString(R.string.pricing_upgrade_plan),
                    badgeLabel = getString(R.string.pricing_best_value),
                    isHighlighted = true,
                    quickHighlights = listOf(
                        getString(R.string.pricing_snapshot_credits_5000),
                        getString(R.string.pricing_highlight_hd_200),
                        getString(R.string.pricing_feature_support_chat)
                    ),
                    snapshotItems = listOf(
                        getString(R.string.pricing_snapshot_credits_5000),
                        getString(R.string.pricing_snapshot_price_5000)
                    ),
                    outputItems = listOf(
                        getString(R.string.pricing_output_hd_200),
                        getString(R.string.pricing_output_2k_142),
                        getString(R.string.pricing_output_4k_125)
                    ),
                    featureItems = listOf(
                        getString(R.string.pricing_feature_brand_safe),
                        getString(R.string.pricing_feature_no_watermark),
                        getString(R.string.pricing_feature_ai_outputs),
                        getString(R.string.pricing_feature_model_background),
                        getString(R.string.pricing_feature_premium_models),
                        getString(R.string.pricing_feature_premium_backgrounds_limited),
                        getString(R.string.pricing_feature_rendering_faster),
                        getString(R.string.pricing_feature_bulk_upload),
                        getString(R.string.pricing_feature_support_chat),
                        getString(R.string.pricing_feature_upload_20),
                        getString(R.string.pricing_feature_turnaround)
                    )
                ),
                PricingPlanUiModel(
                    id = "pro",
                    title = getString(R.string.pricing_plan_pro),
                    subtitle = getString(R.string.pricing_plan_pro_subtitle),
                    priceLabel = getString(R.string.pricing_price_pro),
                    creditsLabel = getString(R.string.pricing_credits_line_pro),
                    actionLabel = getString(R.string.pricing_upgrade_plan),
                    quickHighlights = listOf(
                        getString(R.string.pricing_snapshot_credits_10000),
                        getString(R.string.pricing_highlight_hd_400),
                        getString(R.string.pricing_feature_support_priority)
                    ),
                    snapshotItems = listOf(
                        getString(R.string.pricing_snapshot_credits_10000),
                        getString(R.string.pricing_snapshot_price_10000)
                    ),
                    outputItems = listOf(
                        getString(R.string.pricing_output_hd_400),
                        getString(R.string.pricing_output_2k_285),
                        getString(R.string.pricing_output_4k_250)
                    ),
                    featureItems = listOf(
                        getString(R.string.pricing_feature_brand_safe),
                        getString(R.string.pricing_feature_no_watermark),
                        getString(R.string.pricing_feature_ai_outputs),
                        getString(R.string.pricing_feature_model_background),
                        getString(R.string.pricing_feature_premium_models_full),
                        getString(R.string.pricing_feature_premium_backgrounds_full),
                        getString(R.string.pricing_feature_rendering_fastest),
                        getString(R.string.pricing_feature_bulk_upload),
                        getString(R.string.pricing_feature_support_priority),
                        getString(R.string.pricing_feature_upload_50),
                        getString(R.string.pricing_feature_turnaround_priority)
                    )
                )
            )
        }

        plansAdapter.submitList(
            basePlans.map { plan ->
                plan.copy(isExpanded = expandedPlanIds.contains(plan.id))
            }
        ) {
            binding.pricingPlansRecyclerView.post {
                (binding.pricingPlansRecyclerView.layoutManager as? StaggeredGridLayoutManager)
                    ?.invalidateSpanAssignments()
                binding.pricingPlansRecyclerView.requestLayout()
            }
        }
    }

    private fun togglePlanDetails(planId: String) {
        val isExpanding = expandedPlanIds.add(planId)
        if (!isExpanding) {
            expandedPlanIds.remove(planId)
        }
        bindPlans()
        if (isExpanding) {
            scrollExpandedPlanIntoView(planId)
        }
    }

    private fun scrollExpandedPlanIntoView(planId: String) {
        val planIndex = basePlans.indexOfFirst { it.id == planId }
        if (planIndex == -1) return

        binding.pricingPlansRecyclerView.post {
            val viewHolder = binding.pricingPlansRecyclerView.findViewHolderForAdapterPosition(planIndex)
            val itemView = viewHolder?.itemView ?: return@post
            itemView.post {
                val targetBottom =
                    itemView.bottom + binding.pricingPlansRecyclerView.top + resources.getDimensionPixelSize(R.dimen._20sdp)
                val desiredScrollY = targetBottom - binding.pricingScrollView.height
                if (desiredScrollY > binding.pricingScrollView.scrollY) {
                    binding.pricingScrollView.smoothScrollTo(0, desiredScrollY)
                }
            }
        }
    }

    private fun bindHistory() {
        val history = listOf(
            CreditHistoryUiModel(
                dateLabel = "2026-04-20 06:03",
                planLabel = "starter_approved",
                packLabel = "2500",
                creditsLabel = "+2500"
            ),
            CreditHistoryUiModel(
                dateLabel = "2026-04-18 12:32",
                planLabel = "starter_approved",
                packLabel = "50",
                creditsLabel = "+50"
            ),
            CreditHistoryUiModel(
                dateLabel = "2026-04-18 07:13",
                planLabel = "starter_approved",
                packLabel = "50",
                creditsLabel = "+50"
            ),
            CreditHistoryUiModel(
                dateLabel = "2026-04-18 07:00",
                planLabel = "free",
                packLabel = "5",
                creditsLabel = "+5"
            )
        )

        binding.historyEmptyState.isVisible = history.isEmpty()
        binding.historyRowsContainer.removeAllViews()
        history.forEach { item ->
            val rowBinding = ItemCreditHistoryBinding.inflate(layoutInflater, binding.historyRowsContainer, false)
            rowBinding.dateValue.text = item.dateLabel
            rowBinding.planValue.text = item.planLabel
            rowBinding.packValue.text = item.packLabel
            rowBinding.creditsValue.text = item.creditsLabel
            binding.historyRowsContainer.addView(rowBinding.root)
        }
    }

    private fun calculateSpanCount(): Int {
        val screenWidthDp = resources.configuration.screenWidthDp
        return when {
            screenWidthDp >= 1200 -> 4
            screenWidthDp >= 600 -> 2
            else -> 1
        }
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
