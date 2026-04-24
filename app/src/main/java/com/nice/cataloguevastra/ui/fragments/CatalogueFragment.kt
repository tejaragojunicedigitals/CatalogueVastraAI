package com.nice.cataloguevastra.ui.fragments

import android.app.DownloadManager
import android.content.Context
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.FragmentCatalogueBinding
import com.nice.cataloguevastra.adapters.CataloguesAdapter
import com.nice.cataloguevastra.model.CatalogueCardUiModel
import com.nice.cataloguevastra.ui.base.BaseFragment
import com.nice.cataloguevastra.viewmodel.CatalogueListUiState
import com.nice.cataloguevastra.viewmodel.CatalogueListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

class CatalogueFragment : BaseFragment() {

    private var _binding: FragmentCatalogueBinding? = null
    private val binding get() = _binding!!
    private val catalogueListViewModel: CatalogueListViewModel by activityViewModels()
    private lateinit var cataloguesAdapter: CataloguesAdapter

    private val allCatalogues = mutableListOf<CatalogueCardUiModel>()
    private val selectedCatalogueIds = linkedSetOf<String>()
    private var visibleCatalogues: List<CatalogueCardUiModel> = emptyList()
    private var searchQuery = ""
    private var selectedDateFilter = ""
    private var selectedCategoryFilter = ""
    private var selectedPlatformFilter = ""
    private var activeFilterType = FilterType.DATE
    private var isLoadingCatalogues = false
    private var cataloguesErrorMessage: String? = null
    private var isPullRefreshing = false
    private var filterJob: Job? = null

    private val today: LocalDate = LocalDate.now()

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
        setupListeners()
        setupSwipeRefresh()
        observeCatalogues()
        updateFilterLabels()
        updateFilterMenuState()
        applyFilters()
        catalogueListViewModel.loadCatalogues(forceRefresh = true)
    }

    override fun loaderViewId(): Int = R.id.catalogueLoadingOverlay

    private fun setupRecyclerView() = with(binding.cataloguesRecyclerView) {
        cataloguesAdapter = CataloguesAdapter(
            onItemClick = { catalogue ->
                openCataloguePreview(catalogue)
            },
            onDeleteClick = { catalogue ->
                catalogueListViewModel.deleteCatalogue(catalogue.id)
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

    private fun openCataloguePreview(catalogue: CatalogueCardUiModel) {
        val imageUrls = catalogue.thumbnails.mapNotNull { it.url }
            .ifEmpty { listOfNotNull(catalogue.previewImageUrl) }

        if (imageUrls.isEmpty()) {
            showMessage(getString(R.string.catalogues_empty_state))
            return
        }

        findNavController().navigate(
            R.id.assetPreviewFragment,
            AssetPreviewFragment.createArgs(
                title = catalogue.title,
                imageUrls = ArrayList(imageUrls),
                showThumbnails = imageUrls.size > 1,
                source = AssetPreviewFragment.SOURCE_CATALOGUES
            )
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

            val selectableCatalogues = visibleCatalogues.take(MAX_DOWNLOAD_SELECTION)
            val allVisibleSelected = selectableCatalogues.all { selectedCatalogueIds.contains(it.id) }
            if (allVisibleSelected) {
                selectableCatalogues.forEach { selectedCatalogueIds.remove(it.id) }
            } else {
                selectedCatalogueIds.clear()
                selectableCatalogues.forEach { selectedCatalogueIds.add(it.id) }
                if (visibleCatalogues.size > MAX_DOWNLOAD_SELECTION) {
                    showMessage(getString(R.string.catalogues_download_limit))
                }
            }
            syncSelectionUi()
        }

        downloadSelectedButton.setOnClickListener {
            downloadSelectedCatalogues()
        }
    }

    private fun setupSwipeRefresh() = with(binding.swipeRefreshLayout) {
        setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.primaryColor))
        setProgressBackgroundColorSchemeColor(ContextCompat.getColor(requireContext(), R.color.white))
        setOnRefreshListener {
            isPullRefreshing = true
            showLoader()
            catalogueListViewModel.loadCatalogues(forceRefresh = true)
        }
    }

    private fun observeCatalogues() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    catalogueListViewModel.uiState.collect(::renderCatalogueState)
                }
                launch {
                    catalogueListViewModel.events.collect(::showMessage)
                }
            }
        }
    }

    private fun renderCatalogueState(state: CatalogueListUiState) {
        binding.swipeRefreshLayout.isRefreshing = isPullRefreshing && state.isLoading
        if (!state.isLoading) {
            isPullRefreshing = false
        }
        if (state.isLoading) showLoader() else hideLoader()
        isLoadingCatalogues = state.isLoading
        cataloguesErrorMessage = state.errorMessage
        allCatalogues.clear()
        allCatalogues += state.catalogues
        selectedCatalogueIds.retainAll(state.catalogues.map { it.id }.toSet())
        applyFilters()
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
        filterJob?.cancel()

        val sourceCatalogues = allCatalogues.toList()
        val query = searchQuery
        val dateFilter = selectedDateFilter
        val categoryFilter = selectedCategoryFilter
        val platformFilter = selectedPlatformFilter
        val defaultDateLabel = defaultLabel(FilterType.DATE)
        val defaultCategoryLabel = defaultLabel(FilterType.CATEGORY)
        val defaultPlatformLabel = defaultLabel(FilterType.PLATFORM)
        val todayLabel = getString(R.string.catalogues_date_today)
        val thisWeekLabel = getString(R.string.catalogues_date_this_week)
        val thisMonthLabel = getString(R.string.catalogues_date_this_month)
        val todayDate = today

        filterJob = viewLifecycleOwner.lifecycleScope.launch {
            val filteredCatalogues = withContext(Dispatchers.Default) {
                sourceCatalogues.filter { item ->
                    item.matchesSearch(query) &&
                        item.matchesDateFilter(
                            selectedValue = dateFilter,
                            defaultValue = defaultDateLabel,
                            todayLabel = todayLabel,
                            thisWeekLabel = thisWeekLabel,
                            thisMonthLabel = thisMonthLabel,
                            todayDate = todayDate
                        ) &&
                        item.matchesFilter(categoryFilter, defaultCategoryLabel, item.categoryTag) &&
                        item.matchesFilter(platformFilter, defaultPlatformLabel, item.platformTag)
                }
            }

            visibleCatalogues = filteredCatalogues
            selectedCatalogueIds.retainAll(filteredCatalogues.map { it.id }.toSet())
            cataloguesAdapter.submitList(filteredCatalogues)
            syncSelectionUi()
            renderListState()
        }
    }

    private fun renderListState() {
        binding.cataloguesRecyclerView.alpha = if (isLoadingCatalogues && visibleCatalogues.isEmpty()) 0.35f else 1f
        binding.emptyStateText.isVisible = isLoadingCatalogues || visibleCatalogues.isEmpty()
        binding.emptyStateText.text = when {
            isLoadingCatalogues -> getString(R.string.catalogues_loading)
            !cataloguesErrorMessage.isNullOrBlank() -> cataloguesErrorMessage
            else -> getString(R.string.catalogues_empty_state)
        }
    }

    private fun toggleSelection(catalogueId: String) {
        if (!selectedCatalogueIds.add(catalogueId)) {
            selectedCatalogueIds.remove(catalogueId)
        } else if (selectedCatalogueIds.size > MAX_DOWNLOAD_SELECTION) {
            selectedCatalogueIds.remove(catalogueId)
            showMessage(getString(R.string.catalogues_download_limit))
        }
        syncSelectionUi()
    }

    private fun syncSelectionUi() {
        cataloguesAdapter.updateSelection(selectedCatalogueIds)
        val hasSelection = selectedCatalogueIds.isNotEmpty()
        binding.downloadSelectedButton.isVisible = hasSelection
        binding.downloadSelectedButton.isEnabled = hasSelection
        binding.downloadSelectedButton.alpha = if (hasSelection) 1f else 0.6f

        val allVisibleSelected = visibleCatalogues.isNotEmpty() &&
            visibleCatalogues.take(MAX_DOWNLOAD_SELECTION).all { selectedCatalogueIds.contains(it.id) }

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

    private fun downloadSelectedCatalogues() {
        val selectedCatalogues = visibleCatalogues
            .filter { selectedCatalogueIds.contains(it.id) }
            .take(MAX_DOWNLOAD_SELECTION)

        if (selectedCatalogues.isEmpty()) {
            showMessage(getString(R.string.catalogues_download_select_prompt))
            return
        }

        val downloads = selectedCatalogues.flatMap { catalogue ->
            catalogue.downloadUrls().mapIndexed { index, imageUrl ->
                imageUrl to buildCatalogueDownloadTitle(catalogue, index)
            }
        }

        if (downloads.isEmpty()) {
            showMessage(getString(R.string.catalogues_download_no_images))
            return
        }

        val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloads.forEach { (imageUrl, title) ->
            val request = DownloadManager.Request(Uri.parse(imageUrl))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setTitle(title)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "$title.jpg"
                )
            downloadManager.enqueue(request)
        }

        showMessage(getString(R.string.catalogues_download_started, downloads.size))
    }

    private fun CatalogueCardUiModel.downloadUrls(): List<String> {
        return thumbnails.mapNotNull { it.url?.takeIf(String::isNotBlank) }
            .ifEmpty { listOfNotNull(previewImageUrl?.takeIf(String::isNotBlank)) }
    }

    private fun buildCatalogueDownloadTitle(catalogue: CatalogueCardUiModel, index: Int): String {
        val safeTitle = catalogue.title
            .ifBlank { "catalogue_${catalogue.id}" }
            .replace("[^A-Za-z0-9_-]+".toRegex(), "_")
            .trim('_')
            .ifBlank { "catalogue_${catalogue.id}" }
        return "${safeTitle}_${index + 1}"
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

    private fun defaultLabel(filterType: FilterType): String {
        return when (filterType) {
            FilterType.DATE -> getString(R.string.catalogues_filter_date)
            FilterType.CATEGORY -> getString(R.string.catalogues_filter_category)
            FilterType.PLATFORM -> getString(R.string.catalogues_filter_platform)
        }
    }

    override fun showMessage(message: String) {
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

    private fun CatalogueCardUiModel.matchesDateFilter(
        selectedValue: String,
        defaultValue: String,
        todayLabel: String,
        thisWeekLabel: String,
        thisMonthLabel: String,
        todayDate: LocalDate
    ): Boolean {
        if (selectedValue == defaultValue) return true

        val createdDate = LocalDate.parse(createdDateIso)
        return when (selectedValue) {
            todayLabel -> createdDate == todayDate
            thisWeekLabel -> {
                val startOfWeek = todayDate.with(DayOfWeek.MONDAY)
                createdDate >= startOfWeek && createdDate <= todayDate
            }
            thisMonthLabel -> {
                createdDate.year == todayDate.year && createdDate.month == todayDate.month
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
        filterJob?.cancel()
        filterJob = null
        _binding = null
    }

    private enum class FilterType {
        DATE,
        CATEGORY,
        PLATFORM
    }

    private companion object {
        const val MAX_DOWNLOAD_SELECTION = 5
    }
}
