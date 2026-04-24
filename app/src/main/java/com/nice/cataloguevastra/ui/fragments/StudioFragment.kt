package com.nice.cataloguevastra.ui.fragments

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.nice.cataloguevastra.CatalogueVastraApp
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.viewmodel.CataloguesViewModel
import com.nice.cataloguevastra.adapters.ImageRailAdapter
import com.nice.cataloguevastra.adapters.SelectableChipAdapter
import com.nice.cataloguevastra.model.CatalogueUiState
import com.nice.cataloguevastra.model.RailItemUiModel
import com.nice.cataloguevastra.model.RailSectionUiModel
import com.nice.cataloguevastra.databinding.FragmentStudioBinding
import com.nice.cataloguevastra.repositories.GenerateCatalogueParams
import com.nice.cataloguevastra.ui.base.BaseFragment
import com.nice.cataloguevastra.utils.ImagePickerManager
import com.nice.cataloguevastra.viewmodel.CataloguesViewModel.Companion.DROPDOWN_PLACEHOLDER

class StudioFragment : BaseFragment() {

    private var _binding: FragmentStudioBinding? = null
    private val binding get() = _binding!!
    private lateinit var imagePickerManager: ImagePickerManager
    private var pendingImageTarget = UploadTarget.PRODUCT
    private var pendingPoseOptionId: String? = null
    private var selectedProductImageUri: Uri? = null
    private var previousModelItemIds: List<String> = emptyList()
    private var previousBackgroundItemIds: List<String> = emptyList()
    private var previousPoseItemIds: List<String> = emptyList()
    private var isUpdatingResolutionInputs = false
    private var latestStudioState: CatalogueUiState? = null
    private val poseGarmentSelections = linkedMapOf<String, PoseGarmentSelection>()

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
        onUploadClicked = {}
    )
    private val backgroundAdapter = ImageRailAdapter(
        onVisualClicked = { item -> viewModel.selectBackground(item.id) },
        onUploadClicked = {}
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
        observeGenerateProgress()
        observeGenerationReturnMessage()
        viewModel.ensureInitialStudioData()
        setupGenerateAction()
    }

    override fun loaderViewId(): Int = R.id.progressIndicator

    private fun setupGenerateAction() {
        binding.generateBtn.setOnClickListener {
            android.util.Log.d("StudioFragment", "Generate Catalogue clicked")
            try {
                submitGenerateCatalogue()
            } catch (error: Exception) {
                android.util.Log.e("StudioFragment", "Generate Catalogue click failed", error)
            }
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
//        outfitTypeInput.setOnClickListener { outfitTypeInput.showDropDown() }
//        outfitTypeInput.setOnItemClickListener { _, _, position, _ ->
//            val value = outfitTypeInput.adapter.getItem(position).toString()
//            viewModel.updateOutfitType(value)
//        }
        productCodeInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.updateProductCode(productCodeInput.text?.toString().orEmpty())
            }
        }
        modelViewAll.setOnClickListener { showModelBottomSheet() }
        backgroundViewAll.setOnClickListener {
            showVisualBottomSheet(VisualSelectionBottomSheet.VisualRailType.BACKGROUND)
        }
        poseViewAll.setOnClickListener {
            showVisualBottomSheet(VisualSelectionBottomSheet.VisualRailType.POSE)
        }
//        businessLogoBrowse?.setOnClickListener {
//            openImagePickerFor(UploadTarget.BUSINESS_LOGO)
//        }
        uploadCard.setOnClickListener {
            openImagePickerFor(UploadTarget.PRODUCT)
        }
        swapResolutionButton.setOnClickListener {
            viewModel.swapResolution()
        }
        resolutionWidthInput.addTextChangedListener { editable ->
            if (isUpdatingResolutionInputs) return@addTextChangedListener
            viewModel.updateResolutionWidth(editable?.toString().orEmpty())
        }
        resolutionHeightInput.addTextChangedListener { editable ->
            if (isUpdatingResolutionInputs) return@addTextChangedListener
            viewModel.updateResolutionHeight(editable?.toString().orEmpty())
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

    private fun observeGenerationReturnMessage() {
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        savedStateHandle.getLiveData<String>(GENERATION_ERROR_RESULT_KEY)
            .observe(viewLifecycleOwner) { message ->
                if (message.isNullOrBlank()) return@observe
                showDummyMessage(message)
                savedStateHandle.remove<String>(GENERATION_ERROR_RESULT_KEY)
            }
    }

    private fun observeGenerateProgress() {
        viewModel.generateProgress.observe(viewLifecycleOwner) { progress ->
            if (progress == null) {
                binding.generateProgressBar.progress = 0
                binding.generateProgressPercent.text = ""
                binding.generateProgressMessage.text = getString(R.string.generating_images)
                return@observe
            }
            binding.generateProgressBar.setProgressCompat(progress.percent, true)
            binding.generateProgressPercent.text = "${progress.percent}%"
            binding.generateProgressMessage.text = progress.message
        }
    }

    private fun render(state: CatalogueUiState) = with(binding) {
        latestStudioState = state
        generateLoadingOverlay.isVisible = state.isLoading
        generateBtn.isEnabled = !state.isLoading
        generateBtn.alpha = if (state.isLoading) 0.65f else 1f
        if (state.isLoading) showLoader() else hideLoader()
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
//        outfitTypeInput.setAdapter(
//            ArrayAdapter(
//                requireContext(),
//                R.layout.item_dropdown_option,
//                state.outfitTypeOptions
//            )
//        )

        catalogueForInput.setText(state.selectedCatalogueFor, false)
        categoryInput.setText(state.selectedCategory, false)
//        outfitTypeInput.setText(state.selectedOutfitType, false)
        val hasCategoryOptions = state.categoryOptions.isNotEmpty()
        categoryInput.isEnabled = hasCategoryOptions
        categoryInput.alpha = if (hasCategoryOptions) 1f else 0.65f
        if (!hasCategoryOptions && state.selectedCategory != DROPDOWN_PLACEHOLDER) {
            categoryInput.setText(DROPDOWN_PLACEHOLDER, false)
        }
//        businessLogoValue?.text = state.businessLogoName
        if (productCodeInput.text?.toString() != state.productCode) {
            productCodeInput.setText(state.productCode)
            productCodeInput.setSelection(state.productCode.length)
        }
        renderProductUploadCard()

        platformAdapter.submitList(state.platforms)
        aspectRatioAdapter.submitList(state.aspectRatios)
        bindResolutionInputs(
            width = state.resolutionWidth,
            height = state.resolutionHeight,
            editable = state.isResolutionEditable
        )
        renderPoseGarmentSection(state)
        renderCredits(state)
        val modelPreviewItems = state.modelRail.previewRailItems(STUDIO_RAIL_PREVIEW_LIMIT)
        val backgroundPreviewItems = state.backgroundRail.previewRailItems(STUDIO_RAIL_PREVIEW_LIMIT)
        val posePreviewItems = state.poseRail.previewRailItems(STUDIO_RAIL_PREVIEW_LIMIT)

        modelViewAll.isVisible = state.modelRail.visualItemCount() > STUDIO_RAIL_PREVIEW_LIMIT
        backgroundViewAll.isVisible = state.backgroundRail.visualItemCount() > STUDIO_RAIL_PREVIEW_LIMIT
        poseViewAll.isVisible = state.poseRail.visualItemCount() > STUDIO_RAIL_PREVIEW_LIMIT
        modelRecyclerView.isVisible = modelPreviewItems.isNotEmpty()
        backgroundRecyclerView.isVisible = backgroundPreviewItems.isNotEmpty()
        poseRecyclerView.isVisible = posePreviewItems.isNotEmpty()
        modelEmptyText.isVisible = !state.isLoading && modelPreviewItems.isEmpty()
        backgroundEmptyText.isVisible = !state.isLoading && backgroundPreviewItems.isEmpty()
        poseEmptyText.isVisible = !state.isLoading && posePreviewItems.isEmpty()

        submitRailItems(
            recyclerView = modelRecyclerView,
            adapter = modelAdapter,
            items = modelPreviewItems,
            previousIds = previousModelItemIds
        ) { ids ->
            previousModelItemIds = ids
        }
        submitRailItems(
            recyclerView = backgroundRecyclerView,
            adapter = backgroundAdapter,
            items = backgroundPreviewItems,
            previousIds = previousBackgroundItemIds
        ) { ids ->
            previousBackgroundItemIds = ids
        }
        submitRailItems(
            recyclerView = poseRecyclerView,
            adapter = poseAdapter,
            items = posePreviewItems,
            previousIds = previousPoseItemIds
        ) { ids ->
            previousPoseItemIds = ids
        }
    }

    private fun submitRailItems(
        recyclerView: RecyclerView,
        adapter: ImageRailAdapter,
        items: List<RailItemUiModel>,
        previousIds: List<String>,
        onIdsUpdated: (List<String>) -> Unit
    ) {
        val currentIds = items.map { it.id }
        val shouldResetPosition = previousIds.isNotEmpty() && previousIds != currentIds
        adapter.submitList(items) {
            if (shouldResetPosition && recyclerView.isAttachedToWindow) {
                recyclerView.scrollToPosition(0)
            }
        }
        onIdsUpdated(currentIds)
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
                latestStudioState?.let { renderPoseGarmentSection(it) }
            }
            UploadTarget.MODEL -> {
                viewModel.uploadPickedModel(uri, fileName)
            }
            UploadTarget.BACKGROUND -> {
                viewModel.uploadPickedBackground(uri, fileName)
            }
            UploadTarget.POSE_GARMENT -> {
                val poseId = pendingPoseOptionId ?: return
                poseGarmentSelections[poseId] = PoseGarmentSelection(
                    uploadedImageUri = uri,
                    uploadedFileName = fileName,
                    useSameGarment = false
                )
                latestStudioState?.let { renderPoseGarmentSection(it) }
            }
            UploadTarget.BUSINESS_LOGO -> {
                viewModel.updateBusinessLogoName(fileName)
            }
        }
        pendingPoseOptionId = null
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

    private fun renderCredits(state: CatalogueUiState) = with(binding) {
        val selectedPoseCount = state.poseRail.items
            .filterIsInstance<RailItemUiModel.Visual>()
            .count { it.isSelected }
        val totalCredits = selectedPoseCount * POSE_CREDIT_COST

        credits.text = totalCredits.toString()
        poseCountTv.text = " $selectedPoseCount "
    }

    private fun submitGenerateCatalogue() {
        val state = latestStudioState
        if (state == null) {
            showDummyMessage(getString(R.string.generate_error_studio_not_ready))
            return
        }

        val selectedPoses = state.poseRail.items
            .filterIsInstance<RailItemUiModel.Visual>()
            .filter { it.isSelected }
        if (selectedPoses.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.generate_error_pose_required), Toast.LENGTH_SHORT).show()
            return
        }

        val productUri = selectedProductImageUri
        if (productUri == null) {
            showDummyMessage(getString(R.string.generate_error_product_required))
            return
        }

        val selectedTheme = state.catalogueThemes.firstOrNull { theme ->
            theme.name.equals(state.selectedCatalogueFor, ignoreCase = true) ||
                theme.themeFor.equals(state.selectedCatalogueFor, ignoreCase = true)
        }
        val selectedSubcategory = state.garmentSubcategories.firstOrNull { subcategory ->
            subcategory.name.equals(state.selectedCategory, ignoreCase = true)
        }
        val selectedModel = state.modelRail.items
            .filterIsInstance<RailItemUiModel.Visual>()
            .firstOrNull { it.isSelected }
        val selectedBackground = state.backgroundRail.items
            .filterIsInstance<RailItemUiModel.Visual>()
            .firstOrNull { it.isSelected }

        when {
            selectedSubcategory == null -> showDummyMessage(getString(R.string.generate_error_category_required))
            selectedModel == null -> showDummyMessage(getString(R.string.generate_error_model_required))
            selectedBackground == null -> showDummyMessage(getString(R.string.generate_error_background_required))
            else -> {
                val poseIds = selectedPoses.map { it.id }
                val catalogueFor = selectedTheme?.themeFor
                    ?.takeIf { it.isNotBlank() }
                    ?: state.selectedCatalogueFor.trim().lowercase()
                val poseGarmentImages = poseIds.mapNotNull { poseId ->
                    val uploadedUri = poseGarmentSelections[poseId]?.uploadedImageUri
                    if (uploadedUri == null) null else poseId to uploadedUri
                }.toMap()
                val sameGarmentPoseIds = poseIds.filterNot { poseGarmentImages.containsKey(it) }

                findNavController().navigate(
                    R.id.generatedCatalogueFragment,
                    GeneratedCatalogueFragment.createGeneratingArgs(
                        title = state.productCode.ifBlank { state.selectedCategory }
                    )
                )
                viewModel.generateCatalogue(
                    GenerateCatalogueParams(
                        catalogueFor = catalogueFor,
                        dressType = selectedSubcategory.id,
                        modelId = selectedModel.id,
                        backgroundId = selectedBackground.id,
                        platform = state.platforms.firstOrNull { it.isSelected }?.id ?: DEFAULT_PLATFORM,
                        aspectRatio = state.aspectRatios.firstOrNull { it.isSelected }?.label ?: DEFAULT_ASPECT_RATIO,
                        width = state.resolutionWidth,
                        height = state.resolutionHeight,
                        poseIds = poseIds,
                        productImageUri = productUri,
                        poseGarmentImages = poseGarmentImages,
                        sameGarmentPoseIds = sameGarmentPoseIds,
                        title = state.productCode.ifBlank { state.selectedCategory }
                    )
                )
            }
        }
    }

    private fun renderPoseGarmentSection(state: CatalogueUiState) = with(binding) {
        val selectedPoses = state.poseRail.items
            .filterIsInstance<RailItemUiModel.Visual>()
            .filter { it.isSelected }

        val activePoseIds = selectedPoses.map { it.id }.toSet()
        poseGarmentSelections.keys.retainAll(activePoseIds)

        poseGarmentSection.visibility = if (selectedPoses.isEmpty()) View.GONE else View.VISIBLE
        if (selectedPoses.isEmpty()) {
            poseGarmentOptionsContainer.removeAllViews()
            return
        }

        poseGarmentOptionsContainer.removeAllViews()
        selectedPoses.forEach { pose ->
            poseGarmentOptionsContainer.addView(createPoseGarmentCard(pose))
        }
    }

    private fun createPoseGarmentCard(pose: RailItemUiModel.Visual): View {
        val context = requireContext()
        val selection = poseGarmentSelections[pose.id]
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen._12sdp)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen._12sdp)
        val spacingSmall = resources.getDimensionPixelSize(R.dimen._4sdp)
        val spacingMedium = resources.getDimensionPixelSize(R.dimen._8sdp)
        val spacingLarge = resources.getDimensionPixelSize(R.dimen._10sdp)
        val strokeWidth = resources.getDimensionPixelSize(R.dimen._1sdp)

        val titleText = pose.label.ifBlank { getString(R.string.pose_optional_default_title) }
        val subtitleText = getString(R.string.pose_optional_subtitle)
        val statusText = when {
            !selection?.uploadedFileName.isNullOrBlank() -> {
                getString(R.string.pose_optional_uploaded_status, selection?.uploadedFileName.orEmpty())
            }
            selection?.useSameGarment == true || selectedProductImageUri != null -> {
                getString(R.string.pose_optional_same_garment_status)
            }
            else -> getString(R.string.pose_optional_empty_status)
        }

        return MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = spacingLarge
            }
            radius = resources.getDimension(R.dimen.input_corner_radius)
            cardElevation = 0f
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            strokeColor = ContextCompat.getColor(context, R.color.strokeLight)
            this.strokeWidth = strokeWidth

            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

                    addView(
                        LinearLayout(context).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = android.view.Gravity.CENTER_VERTICAL

                            addView(
                                AppCompatTextView(context).apply {
                                    layoutParams = LinearLayout.LayoutParams(
                                        0,
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                        1f
                                    )
                                    text = titleText
                                    setTextColor(ContextCompat.getColor(context, R.color.black))
                                    textSize = 13f
                                    typeface = resources.getFont(R.font.satoshi_bold)
                                }
                            )

                            addView(
                                createPoseActionButton(
                                    label = getString(R.string.pose_optional_upload),
                                    usePrimaryStyle = false
                                ) {
                                    pendingImageTarget = UploadTarget.POSE_GARMENT
                                    pendingPoseOptionId = pose.id
                                    imagePickerManager.openPicker()
                                }
                            )

                            addView(
                                createPoseActionButton(
                                    label = getString(R.string.pose_optional_same_garment),
                                    usePrimaryStyle = selection?.useSameGarment == true
                                ) {
                                    poseGarmentSelections[pose.id] = PoseGarmentSelection(
                                        uploadedImageUri = null,
                                        uploadedFileName = null,
                                        useSameGarment = true
                                    )
                                    latestStudioState?.let { renderPoseGarmentSection(it) }
                                }
                            )
                        }
                    )

                    addView(
                        AppCompatTextView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                topMargin = spacingSmall
                            }
                            text = subtitleText
                            setTextColor(ContextCompat.getColor(context, R.color.textSecondary))
                            textSize = 12f
                            typeface = resources.getFont(R.font.satoshi_medium)
                        }
                    )

                    addView(
                        View(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                strokeWidth
                            ).apply {
                                topMargin = spacingMedium
                                bottomMargin = spacingMedium
                            }
                            backgroundTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(context, R.color.strokeLight)
                            )
                        }
                    )

                    addView(
                        AppCompatTextView(context).apply {
                            text = statusText
                            setTextColor(ContextCompat.getColor(context, R.color.textSecondary))
                            textSize = 11f
                            typeface = resources.getFont(R.font.satoshi_medium)
                        }
                    )
                }
            )
        }
    }

    private fun createPoseActionButton(
        label: String,
        usePrimaryStyle: Boolean,
        onClick: () -> Unit
    ): MaterialButton {
        val context = requireContext()
        val stroke = if (usePrimaryStyle) R.color.successText else R.color.primaryColor
        val textColorRes = if (usePrimaryStyle) R.color.successText else R.color.primaryColor
        val background = if (usePrimaryStyle) R.color.successSoft else R.color.white

        return MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                resources.getDimensionPixelSize(R.dimen._36sdp)
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen._8sdp)
            }
            minWidth = 0
            insetTop = 0
            insetBottom = 0
            textSize = 11f
            text = label
            isAllCaps = false
            cornerRadius = resources.getDimensionPixelSize(R.dimen._18sdp)
            strokeWidth = resources.getDimensionPixelSize(R.dimen._1sdp)
            strokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, stroke))
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, background))
            setTextColor(ContextCompat.getColor(context, textColorRes))
            typeface = resources.getFont(R.font.satoshi_bold)
            setOnClickListener { onClick() }
        }
    }

    private fun bindResolutionInputs(width: String, height: String, editable: Boolean) = with(binding) {
        isUpdatingResolutionInputs = true
        resolutionWidthInput.updateTextIfNeeded(width)
        resolutionHeightInput.updateTextIfNeeded(height)
        isUpdatingResolutionInputs = false

        resolutionWidthInput.updateEditableState(editable)
        resolutionHeightInput.updateEditableState(editable)
    }

    private fun TextInputEditText.updateTextIfNeeded(value: String) {
        if (text?.toString() == value) return
        setText(value)
        setSelection(value.length)
    }

    private fun TextInputEditText.updateEditableState(editable: Boolean) {
        if (!editable) {
            clearFocus()
        }
        isFocusable = editable
        isFocusableInTouchMode = editable
        isCursorVisible = editable
        isLongClickable = editable
        showSoftInputOnFocus = editable
    }

    private fun showModelBottomSheet() {
        val existingSheet = childFragmentManager.findFragmentByTag(ModelSelectionBottomSheet.Companion.TAG)
        if (existingSheet == null) {
            ModelSelectionBottomSheet().show(childFragmentManager, ModelSelectionBottomSheet.Companion.TAG)
        }
    }

    private fun showVisualBottomSheet(type: VisualSelectionBottomSheet.VisualRailType) {
        val visualCount = latestStudioState?.let { state ->
            when (type) {
                VisualSelectionBottomSheet.VisualRailType.BACKGROUND -> state.backgroundRail.visualItemCount()
                VisualSelectionBottomSheet.VisualRailType.POSE -> state.poseRail.visualItemCount()
            }
        } ?: 0

        if (visualCount == 0) return

        val tag = VisualSelectionBottomSheet.tagFor(type)
        val existingSheet = childFragmentManager.findFragmentByTag(tag)
        if (existingSheet == null) {
            VisualSelectionBottomSheet.newInstance(type).show(childFragmentManager, tag)
        }
    }

    private fun RailSectionUiModel.previewRailItems(limit: Int): List<RailItemUiModel> {
        return items.filterIsInstance<RailItemUiModel.Visual>().take(limit)
    }

    private fun RailSectionUiModel.visualItemCount(): Int {
        return items.count { it is RailItemUiModel.Visual }
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
        POSE_GARMENT,
        BUSINESS_LOGO
    }

    companion object {
        const val POSE_CREDIT_COST = 25
        const val STUDIO_RAIL_PREVIEW_LIMIT = 5
        const val DEFAULT_PLATFORM = "amazon"
        const val DEFAULT_ASPECT_RATIO = "1:1"
        const val GENERATION_ERROR_RESULT_KEY = "generation_error_result"
    }

    private data class PoseGarmentSelection(
        val uploadedImageUri: Uri? = null,
        val uploadedFileName: String? = null,
        val useSameGarment: Boolean = false
    )
}
