package com.nice.cataloguevastra.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.adapters.AssetsAdapter
import com.nice.cataloguevastra.databinding.FragmentAssetsBinding
import com.nice.cataloguevastra.model.AssetTabType
import com.nice.cataloguevastra.ui.base.BaseFragment
import com.nice.cataloguevastra.viewmodel.AssetsUiState
import com.nice.cataloguevastra.viewmodel.AssetsViewModel
import kotlinx.coroutines.launch

class AssetsFragment : BaseFragment() {

    private var _binding: FragmentAssetsBinding? = null
    private val binding get() = _binding!!

    private val assetsViewModel: AssetsViewModel by activityViewModels()
    private lateinit var assetsAdapter: AssetsAdapter
    private val selectedAssetIds = linkedSetOf<String>()
    private var selectedCategory = AssetCategory.ALL
    private var isPullRefreshing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupTabs()
        setupCategoryDropdown()
        setupListeners()
        setupSwipeRefresh()
        observeAssets()
        assetsViewModel.loadAssets(
            category = selectedCategory.apiValue,
            dressName = null
        )
    }

    override fun loaderViewId(): Int = R.id.progressIndicator

    private fun setupTabs() = with(binding.assetTabs) {
        removeAllTabs()
        AssetTabType.entries.forEach { tabType ->
            addTab(newTab().setText(tabType.label))
        }
        addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedAssetIds.clear()
                assetsAdapter.updateSelection(emptySet())
                assetsViewModel.selectTab(AssetTabType.entries[tab.position])
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        getTabAt(assetsViewModel.uiState.value.selectedTab.ordinal)?.select()
    }

    private fun setupRecyclerView() = with(binding.assetsRecyclerView) {
        assetsAdapter = AssetsAdapter(
            onItemClick = { asset ->
                openAssetPreview(asset)
            },
            onSelectionClick = { asset ->
                toggleSelection(asset.id)
            }
        )
        layoutManager = GridLayoutManager(requireContext(), calculateSpanCount())
        adapter = assetsAdapter
        setHasFixedSize(true)
        itemAnimator = null
    }

    private fun setupCategoryDropdown() = with(binding.categoryInput) {
        val labels = AssetCategory.entries.map { getString(it.labelRes) }
        setAdapter(
            ArrayAdapter(
                requireContext(),
                R.layout.item_dropdown_option,
                labels
            )
        )
        setText(getString(selectedCategory.labelRes), false)
        setOnClickListener { showDropDown() }
        setOnItemClickListener { _, _, position, _ ->
            val category = AssetCategory.entries.getOrNull(position) ?: return@setOnItemClickListener
            if (category == selectedCategory) return@setOnItemClickListener
            selectedCategory = category
            selectedAssetIds.clear()
            assetsAdapter.updateSelection(emptySet())
            assetsViewModel.loadAssets(
                category = selectedCategory.apiValue,
                dressName = null,
                forceRefresh = true
            )
        }
    }

    private fun setupListeners() = with(binding) {
        uploadAssetButton.setOnClickListener {
            showMessage(getString(R.string.assets_upload_message))
        }
    }

    private fun setupSwipeRefresh() = with(binding.swipeRefreshLayout) {
        setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.primaryColor))
        setProgressBackgroundColorSchemeColor(ContextCompat.getColor(requireContext(), R.color.white))
        setOnRefreshListener {
            isPullRefreshing = true
            assetsViewModel.loadAssets(
                category = selectedCategory.apiValue,
                dressName = null,
                forceRefresh = true
            )
        }
    }

    private fun observeAssets() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                assetsViewModel.uiState.collect(::render)
            }
        }
    }

    private fun toggleSelection(assetId: String) {
        if (!selectedAssetIds.add(assetId)) {
            selectedAssetIds.remove(assetId)
        }
        assetsAdapter.updateSelection(selectedAssetIds)
    }

    private fun openAssetPreview(asset: com.nice.cataloguevastra.model.AssetCardUiModel) {
        val imageUrls = when (asset.tabType) {
            AssetTabType.PRODUCTS -> asset.thumbnails.mapNotNull { it.url }
                .ifEmpty { listOfNotNull(asset.previewImageUrl) }
            AssetTabType.MODELS, AssetTabType.BACKGROUNDS -> listOfNotNull(
                asset.previewImageUrl ?: asset.thumbnails.firstOrNull()?.url
            )
        }

        if (imageUrls.isEmpty()) {
            showMessage(getString(R.string.assets_open_asset_message, asset.title))
            return
        }

        findNavController().navigate(
            R.id.assetPreviewFragment,
            AssetPreviewFragment.createArgs(
                title = asset.title,
                imageUrls = ArrayList(imageUrls),
                showThumbnails = asset.tabType == AssetTabType.PRODUCTS,
                source = AssetPreviewFragment.SOURCE_ASSETS
            )
        )
    }

    private fun render(state: AssetsUiState) {
        binding.swipeRefreshLayout.isRefreshing = isPullRefreshing && state.isLoading
        if (!state.isLoading) {
            isPullRefreshing = false
        }
        if (state.isLoading) showLoader() else hideLoader()
        if (binding.assetTabs.selectedTabPosition != state.selectedTab.ordinal) {
            binding.assetTabs.getTabAt(state.selectedTab.ordinal)?.select()
        }
        binding.emptyStateText.isVisible = !state.isLoading && state.visibleAssets.isEmpty()
        binding.emptyStateText.text = state.errorMessage ?: getString(R.string.assets_empty_state)
        binding.assetsRecyclerView.alpha = if (state.isLoading && state.visibleAssets.isEmpty()) 0.35f else 1f
        assetsAdapter.submitList(state.visibleAssets)
        assetsAdapter.updateSelection(selectedAssetIds)
    }

    private fun calculateSpanCount(): Int {
        val screenWidthDp = resources.configuration.screenWidthDp
        return when {
            screenWidthDp >= 1240 -> 4
            screenWidthDp >= 840 -> 4
            screenWidthDp >= 600 -> 3
            screenWidthDp >= 360 -> 2
            else -> 2
        }
    }

    override fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private enum class AssetCategory(val apiValue: String, val labelRes: Int) {
        ALL("all", R.string.assets_category_all),
        WOMEN("women", R.string.assets_category_women),
        MEN("men", R.string.assets_category_men),
        GIRLS("girl", R.string.assets_category_girls),
        BOYS("boy", R.string.assets_category_boys)
    }
}
