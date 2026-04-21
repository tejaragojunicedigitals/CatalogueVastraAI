package com.nice.cataloguevastra.ui.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.FragmentAssetsBinding
import com.nice.cataloguevastra.adapters.AssetsAdapter
import com.nice.cataloguevastra.model.AssetCardUiModel
import com.nice.cataloguevastra.model.AssetSortType
import com.nice.cataloguevastra.model.AssetTabType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class AssetsFragment : Fragment() {

    private var _binding: FragmentAssetsBinding? = null
    private val binding get() = _binding!!

    private lateinit var assetsAdapter: AssetsAdapter
    private val allAssets = mutableListOf<AssetCardUiModel>()
    private val selectedAssetIds = linkedSetOf<String>()
    private var visibleAssets: List<AssetCardUiModel> = emptyList()
    private var selectedTab = AssetTabType.PRODUCTS
    private var selectedSort = AssetSortType.LATEST

    private val titleDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.ENGLISH)

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
        setupTabs()
        setupRecyclerView()
        seedAssets()
        setupListeners()
        updateSortButtonLabel()
        applyFilters()
    }

    private fun setupTabs() = with(binding.assetTabs) {
        removeAllTabs()
        AssetTabType.entries.forEach { tabType ->
            addTab(newTab().setText(tabType.label))
        }
        addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedTab = AssetTabType.entries[tab.position]
                selectedAssetIds.clear()
                applyFilters()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        getTabAt(0)?.select()
    }

    private fun setupRecyclerView() = with(binding.assetsRecyclerView) {
        assetsAdapter = AssetsAdapter(
            onItemClick = { asset ->
                showMessage(getString(R.string.assets_open_asset_message, asset.title))
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

    private fun setupListeners() = with(binding) {
        uploadAssetButton.setOnClickListener {
            showMessage(getString(R.string.assets_upload_message))
        }

        sortButton.setOnClickListener { anchor ->
            PopupMenu(requireContext(), anchor, Gravity.END).apply {
                menu.add(0, AssetSortType.LATEST.ordinal, 0, getString(R.string.assets_sort_latest))
                menu.add(0, AssetSortType.OLDEST.ordinal, 1, getString(R.string.assets_sort_oldest))
                setOnMenuItemClickListener { item ->
                    selectedSort = AssetSortType.entries[item.itemId]
                    updateSortButtonLabel()
                    applyFilters()
                    true
                }
            }.show()
        }
    }

    private fun seedAssets() {
        if (allAssets.isNotEmpty()) return

        allAssets += listOf(
            asset(
                id = "asset_25234",
                title = "img_25234",
                previewImageRes = R.drawable.model_img,
                thumbnails = listOf(R.drawable.model_img),
                tabType = AssetTabType.PRODUCTS,
                uploadedDate = LocalDate.of(2026, 4, 20),
                imageCount = 1
            ),
            asset(
                id = "asset_women_210226",
                title = "Womens_garden_${LocalDate.of(2026, 2, 21).format(titleDateFormatter)}",
                previewImageRes = R.drawable.model,
                thumbnails = listOf(R.drawable.model, R.drawable.model_img, R.drawable.model),
                tabType = AssetTabType.PRODUCTS,
                uploadedDate = LocalDate.of(2026, 2, 21),
                imageCount = 4
            ),
            asset(
                id = "asset_mens_200226",
                title = "Mens_arch_${LocalDate.of(2026, 2, 20).format(titleDateFormatter)}",
                previewImageRes = R.drawable.model_img,
                thumbnails = listOf(R.drawable.model_img, R.drawable.model),
                tabType = AssetTabType.PRODUCTS,
                uploadedDate = LocalDate.of(2026, 2, 20),
                imageCount = 6
            ),
            asset(
                id = "asset_girls_180226",
                title = "Girls_palace_${LocalDate.of(2026, 2, 18).format(titleDateFormatter)}",
                previewImageRes = R.drawable.model,
                thumbnails = listOf(R.drawable.model),
                tabType = AssetTabType.PRODUCTS,
                uploadedDate = LocalDate.of(2026, 2, 18),
                imageCount = 2
            ),
            asset(
                id = "asset_model_120226",
                title = "Model_library_${LocalDate.of(2026, 2, 12).format(titleDateFormatter)}",
                previewImageRes = R.drawable.model,
                thumbnails = listOf(R.drawable.model, R.drawable.model_img),
                tabType = AssetTabType.MODELS,
                uploadedDate = LocalDate.of(2026, 2, 12),
                imageCount = 5
            ),
            asset(
                id = "asset_model_090226",
                title = "Editorial_pose_${LocalDate.of(2026, 2, 9).format(titleDateFormatter)}",
                previewImageRes = R.drawable.model_img,
                thumbnails = listOf(R.drawable.model_img),
                tabType = AssetTabType.MODELS,
                uploadedDate = LocalDate.of(2026, 2, 9),
                imageCount = 3
            ),
            asset(
                id = "asset_bg_150226",
                title = "Arch_background_${LocalDate.of(2026, 2, 15).format(titleDateFormatter)}",
                previewImageRes = R.drawable.model,
                thumbnails = listOf(R.drawable.model),
                tabType = AssetTabType.BACKGROUNDS,
                uploadedDate = LocalDate.of(2026, 2, 15),
                imageCount = 4
            ),
            asset(
                id = "asset_bg_080226",
                title = "Studio_scene_${LocalDate.of(2026, 2, 8).format(titleDateFormatter)}",
                previewImageRes = R.drawable.model_img,
                thumbnails = listOf(R.drawable.model_img, R.drawable.model),
                tabType = AssetTabType.BACKGROUNDS,
                uploadedDate = LocalDate.of(2026, 2, 8),
                imageCount = 2
            )
        )
    }

    private fun applyFilters() {
        visibleAssets = allAssets
            .filter { it.tabType == selectedTab }
            .sortedWith(
                if (selectedSort == AssetSortType.LATEST) {
                    compareByDescending<AssetCardUiModel> { it.uploadedDate }.thenBy { it.title }
                } else {
                    compareBy<AssetCardUiModel> { it.uploadedDate }.thenBy { it.title }
                }
            )

        assetsAdapter.submitList(visibleAssets)
        assetsAdapter.updateSelection(selectedAssetIds)
        binding.emptyStateText.isVisible = visibleAssets.isEmpty()
    }

    private fun toggleSelection(assetId: String) {
        if (!selectedAssetIds.add(assetId)) {
            selectedAssetIds.remove(assetId)
        }
        assetsAdapter.updateSelection(selectedAssetIds)
    }

    private fun updateSortButtonLabel() {
        binding.sortButton.text = if (selectedSort == AssetSortType.LATEST) {
            getString(R.string.assets_upload_date)
        } else {
            getString(R.string.assets_sort_oldest)
        }
    }

    private fun calculateSpanCount(): Int {
        val screenWidthDp = resources.configuration.screenWidthDp
        return when {
            screenWidthDp >= 1240 -> 4
            screenWidthDp >= 840 -> 4
            screenWidthDp >= 600 -> 3
            else -> 2
        }
    }

    private fun asset(
        id: String,
        title: String,
        previewImageRes: Int,
        thumbnails: List<Int>,
        tabType: AssetTabType,
        uploadedDate: LocalDate,
        imageCount: Int
    ) = AssetCardUiModel(
        id = id,
        title = title,
        subtitle = "$imageCount ${if (imageCount == 1) "Image" else "Images"}",
        previewImageRes = previewImageRes,
        thumbnails = thumbnails,
        tabType = tabType,
        uploadedDate = uploadedDate
    )

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
