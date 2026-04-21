package com.nice.cataloguevastra.ui.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.FragmentCatalogueBinding
import com.nice.cataloguevastra.adapters.CataloguesAdapter
import com.nice.cataloguevastra.model.CatalogueCardUiModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class CatalogueFragment : Fragment() {

    private var _binding: FragmentCatalogueBinding? = null
    private val binding get() = _binding!!
    private lateinit var cataloguesAdapter: CataloguesAdapter

    private val allCatalogues = mutableListOf<CatalogueCardUiModel>()
    private val selectedCatalogueIds = linkedSetOf<String>()
    private var visibleCatalogues: List<CatalogueCardUiModel> = emptyList()
    private var searchQuery = ""
    private var selectedDateFilter = ""
    private var selectedCategoryFilter = ""
    private var selectedPlatformFilter = ""
    private var activeFilterType = FilterType.DATE

    private val today: LocalDate = LocalDate.now()
    private val titleDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.ENGLISH)
    private val subtitleDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCatalogueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectedDateFilter = defaultLabel(FilterType.DATE)
        selectedCategoryFilter = defaultLabel(FilterType.CATEGORY)
        selectedPlatformFilter = defaultLabel(FilterType.PLATFORM)

        setupRecyclerView()
        seedCatalogueItems()
        setupListeners()
        updateFilterLabels()
        updateFilterMenuState()
        applyFilters()
    }

    private fun setupRecyclerView() = with(binding.cataloguesRecyclerView) {
        cataloguesAdapter = CataloguesAdapter(
            onItemClick = {
                findNavController().navigate(R.id.generatedCatalogueFragment)
            },
            onDeleteClick = { catalogue ->
                selectedCatalogueIds.remove(catalogue.id)
                allCatalogues.removeAll { it.id == catalogue.id }
                showMessage(getString(R.string.catalogues_deleted_message, catalogue.title))
                applyFilters()
            },
            onSelectionClick = { catalogue ->
                toggleSelection(catalogue.id)
            }
        )
        layoutManager = GridLayoutManager(requireContext(), calculateSpanCount())
        adapter = cataloguesAdapter
        setHasFixedSize(true)
        itemAnimator = null
        isNestedScrollingEnabled = false
    }

    private fun seedCatalogueItems() {
        if (allCatalogues.isNotEmpty()) return

        allCatalogues += listOf(
            buildCatalogue("5556", 0, 1, R.drawable.model, listOf(R.drawable.model), "Women's", "Amazon"),
            buildCatalogue("5555", 0, 1, R.drawable.model_img, listOf(R.drawable.model_img), "Men's", "Myntra"),
            buildCatalogue("5554", 1, 1, R.drawable.model, listOf(R.drawable.model, R.drawable.model_img), "Women's", "Shopify"),
            buildCatalogue("5553", 2, 1, R.drawable.model, listOf(R.drawable.model), "Boy's", "Flipkart"),
            buildCatalogue("5552", 3, 1, R.drawable.model, listOf(R.drawable.model, R.drawable.model_img), "Girl's", "Meesho"),
            buildCatalogue("5357", 4, 1, R.drawable.model_img, listOf(R.drawable.model_img), "Men's", "Amazon"),
            buildCatalogue("5355", 5, 1, R.drawable.model_img, listOf(R.drawable.model_img), "Women's", "Myntra"),
            buildCatalogue("5354", 6, 1, R.drawable.model, listOf(R.drawable.model, R.drawable.model_img), "Girl's", "Shopify"),
            buildCatalogue("5291", 8, 3, R.drawable.model_img, listOf(R.drawable.model_img, R.drawable.model, R.drawable.model_img), "Women's", "Meesho"),
            buildCatalogue("5290", 10, 3, R.drawable.model, listOf(R.drawable.model, R.drawable.model_img, R.drawable.model), "Boy's", "Amazon"),
            buildCatalogue("5289", 12, 3, R.drawable.model_img, listOf(R.drawable.model_img, R.drawable.model, R.drawable.model_img), "Girl's", "Flipkart"),
            buildCatalogue("5195", 15, 1, R.drawable.model, listOf(R.drawable.model), "Men's", "Shopify")
        )
    }

    private fun setupListeners() = with(binding) {
        createNewButton.setOnClickListener {
            findNavController().navigate(R.id.studioFragment)
        }

        searchInput.addTextChangedListener { editable ->
            searchQuery = editable?.toString().orEmpty().trim()
            applyFilters()
        }

        filterMenuButton.setOnClickListener {
            val shouldShow = !filterPanel.isVisible
            filterPanel.isVisible = shouldShow
            if (shouldShow) {
                showFilterType(activeFilterType)
            } else {
                updateFilterMenuState()
            }
        }

        dateFilterButton.setOnClickListener {
            showFilterType(FilterType.DATE)
        }

        categoryFilterButton.setOnClickListener {
            showFilterType(FilterType.CATEGORY)
        }

        platformFilterButton.setOnClickListener {
            showFilterType(FilterType.PLATFORM)
        }

        selectAllButton.setOnClickListener {
            if (visibleCatalogues.isEmpty()) return@setOnClickListener

            val allVisibleSelected = visibleCatalogues.all { selectedCatalogueIds.contains(it.id) }
            if (allVisibleSelected) {
                visibleCatalogues.forEach { selectedCatalogueIds.remove(it.id) }
            } else {
                visibleCatalogues.forEach { selectedCatalogueIds.add(it.id) }
            }
            syncSelectionUi()
        }

        downloadSelectedButton.setOnClickListener {
            val visibleCount = visibleCatalogues.size
            val message = if (visibleCount == 0) {
                getString(R.string.catalogues_empty_state)
            } else {
                getString(R.string.catalogues_download_all_message, visibleCount)
            }
            showMessage(message)
        }
    }

    private fun showFilterType(filterType: FilterType) {
        activeFilterType = filterType
        binding.filterPanel.isVisible = true
        updateFilterMenuState()
        updateFilterButtons()
        populateFilterOptions()
    }

    private fun populateFilterOptions() {
        val container = binding.filterOptionsContainer
        container.removeAllViews()

        val options = when (activeFilterType) {
            FilterType.DATE -> listOf(
                getString(R.string.catalogues_date_today),
                getString(R.string.catalogues_date_this_week),
                getString(R.string.catalogues_date_this_month)
            )
            FilterType.CATEGORY -> listOf(
                getString(R.string.catalogues_category_womens),
                getString(R.string.catalogues_category_mens),
                getString(R.string.catalogues_category_boys),
                getString(R.string.catalogues_category_girls)
            )
            FilterType.PLATFORM -> listOf(
                getString(R.string.catalogues_platform_amazon),
                getString(R.string.catalogues_platform_flipkart),
                getString(R.string.catalogues_platform_myntra),
                getString(R.string.catalogues_platform_shopify),
                getString(R.string.catalogues_platform_meesho)
            )
        }

        options.forEach { option ->
            container.addView(createFilterOptionView(option))
        }
    }

    private fun createFilterOptionView(option: String): View {
        val context = requireContext()
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen._10sdp)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen._12sdp)
        val iconSize = resources.getDimensionPixelSize(R.dimen._18sdp)
        val spacing = resources.getDimensionPixelSize(R.dimen._12sdp)

        return LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

            val optionIcon = AppCompatImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
                setImageResource(
                    if (isOptionSelected(option)) {
                        R.drawable.ic_filter_checkbox_checked
                    } else {
                        R.drawable.ic_filter_checkbox_unchecked
                    }
                )
            }

            val optionLabel = AppCompatTextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = spacing
                }
                setTextColor(ContextCompat.getColor(context, R.color.bottomNavInactive))
                text = option
                textSize = 15f
                typeface = resources.getFont(R.font.satoshi_medium)
            }

            addView(optionIcon)
            addView(optionLabel)

            setOnClickListener {
                onFilterOptionSelected(option)
            }
        }
    }

    private fun onFilterOptionSelected(option: String) {
        val currentValue = currentFilterValue(activeFilterType)
        val newValue = if (currentValue == option) {
            defaultLabel(activeFilterType)
        } else {
            option
        }

        when (activeFilterType) {
            FilterType.DATE -> selectedDateFilter = newValue
            FilterType.CATEGORY -> selectedCategoryFilter = newValue
            FilterType.PLATFORM -> selectedPlatformFilter = newValue
        }

        updateFilterLabels()
        populateFilterOptions()
        applyFilters()
    }

    private fun isOptionSelected(option: String): Boolean {
        return currentFilterValue(activeFilterType) == option
    }

    private fun currentFilterValue(filterType: FilterType): String {
        return when (filterType) {
            FilterType.DATE -> selectedDateFilter
            FilterType.CATEGORY -> selectedCategoryFilter
            FilterType.PLATFORM -> selectedPlatformFilter
        }
    }

    private fun updateFilterMenuState() {
        binding.filterMenuButton.setIconResource(
            if (binding.filterPanel.isVisible) {
                R.drawable.ic_close
            } else {
                R.drawable.ic_menu_lines
            }
        )
        updateFilterButtons()
    }

    private fun updateFilterButtons() {
        updateFilterButton(binding.dateFilterButton, FilterType.DATE)
        updateFilterButton(binding.categoryFilterButton, FilterType.CATEGORY)
        updateFilterButton(binding.platformFilterButton, FilterType.PLATFORM)
    }

    private fun updateFilterButton(button: MaterialButton, filterType: FilterType) {
        val context = requireContext()
        val isActive = binding.filterPanel.isVisible && activeFilterType == filterType
        button.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                context,
                if (isActive) R.color.primaryContainer else R.color.white
            )
        )
        button.strokeColor = ColorStateList.valueOf(
            ContextCompat.getColor(
                context,
                if (isActive) R.color.primaryColor else R.color.grey
            )
        )
        button.setTextColor(
            ContextCompat.getColor(
                context,
                if (isActive) R.color.primaryColor else R.color.bottomNavInactive
            )
        )
        button.iconTint = ColorStateList.valueOf(
            ContextCompat.getColor(
                context,
                if (isActive) R.color.primaryColor else R.color.bottomNavInactive
            )
        )
    }

    private fun applyFilters() {
        visibleCatalogues = allCatalogues.filter { item ->
            item.matchesSearch(searchQuery) &&
                item.matchesDateFilter(selectedDateFilter) &&
                item.matchesFilter(selectedCategoryFilter, defaultLabel(FilterType.CATEGORY), item.categoryTag) &&
                item.matchesFilter(selectedPlatformFilter, defaultLabel(FilterType.PLATFORM), item.platformTag)
        }
        cataloguesAdapter.submitList(visibleCatalogues)
        syncSelectionUi()
        binding.emptyStateText.isVisible = visibleCatalogues.isEmpty()
    }

    private fun toggleSelection(catalogueId: String) {
        if (!selectedCatalogueIds.add(catalogueId)) {
            selectedCatalogueIds.remove(catalogueId)
        }
        syncSelectionUi()
    }

    private fun syncSelectionUi() {
        cataloguesAdapter.updateSelection(selectedCatalogueIds)
        binding.downloadSelectedButton.isEnabled = visibleCatalogues.isNotEmpty()
        binding.downloadSelectedButton.alpha = if (visibleCatalogues.isNotEmpty()) 1f else 0.6f

        val allVisibleSelected = visibleCatalogues.isNotEmpty() &&
            visibleCatalogues.all { selectedCatalogueIds.contains(it.id) }

        binding.selectAllButton.text = if (allVisibleSelected) {
            getString(R.string.catalogues_clear_selection)
        } else {
            getString(R.string.catalogues_select_all)
        }
        binding.selectAllButton.setIconResource(
            if (allVisibleSelected) {
                R.drawable.ic_catalogue_select_checked
            } else {
                R.drawable.ic_catalogue_select_unchecked
            }
        )
    }

    private fun updateFilterLabels() = with(binding) {
        dateFilterButton.text = selectedDateFilter
        categoryFilterButton.text = selectedCategoryFilter
        platformFilterButton.text = selectedPlatformFilter
    }

    private fun calculateSpanCount(): Int {
        val screenWidthDp = resources.configuration.screenWidthDp
        return when {
            screenWidthDp >= 1240 -> 5
            screenWidthDp >= 840 -> 4
            screenWidthDp >= 600 -> 3
            else -> 2
        }
    }

    private fun buildCatalogue(
        idSuffix: String,
        daysAgo: Long,
        imageCount: Int,
        previewImageRes: Int,
        thumbnails: List<Int>,
        category: String,
        platform: String
    ): CatalogueCardUiModel {
        val createdDate = today.minusDays(daysAgo)
        return CatalogueCardUiModel(
            id = "cat_$idSuffix",
            title = "Catalogue_${idSuffix}_${createdDate.format(titleDateFormatter)} - $imageCount ${if (imageCount == 1) "Image" else "Images"}",
            subtitle = "${createdDate.format(subtitleDateFormatter)} | $platform",
            previewImageRes = previewImageRes,
            thumbnails = thumbnails,
            createdDateIso = createdDate.toString(),
            categoryTag = category,
            platformTag = platform
        )
    }

    private fun defaultLabel(filterType: FilterType): String {
        return when (filterType) {
            FilterType.DATE -> getString(R.string.catalogues_filter_date)
            FilterType.CATEGORY -> getString(R.string.catalogues_filter_category)
            FilterType.PLATFORM -> getString(R.string.catalogues_filter_platform)
        }
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun CatalogueCardUiModel.matchesSearch(query: String): Boolean {
        if (query.isBlank()) return true
        val normalizedQuery = query.lowercase()
        return title.lowercase().contains(normalizedQuery) ||
            subtitle.lowercase().contains(normalizedQuery) ||
            categoryTag.lowercase().contains(normalizedQuery) ||
            platformTag.lowercase().contains(normalizedQuery)
    }

    private fun CatalogueCardUiModel.matchesDateFilter(selectedValue: String): Boolean {
        if (selectedValue == defaultLabel(FilterType.DATE)) return true

        val createdDate = LocalDate.parse(createdDateIso)
        return when (selectedValue) {
            getString(R.string.catalogues_date_today) -> createdDate == today
            getString(R.string.catalogues_date_this_week) -> {
                val startOfWeek = today.with(DayOfWeek.MONDAY)
                createdDate >= startOfWeek && createdDate <= today
            }
            getString(R.string.catalogues_date_this_month) -> {
                createdDate.year == today.year && createdDate.month == today.month
            }
            else -> true
        }
    }

    private fun CatalogueCardUiModel.matchesFilter(
        selectedValue: String,
        defaultValue: String,
        itemValue: String
    ): Boolean {
        return selectedValue == defaultValue || selectedValue == itemValue
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private enum class FilterType {
        DATE,
        CATEGORY,
        PLATFORM
    }
}
