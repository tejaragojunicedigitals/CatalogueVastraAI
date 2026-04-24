package com.nice.cataloguevastra.ui.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.FragmentPricingBinding
import com.nice.cataloguevastra.databinding.ItemPricingComparePlanBinding
import com.nice.cataloguevastra.databinding.ItemPricingComparisonHeaderRowBinding
import com.nice.cataloguevastra.databinding.ItemPricingComparisonRowBinding
import com.nice.cataloguevastra.databinding.ItemPricingMobileRowBinding
import com.nice.cataloguevastra.databinding.ItemPricingMobileSectionBinding
import com.nice.cataloguevastra.viewmodel.PricingComparisonSectionUiModel
import com.nice.cataloguevastra.viewmodel.PricingPlanCardUiModel
import com.nice.cataloguevastra.viewmodel.PricingUiState
import com.nice.cataloguevastra.viewmodel.PricingViewModel
import kotlinx.coroutines.launch

class PricingFragment : Fragment() {

    private var _binding: FragmentPricingBinding? = null
    private val binding get() = _binding!!
    private val pricingViewModel: PricingViewModel by viewModels()

    private val countries = listOf("India", "United States", "UAE")
    private var selectedCountry = "India"
    private val isTabletLayout: Boolean
        get() = resources.configuration.smallestScreenWidthDp >= 600

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
        observePricing()
        pricingViewModel.refresh()
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

    private fun observePricing() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pricingViewModel.uiState.collect(::renderPricing)
            }
        }
    }

    private fun renderPricing(state: PricingUiState) {
        bindPlanCards(state.plans)
        if (isTabletLayout) {
            bindTabletComparisonTable(state.comparisonSections, state.plans)
        } else {
            bindMobileComparisonSections(state.comparisonSections)
        }
    }

    private fun bindPlanCards(plans: List<PricingPlanCardUiModel>) {
        val spacing = resources.getDimensionPixelSize(R.dimen._12sdp)

        binding.pricingPlansContainer.removeAllViews()
        plans.forEachIndexed { index, plan ->
            val itemBinding = ItemPricingComparePlanBinding.inflate(
                layoutInflater,
                binding.pricingPlansContainer,
                false
            )

            itemBinding.root.layoutParams = LinearLayout.LayoutParams(
                if (isTabletLayout) 0 else ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                if (isTabletLayout) {
                    weight = 1f
                    if (index < plans.lastIndex) marginEnd = spacing
                } else if (index > 0) {
                    topMargin = spacing
                }
            }

            bindPlanCard(itemBinding, plan)
            binding.pricingPlansContainer.addView(itemBinding.root)
        }
    }

    private fun bindPlanCard(
        itemBinding: ItemPricingComparePlanBinding,
        plan: PricingPlanCardUiModel
    ) {
        val context = requireContext()
        val primaryColor = ContextCompat.getColor(context, R.color.primaryColor)
        val whiteColor = ContextCompat.getColor(context, R.color.white)
        val titleColor = ContextCompat.getColor(
            context,
            if (plan.isHighlighted) R.color.white else R.color.textPrimary
        )
        val subtitleColor = ContextCompat.getColor(
            context,
            if (plan.isHighlighted) R.color.primaryContainer else R.color.textSecondary
        )
        val buttonBackgroundColor = ContextCompat.getColor(
            context,
            if (plan.isHighlighted) R.color.white else R.color.pricingButtonSurface
        )
        val buttonTextColor = ContextCompat.getColor(
            context,
            if (plan.isHighlighted) R.color.primaryColor else R.color.textPrimary
        )

        itemBinding.root.strokeColor = ContextCompat.getColor(
            context,
            if (plan.isHighlighted) R.color.primaryColor else R.color.pricingPlanBorder
        )
        itemBinding.root.setCardBackgroundColor(whiteColor)
        itemBinding.planContent.background = if (plan.isHighlighted) {
            AppCompatResources.getDrawable(context, R.drawable.bg_pricing_compare_plan_featured)
        } else {
            null
        }

        itemBinding.badgeView.isVisible = !plan.badgeLabel.isNullOrBlank()
        itemBinding.badgeView.text = plan.badgeLabel
        itemBinding.titleView.text = plan.title
        itemBinding.subtitleView.text = plan.subtitle
        itemBinding.creditsView.text = plan.creditsLabel
        itemBinding.actionButton.text = plan.actionLabel

        itemBinding.titleView.setTextColor(titleColor)
        itemBinding.subtitleView.setTextColor(subtitleColor)
        itemBinding.creditsView.setTextColor(titleColor)

        itemBinding.actionButton.backgroundTintList = ColorStateList.valueOf(buttonBackgroundColor)
        itemBinding.actionButton.strokeColor = ColorStateList.valueOf(
            ContextCompat.getColor(
                context,
                if (plan.isHighlighted) R.color.white else R.color.pricingPlanBorder
            )
        )
        itemBinding.actionButton.strokeWidth = if (plan.isHighlighted) 0 else resources.getDimensionPixelSize(R.dimen._1sdp)
        itemBinding.actionButton.setTextColor(buttonTextColor)
        itemBinding.actionButton.setOnClickListener {
            showMessage(getString(R.string.pricing_plan_action_message, plan.title))
        }

        if (plan.isHighlighted) {
            itemBinding.badgeView.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.pricingBestValueBg)
            )
            itemBinding.badgeView.setTextColor(
                ContextCompat.getColor(context, R.color.pricingBestValueText)
            )
        } else {
            itemBinding.badgeView.backgroundTintList = ColorStateList.valueOf(primaryColor)
            itemBinding.badgeView.setTextColor(whiteColor)
        }
    }

    private fun bindTabletComparisonTable(
        tableSections: List<PricingComparisonSectionUiModel>,
        plans: List<PricingPlanCardUiModel>
    ) {
        binding.pricingComparisonTable.removeAllViews()

        val headerBinding = ItemPricingComparisonHeaderRowBinding.inflate(
            layoutInflater,
            binding.pricingComparisonTable,
            false
        )
        headerBinding.featureText.text = getString(R.string.pricing_table_header_features)
        headerBinding.starterText.text = plans.firstOrNull { it.id == "starter" }?.title
            ?: getString(R.string.pricing_plan_starter)
        headerBinding.growthText.text = plans.firstOrNull { it.id == "growth" }?.title
            ?: getString(R.string.pricing_plan_growth)
        headerBinding.proText.text = plans.firstOrNull { it.id == "pro" }?.title
            ?: getString(R.string.pricing_plan_pro)
        binding.pricingComparisonTable.addView(headerBinding.root)

        tableSections.forEach { section ->
            val sectionView = layoutInflater.inflate(
                R.layout.item_pricing_comparison_section,
                binding.pricingComparisonTable,
                false
            ) as TextView
            sectionView.text = section.title
            binding.pricingComparisonTable.addView(sectionView)

            section.rows.forEachIndexed { index, row ->
                val rowBinding = ItemPricingComparisonRowBinding.inflate(
                    layoutInflater,
                    binding.pricingComparisonTable,
                    false
                )
                rowBinding.featureText.text = row.feature
                rowBinding.starterText.text = row.starterValue
                rowBinding.growthText.text = row.growthValue
                rowBinding.proText.text = row.proValue
                rowBinding.rowDivider.isVisible = index < section.rows.lastIndex
                binding.pricingComparisonTable.addView(rowBinding.root)
            }
        }
    }

    private fun bindMobileComparisonSections(sections: List<PricingComparisonSectionUiModel>) {
        binding.pricingComparisonTable.removeAllViews()

        sections.forEach { section ->
            val sectionBinding = ItemPricingMobileSectionBinding.inflate(
                layoutInflater,
                binding.pricingComparisonTable,
                false
            )
            sectionBinding.sectionTitle.text = section.title

            section.rows.forEach { row ->
                val rowBinding = ItemPricingMobileRowBinding.inflate(
                    layoutInflater,
                    sectionBinding.rowsContainer,
                    false
                )
                rowBinding.featureText.text = row.feature
                rowBinding.starterText.text = row.starterValue
                rowBinding.growthText.text = row.growthValue
                rowBinding.proText.text = row.proValue
                sectionBinding.rowsContainer.addView(rowBinding.root)
            }

            binding.pricingComparisonTable.addView(sectionBinding.root)
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
