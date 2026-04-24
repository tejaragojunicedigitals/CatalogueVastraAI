package com.nice.cataloguevastra.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nice.cataloguevastra.CatalogueVastraApp
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.adapters.SelectionGridAdapter
import com.nice.cataloguevastra.databinding.BottomSheetModelSelectionBinding
import com.nice.cataloguevastra.model.CatalogueUiState
import com.nice.cataloguevastra.model.ModelSheetItemUiModel
import com.nice.cataloguevastra.model.RailItemUiModel
import com.nice.cataloguevastra.viewmodel.CataloguesViewModel

class VisualSelectionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetModelSelectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CataloguesViewModel by activityViewModels {
        CataloguesViewModel.factory(
            (requireActivity().application as CatalogueVastraApp).appContainer.catalogueRepository
        )
    }

    private val selectionAdapter = SelectionGridAdapter { item ->
        when (railType) {
            VisualRailType.BACKGROUND -> {
                selectedItemId = item.id
            }

            VisualRailType.POSE -> {
                if (!selectedPoseIds.add(item.id)) {
                    selectedPoseIds.remove(item.id)
                }
            }
        }
        renderGrid(latestState)
    }

    private val railType: VisualRailType
        get() = requireArguments().getString(ARG_RAIL_TYPE)
            ?.let(VisualRailType::fromValue)
            ?: VisualRailType.BACKGROUND

    private var selectedItemId: String? = null
    private val selectedPoseIds = linkedSetOf<String>()
    private var latestState: CatalogueUiState? = null
    private var selectionInitialized = false

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

        binding.modelSheetTitle.text = getString(railType.titleRes)
        binding.closeButton.setOnClickListener { dismiss() }
        binding.cancelButton.setOnClickListener { dismiss() }
        binding.applyButton.setOnClickListener {
            applySelection()
            dismiss()
        }
        binding.modelTabLayout.visibility = View.GONE
        binding.libraryFilterRecyclerView.visibility = View.GONE

        binding.modelSheetSubtitle.text = getString(railType.subtitleRes)

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            latestState = state
            if (!selectionInitialized) {
                initializeSelection(state)
                selectionInitialized = true
            }
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

    private fun initializeSelection(state: CatalogueUiState) {
        when (railType) {
            VisualRailType.BACKGROUND -> {
                selectedItemId = state.backgroundRail.items
                    .filterIsInstance<RailItemUiModel.Visual>()
                    .firstOrNull { it.isSelected }
                    ?.id
            }

            VisualRailType.POSE -> {
                selectedPoseIds.clear()
                selectedPoseIds += state.poseRail.items
                    .filterIsInstance<RailItemUiModel.Visual>()
                    .filter { it.isSelected }
                    .map { it.id }
            }
        }
    }

    private fun renderGrid(state: CatalogueUiState?) {
        val currentState = state ?: return
        val items = when (railType) {
            VisualRailType.BACKGROUND -> currentState.backgroundRail.items
            VisualRailType.POSE -> currentState.poseRail.items
        }.filterIsInstance<RailItemUiModel.Visual>()
            .map { item ->
                ModelSheetItemUiModel(
                    id = item.id,
                    label = item.label,
                    imageRes = item.imageRes,
                    imageUrl = item.imageUrl,
                    isSelected = when (railType) {
                        VisualRailType.BACKGROUND -> item.id == selectedItemId
                        VisualRailType.POSE -> selectedPoseIds.contains(item.id)
                    }
                )
            }

        selectionAdapter.submitList(items)
    }

    private fun applySelection() {
        val state = latestState ?: return
        when (railType) {
            VisualRailType.BACKGROUND -> {
                selectedItemId?.let(viewModel::selectBackground)
            }

            VisualRailType.POSE -> {
                val currentSelection = state.poseRail.items
                    .filterIsInstance<RailItemUiModel.Visual>()
                    .filter { it.isSelected }
                    .mapTo(linkedSetOf()) { it.id }

                (currentSelection - selectedPoseIds).forEach(viewModel::selectPose)
                (selectedPoseIds - currentSelection).forEach(viewModel::selectPose)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    enum class VisualRailType(
        val value: String,
        val titleRes: Int,
        val subtitleRes: Int
    ) {
        BACKGROUND("background", R.string.background_sheet_title, R.string.background_sheet_subtitle),
        POSE("pose", R.string.pose_sheet_title, R.string.pose_sheet_subtitle);

        companion object {
            fun fromValue(value: String): VisualRailType {
                return entries.firstOrNull { it.value == value } ?: BACKGROUND
            }
        }
    }

    companion object {
        private const val ARG_RAIL_TYPE = "arg_rail_type"

        fun newInstance(type: VisualRailType): VisualSelectionBottomSheet {
            return VisualSelectionBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_RAIL_TYPE, type.value)
                }
            }
        }

        fun tagFor(type: VisualRailType): String = "VisualSelectionBottomSheet_${type.value}"
    }
}
