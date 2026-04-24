package com.nice.cataloguevastra.ui.fragments

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.snackbar.Snackbar
import com.nice.cataloguevastra.CatalogueVastraApp
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.FragmentGeneratedCatalogueBinding
import com.nice.cataloguevastra.viewmodel.CataloguesViewModel

class GeneratedCatalogueFragment : Fragment() {

    private var _binding: FragmentGeneratedCatalogueBinding? = null
    private val binding get() = _binding!!
    private lateinit var thumbnailAdapter: ThumbnailAdapter
    private var previewImages: List<PreviewImageUiModel> = emptyList()
    private var selectedIndex = 0
    private var isWaitingForGeneration = false

    private val viewModel: CataloguesViewModel by activityViewModels {
        CataloguesViewModel.factory(
            (requireActivity().application as CatalogueVastraApp).appContainer.catalogueRepository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGeneratedCatalogueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        previewImages = resolvePreviewImages()
        isWaitingForGeneration = arguments?.getBoolean(ARG_WAIT_FOR_RESULT, false) ?: false
        binding.titleText.text = arguments?.getString(ARG_TITLE).orEmpty()
            .ifBlank { getString(R.string.generated_catalogue_title) }
        setupThumbnails()
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.downloadAllButton.setOnClickListener {
            downloadAllImages()
        }
        binding.downloadOneButton.setOnClickListener {
            downloadCurrentImage()
        }
        binding.expandButton.setOnClickListener {
            openExpandedView()
        }
        binding.previousButton.setOnClickListener { showPreviousImage() }
        binding.nextButton.setOnClickListener { showNextImage() }
        observeGenerationProgress()
        observeGeneratedCatalogue()
        observeGenerateError()
        updatePreviewContainerHeight()
        updateViewerUi()
    }

    private fun setupThumbnails() = with(binding) {
        thumbnailAdapter = ThumbnailAdapter(previewImages) { position ->
            selectedIndex = position
            updateViewerUi()
        }
        thumbnailRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        thumbnailRecyclerView.adapter = thumbnailAdapter
        thumbnailRecyclerView.isVisible = shouldShowThumbnails() && previewImages.size > 1
    }

    private fun resolvePreviewImages(): List<PreviewImageUiModel> {
        val imageUrls = arguments?.getStringArrayList(ARG_IMAGE_URLS)
            ?.mapNotNull { it.trim().takeIf(String::isNotBlank) }
            .orEmpty()

        if (imageUrls.isNotEmpty()) {
            return imageUrls.map { PreviewImageUiModel(imageUrl = it, imageRes = R.drawable.ic_gallery) }
        }

        if (arguments?.getBoolean(ARG_WAIT_FOR_RESULT, false) == true) {
            return emptyList()
        }

        return listOf(
            PreviewImageUiModel(imageRes = R.drawable.model),
            PreviewImageUiModel(imageRes = R.drawable.model),
            PreviewImageUiModel(imageRes = R.drawable.model)
        )
    }

    private fun updateViewerUi() = with(binding) {
        val currentImage = previewImages.getOrNull(selectedIndex)
        if (currentImage == null) {
            mainPreviewImage.setImageDrawable(null)
            previousButton.isVisible = false
            nextButton.isVisible = false
            downloadAllButton.isVisible = false
            downloadOneButton.isVisible = false
            expandButton.isVisible = false
            thumbnailRecyclerView.isVisible = false
            imageCountChip.isVisible = false
            generationProgressOverlay.isVisible = isWaitingForGeneration
            return
        }

        generationProgressOverlay.isVisible = false
        downloadOneButton.isVisible = true
        expandButton.isVisible = true
        if (!currentImage.imageUrl.isNullOrBlank()) {
            mainPreviewImage.setImageDrawable(null)
            mainPreviewImage.load(currentImage.imageUrl) {
                error(currentImage.imageRes ?: R.drawable.ic_gallery)
                crossfade(false)
            }
        } else {
            mainPreviewImage.setImageResource(currentImage.imageRes ?: R.drawable.ic_gallery)
        }

        thumbnailAdapter.updateSelectedPosition(selectedIndex)

        val hasMultipleImages = previewImages.size > 1
        previousButton.isVisible = hasMultipleImages
        nextButton.isVisible = hasMultipleImages
        downloadAllButton.isVisible = hasMultipleImages
        thumbnailRecyclerView.isVisible = shouldShowThumbnails() && hasMultipleImages
        imageCountChip.isVisible = hasMultipleImages
        imageCountText.text = getString(
            R.string.generated_image_count,
            selectedIndex + 1,
            previewImages.size
        )
    }

    private fun observeGenerationProgress() {
        viewModel.generateProgress.observe(viewLifecycleOwner) { progress ->
            if (!isWaitingForGeneration) return@observe
            binding.generationProgressOverlay.isVisible = true
            if (progress == null) {
                binding.generationProgressBar.progress = 0
                binding.generationProgressPercent.text = "0%"
                binding.generationProgressMessage.text = getString(R.string.generating_images)
            } else {
                binding.generationProgressBar.setProgressCompat(progress.percent, true)
                binding.generationProgressPercent.text = "${progress.percent}%"
                binding.generationProgressMessage.text = progress.message
            }
        }
    }

    private fun observeGeneratedCatalogue() {
        viewModel.generatedCatalogue.observe(viewLifecycleOwner) { result ->
            if (result == null || !isWaitingForGeneration) return@observe
            isWaitingForGeneration = false
            previewImages = result.imageUrls.map { imageUrl ->
                PreviewImageUiModel(imageUrl = imageUrl, imageRes = R.drawable.ic_gallery)
            }
            selectedIndex = 0
            binding.titleText.text = result.title.ifBlank { getString(R.string.generated_catalogue_title) }
            thumbnailAdapter.updateImages(previewImages)
            updatePreviewContainerHeight()
            updateViewerUi()
            viewModel.consumeGeneratedCatalogue()
        }
    }

    private fun observeGenerateError() {
        viewModel.generateError.observe(viewLifecycleOwner) { message ->
            if (message.isNullOrBlank() || !isWaitingForGeneration) return@observe
            findNavController().previousBackStackEntry?.savedStateHandle?.set(
                StudioFragment.GENERATION_ERROR_RESULT_KEY,
                message
            )
            viewModel.consumeMessage()
            viewModel.consumeGenerateError()
            findNavController().navigateUp()
        }
    }

    private fun updatePreviewContainerHeight() {
        val screenHeightDp = resources.configuration.screenHeightDp
        val screenWidthDp = resources.configuration.screenWidthDp
        val isTablet = screenWidthDp >= 600
        val showingThumbnails = shouldShowThumbnails() && previewImages.size > 1

        val targetHeightDp = when {
            isTablet && showingThumbnails -> (screenHeightDp * 0.6f).toInt()
            isTablet -> (screenHeightDp * 0.7f).toInt()
            showingThumbnails -> (screenHeightDp * 0.44f).toInt()
            else -> (screenHeightDp * 0.52f).toInt()
        }.coerceIn(
            if (isTablet) 420 else 260,
            if (isTablet) 720 else 460
        )

        binding.previewContainer.layoutParams = binding.previewContainer.layoutParams.apply {
            height = (targetHeightDp * resources.displayMetrics.density).toInt()
        }
    }

    private fun showPreviousImage() {
        if (previewImages.size <= 1) return
        selectedIndex = if (selectedIndex == 0) previewImages.lastIndex else selectedIndex - 1
        updateViewerUi()
    }

    private fun showNextImage() {
        if (previewImages.size <= 1) return
        selectedIndex = if (selectedIndex == previewImages.lastIndex) 0 else selectedIndex + 1
        updateViewerUi()
    }

    private fun openExpandedView() {
        val imageUrls = previewImages.mapNotNull { it.imageUrl?.takeIf(String::isNotBlank) }
        if (imageUrls.isEmpty()) {
            showDummyMessage(getString(R.string.generated_expand_no_images))
            return
        }
        ExpandedImageDialogFragment
            .newInstance(ArrayList(imageUrls), selectedIndex.coerceIn(0, imageUrls.lastIndex))
            .show(childFragmentManager, ExpandedImageDialogFragment.TAG)
    }

    private fun downloadCurrentImage() {
        val imageUrl = previewImages.getOrNull(selectedIndex)?.imageUrl.orEmpty()
        if (imageUrl.isBlank()) {
            showDummyMessage(getString(R.string.generated_download_no_images))
            return
        }

        enqueueDownload(imageUrl, buildDownloadTitle(selectedIndex))
        showDummyMessage(getString(R.string.generated_download_started, 1))
    }

    private fun downloadAllImages() {
        val imageUrls = previewImages.mapNotNull { it.imageUrl?.takeIf(String::isNotBlank) }
        if (imageUrls.isEmpty()) {
            showDummyMessage(getString(R.string.generated_download_no_images))
            return
        }

        imageUrls.forEachIndexed { index, imageUrl ->
            enqueueDownload(imageUrl, buildDownloadTitle(index))
        }
        showDummyMessage(getString(R.string.generated_download_started, imageUrls.size))
    }

    private fun enqueueDownload(imageUrl: String, title: String) {
        val request = DownloadManager.Request(Uri.parse(imageUrl))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setTitle(title)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "$title.jpg"
            )

        val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }

    private fun buildDownloadTitle(index: Int): String {
        val baseTitle = binding.titleText.text?.toString().orEmpty().ifBlank { "generated_catalogue" }
        val safeTitle = baseTitle
            .replace("[^A-Za-z0-9_-]+".toRegex(), "_")
            .trim('_')
            .ifBlank { "generated_catalogue" }
        return "${safeTitle}_${index + 1}"
    }

    private fun shouldShowThumbnails(): Boolean {
        return arguments?.getBoolean(ARG_SHOW_THUMBNAILS, true) ?: true
    }

    private fun showDummyMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_IMAGE_URLS = "arg_image_urls"
        private const val ARG_SHOW_THUMBNAILS = "arg_show_thumbnails"
        private const val ARG_WAIT_FOR_RESULT = "arg_wait_for_result"

        fun createArgs(
            title: String,
            imageUrls: ArrayList<String>,
            showThumbnails: Boolean
        ): Bundle {
            return Bundle().apply {
                putString(ARG_TITLE, title)
                putStringArrayList(ARG_IMAGE_URLS, imageUrls)
                putBoolean(ARG_SHOW_THUMBNAILS, showThumbnails)
            }
        }

        fun createGeneratingArgs(title: String): Bundle {
            return Bundle().apply {
                putString(ARG_TITLE, title.ifBlank { "Generating catalogue" })
                putStringArrayList(ARG_IMAGE_URLS, arrayListOf())
                putBoolean(ARG_SHOW_THUMBNAILS, true)
                putBoolean(ARG_WAIT_FOR_RESULT, true)
            }
        }
    }
}
