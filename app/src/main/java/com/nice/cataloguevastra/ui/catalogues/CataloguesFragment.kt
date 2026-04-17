package com.nice.cataloguevastra.ui.catalogues

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.FragmentCataloguesBinding
import com.nice.cataloguevastra.ui.catalogues.adapter.ImageRailAdapter
import com.nice.cataloguevastra.ui.catalogues.adapter.SelectableChipAdapter
import com.nice.cataloguevastra.ui.catalogues.model.CatalogueUiState
import com.nice.cataloguevastra.ui.catalogues.model.RailItemUiModel

class CataloguesFragment : Fragment() {

    private var _binding: FragmentCataloguesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CataloguesViewModel by activityViewModels {
        CataloguesViewModel.factory()
    }

    private val platformAdapter = SelectableChipAdapter { chip ->
        viewModel.selectPlatform(chip.id)
    }
    private val aspectRatioAdapter = SelectableChipAdapter { chip ->
        viewModel.selectAspectRatio(chip.id)
    }
    private val modelAdapter = ImageRailAdapter(
        onVisualClicked = { item -> viewModel.selectModel(item.id) },
        onUploadClicked = { showModelBottomSheet() }
    )
    private val backgroundAdapter = ImageRailAdapter(
        onVisualClicked = { item -> viewModel.selectBackground(item.id) },
        onUploadClicked = { showDummyMessage(getString(R.string.background_upload_message)) }
    )
    private val poseAdapter = ImageRailAdapter(
        onVisualClicked = { item -> viewModel.selectPose(item.id) },
        onUploadClicked = {}
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCataloguesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupInteractions()
        observeUiState()
    }

    private fun setupRecyclerViews() = with(binding) {
        platformRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        platformRecyclerView.adapter = platformAdapter

        aspectRatioRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        aspectRatioRecyclerView.adapter = aspectRatioAdapter

        modelRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        modelRecyclerView.adapter = modelAdapter

        backgroundRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        backgroundRecyclerView.adapter = backgroundAdapter

        poseRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        poseRecyclerView.adapter = poseAdapter
    }

    private fun setupInteractions() = with(binding) {
        catalogueForInput.setOnItemClickListener { _, _, position, _ ->
            val value = catalogueForInput.adapter.getItem(position).toString()
            viewModel.updateCatalogueFor(value)
        }
        categoryInput.setOnItemClickListener { _, _, position, _ ->
            val value = categoryInput.adapter.getItem(position).toString()
            viewModel.updateCategory(value)
        }
        outfitTypeInput.setOnItemClickListener { _, _, position, _ ->
            val value = outfitTypeInput.adapter.getItem(position).toString()
            viewModel.updateOutfitType(value)
        }
        productCodeInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.updateProductCode(productCodeInput.text?.toString().orEmpty())
            }
        }
        modelViewAll.setOnClickListener { showModelBottomSheet() }
        backgroundViewAll.setOnClickListener {
            showDummyMessage(getString(R.string.view_all_background_message))
        }
        poseViewAll.setOnClickListener {
            showDummyMessage(getString(R.string.view_all_pose_message))
        }
        businessLogoBrowse.setOnClickListener {
            showDummyMessage(getString(R.string.business_logo_browse_message))
        }
        uploadCard.setOnClickListener {
            showDummyMessage(getString(R.string.product_upload_message))
        }
        generateButton.setOnClickListener {
            showDummyMessage(getString(R.string.generation_dummy_message))
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner, ::render)
    }

    private fun render(state: CatalogueUiState) = with(binding) {
        if (catalogueForInput.adapter == null) {
            catalogueForInput.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    state.catalogueForOptions
                )
            )
            categoryInput.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    state.categoryOptions
                )
            )
            outfitTypeInput.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    state.outfitTypeOptions
                )
            )
        }

        catalogueForInput.setText(state.selectedCatalogueFor, false)
        categoryInput.setText(state.selectedCategory, false)
        outfitTypeInput.setText(state.selectedOutfitType, false)
        businessLogoValue.text = state.businessLogoName
        if (productCodeInput.text?.toString() != state.productCode) {
            productCodeInput.setText(state.productCode)
            productCodeInput.setSelection(state.productCode.length)
        }

        platformAdapter.submitList(state.platforms)
        aspectRatioAdapter.submitList(state.aspectRatios)
        modelAdapter.submitList(state.modelRail.items)
        backgroundAdapter.submitList(state.backgroundRail.items)
        poseAdapter.submitList(state.poseRail.items)
    }

    private fun showModelBottomSheet() {
        val existingSheet = childFragmentManager.findFragmentByTag(ModelSelectionBottomSheet.TAG)
        if (existingSheet == null) {
            ModelSelectionBottomSheet().show(childFragmentManager, ModelSelectionBottomSheet.TAG)
        }
    }

    private fun showDummyMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
