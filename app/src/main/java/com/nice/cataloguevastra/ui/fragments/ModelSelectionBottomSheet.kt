package com.nice.cataloguevastra.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.nice.cataloguevastra.CatalogueVastraApp
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.nice.cataloguevastra.adapters.SelectableChipAdapter
import com.nice.cataloguevastra.adapters.SelectionGridAdapter
import com.nice.cataloguevastra.databinding.BottomSheetModelSelectionBinding
import com.nice.cataloguevastra.model.CatalogueUiState
import com.nice.cataloguevastra.model.ModelLibraryFilterType
import com.nice.cataloguevastra.model.ModelTabType
import com.nice.cataloguevastra.viewmodel.CataloguesViewModel

class ModelSelectionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetModelSelectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CataloguesViewModel by activityViewModels {
        CataloguesViewModel.factory(
            (requireActivity().application as CatalogueVastraApp).appContainer.catalogueRepository
        )
    }

    private val selectionAdapter = SelectionGridAdapter { item ->
        selectedModelId = item.id
        renderGrid(latestState)
    }
    private val filterAdapter = SelectableChipAdapter { chip ->
        selectedFilter = ModelLibraryFilterType.entries.firstOrNull { it.id == chip.id }
            ?: ModelLibraryFilterType.ALL
        renderLibraryFilters(latestState)
        renderGrid(latestState)
    }

    private var selectedTab = ModelTabType.LIBRARY
    private var selectedFilter = ModelLibraryFilterType.ALL
    private var selectedModelId: String? = null
    private var latestState: CatalogueUiState? = null
    private var tabsInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetModelSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.modelGridRecyclerView.layoutManager = GridLayoutManager(
            requireContext(),
            if (resources.configuration.smallestScreenWidthDp >= 600) 5 else 4
        )
        binding.modelGridRecyclerView.adapter = selectionAdapter
        binding.libraryFilterRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.libraryFilterRecyclerView.adapter = filterAdapter

        binding.closeButton.setOnClickListener { dismiss() }
        binding.cancelButton.setOnClickListener { dismiss() }
        binding.applyButton.setOnClickListener {
            selectedModelId?.let(viewModel::selectModel)
            dismiss()
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            latestState = state
            if (!tabsInitialized) {
                selectedModelId = state.modelSelection.selectedModelId
                setupTabs(state)
                tabsInitialized = true
            }
            renderLibraryFilters(state)
            renderGrid(state)
        }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun setupTabs(state: CatalogueUiState) = with(binding.modelTabLayout) {
        removeAllTabs()
        state.modelSelection.tabs.forEach { tabType ->
            addTab(newTab().setText(tabType.label).setTag(tabType))
        }

        getTabAt(state.modelSelection.tabs.indexOf(selectedTab))?.select()
        addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedTab = tab.tag as? ModelTabType ?: ModelTabType.LIBRARY
                renderGrid(latestState)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun renderGrid(state: CatalogueUiState?) {
        val currentState = state ?: return
        val currentSelection = selectedModelId ?: currentState.modelSelection.selectedModelId
        val sourceItems = when (selectedTab) {
            ModelTabType.LIBRARY -> {
                currentState.modelSelection.libraryItemsByFilter[selectedFilter].orEmpty()
            }
            ModelTabType.YOUR_MODELS -> currentState.modelSelection.yourModels
        }
        val items = sourceItems.map { item ->
            item.copy(isSelected = item.id == currentSelection)
        }
        selectionAdapter.submitList(items)
    }

    private fun renderLibraryFilters(state: CatalogueUiState?) {
        val currentState = state ?: return
        val showFilters = selectedTab == ModelTabType.LIBRARY
        binding.libraryFilterRecyclerView.visibility = if (showFilters) View.VISIBLE else View.GONE
        if (!showFilters) return

        val filters = currentState.modelSelection.libraryFilters.map { filter ->
            filter.copy(isSelected = filter.id == selectedFilter.id)
        }
        filterAdapter.submitList(filters)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ModelSelectionBottomSheet"
    }
}
