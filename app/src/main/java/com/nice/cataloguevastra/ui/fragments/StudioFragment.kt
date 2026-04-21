package com.nice.cataloguevastra.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nice.cataloguevastra.CatalogueVastraApp
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.FragmentStudioBinding
import com.nice.cataloguevastra.ui.activities.LoginActivity
import com.nice.cataloguevastra.viewmodel.CataloguesViewModel
import com.nice.cataloguevastra.adapters.ImageRailAdapter
import com.nice.cataloguevastra.adapters.SelectableChipAdapter
import com.nice.cataloguevastra.model.CatalogueUiState
import com.nice.cataloguevastra.utils.ImagePickerManager
import com.nice.cataloguevastra.viewmodel.CataloguesViewModel.Companion.DROPDOWN_PLACEHOLDER

class StudioFragment : Fragment() {

    private var _binding: FragmentStudioBinding? = null
    private val binding get() = _binding!!
    private lateinit var imagePickerManager: ImagePickerManager
    private var pendingImageTarget = UploadTarget.PRODUCT
    private var selectedProductImageUri: Uri? = null

    private val viewModel: CataloguesViewModel by activityViewModels {
        CataloguesViewModel.factory(
            (requireActivity().application as CatalogueVastraApp).appContainer.catalogueRepository
        )
    }

    private val platformAdapter = SelectableChipAdapter { chip ->
        viewModel.selectPlatform(chip.id)
    }
    private val aspectRatioAdapter = SelectableChipAdapter { chip ->
        viewModel.selectAspectRatio(chip.id)
    }
    private val modelAdapter = ImageRailAdapter(
        onVisualClicked = { item -> viewModel.selectModel(item.id) },
        onUploadClicked = { openImagePickerFor(UploadTarget.MODEL) }
    )
    private val backgroundAdapter = ImageRailAdapter(
        onVisualClicked = { item -> viewModel.selectBackground(item.id) },
        onUploadClicked = { openImagePickerFor(UploadTarget.BACKGROUND) }
    )
    private val poseAdapter = ImageRailAdapter(
        onVisualClicked = { item -> viewModel.selectPose(item.id) },
        onUploadClicked = {}
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePickerManager = ImagePickerManager(this, requireContext(), ::handlePickedImage)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupInteractions()
        observeUiState()
        observeMessages()
        viewModel.ensureInitialStudioData()
        setupLogin()
    }

    fun setupLogin() {
        binding.generateBtn?.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
        }
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
        catalogueForInput.setOnClickListener { catalogueForInput.showDropDown() }
        catalogueForInput.setOnItemClickListener { _, _, position, _ ->
            val value = catalogueForInput.adapter.getItem(position).toString()
            viewModel.updateCatalogueFor(value)
        }
        categoryInput.setOnClickListener {
            if (categoryInput.adapter != null && categoryInput.adapter.count > 0) {
                categoryInput.showDropDown()
            } else {
                showDummyMessage(getString(R.string.select_catalogue_for_first))
            }
        }
        categoryInput.setOnItemClickListener { _, _, position, _ ->
            val value = categoryInput.adapter.getItem(position).toString()
            viewModel.updateCategory(value)
        }
        outfitTypeInput.setOnClickListener { outfitTypeInput.showDropDown() }
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
        businessLogoBrowse?.setOnClickListener {
            openImagePickerFor(UploadTarget.BUSINESS_LOGO)
        }
        uploadCard.setOnClickListener {
            openImagePickerFor(UploadTarget.PRODUCT)
        }

    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner, ::render)
    }

    private fun observeMessages() {
        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                viewModel.consumeMessage()
            }
        }
    }

    private fun render(state: CatalogueUiState) = with(binding) {
        catalogueForInput.setAdapter(
            ArrayAdapter(
                requireContext(),
                R.layout.item_dropdown_option,
                state.catalogueForOptions
            )
        )
        categoryInput.setAdapter(
            ArrayAdapter(
                requireContext(),
                R.layout.item_dropdown_option,
                state.categoryOptions
            )
        )
        outfitTypeInput.setAdapter(
            ArrayAdapter(
                requireContext(),
                R.layout.item_dropdown_option,
                state.outfitTypeOptions
            )
        )

        catalogueForInput.setText(state.selectedCatalogueFor, false)
        categoryInput.setText(state.selectedCategory, false)
        outfitTypeInput.setText(state.selectedOutfitType, false)
        val hasCategoryOptions = state.categoryOptions.isNotEmpty()
        categoryInput.isEnabled = hasCategoryOptions
        categoryInput.alpha = if (hasCategoryOptions) 1f else 0.65f
        if (!hasCategoryOptions && state.selectedCategory != DROPDOWN_PLACEHOLDER) {
            categoryInput.setText(DROPDOWN_PLACEHOLDER, false)
        }
        businessLogoValue?.text = state.businessLogoName
        if (productCodeInput.text?.toString() != state.productCode) {
            productCodeInput.setText(state.productCode)
            productCodeInput.setSelection(state.productCode.length)
        }
        renderProductUploadCard()

        platformAdapter.submitList(state.platforms)
        aspectRatioAdapter.submitList(state.aspectRatios)
        modelAdapter.submitList(state.modelRail.items)
        backgroundAdapter.submitList(state.backgroundRail.items)
        poseAdapter.submitList(state.poseRail.items)
    }

    private fun openImagePickerFor(target: UploadTarget) {
        pendingImageTarget = target
        imagePickerManager.openPicker()
    }

    private fun handlePickedImage(uri: Uri) {
        val fileName = resolveFileName(uri)
        when (pendingImageTarget) {
            UploadTarget.PRODUCT -> {
                selectedProductImageUri = uri
                binding.uploadProductTitle.text = fileName
                binding.uploadProductSubtitle.text = getString(R.string.upload_product_selected_caption)
                renderProductUploadCard()
            }
            UploadTarget.MODEL -> {
                viewModel.uploadPickedModel(uri, fileName)
            }
            UploadTarget.BACKGROUND -> {
                viewModel.uploadPickedBackground(uri, fileName)
            }
            UploadTarget.BUSINESS_LOGO -> {
                viewModel.updateBusinessLogoName(fileName)
            }
        }
    }

    private fun resolveFileName(uri: Uri): String {
        val projection = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
        return requireContext().contentResolver.query(uri, projection, null, null, null)
            ?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
            ?: uri.lastPathSegment
            ?: getString(R.string.selected_image_fallback_name)
    }

    private fun renderProductUploadCard() = with(binding) {
        val hasImage = selectedProductImageUri != null
        uploadProductPreview.visibility = if (hasImage) View.VISIBLE else View.GONE
        uploadProductIcon.visibility = if (hasImage) View.GONE else View.VISIBLE
        if (hasImage) {
            uploadProductPreview.setImageURI(selectedProductImageUri)
        } else {
            uploadProductPreview.setImageDrawable(null)
            uploadProductTitle.text = getString(R.string.upload_product_title)
            uploadProductSubtitle.text = getString(R.string.upload_product_subtitle)
        }
    }

    private fun showModelBottomSheet() {
        val existingSheet = childFragmentManager.findFragmentByTag(ModelSelectionBottomSheet.Companion.TAG)
        if (existingSheet == null) {
            ModelSelectionBottomSheet().show(childFragmentManager, ModelSelectionBottomSheet.Companion.TAG)
        }
    }

    private fun showDummyMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private enum class UploadTarget {
        PRODUCT,
        MODEL,
        BACKGROUND,
        BUSINESS_LOGO
    }
}
