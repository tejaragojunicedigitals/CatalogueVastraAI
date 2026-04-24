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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.snackbar.Snackbar
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.FragmentAssetPreviewBinding

class AssetPreviewFragment : Fragment() {

    private var _binding: FragmentAssetPreviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var thumbnailAdapter: ThumbnailAdapter
    private var previewImages: List<PreviewImageUiModel> = emptyList()
    private var selectedIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssetPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        previewImages = resolvePreviewImages()
        binding.titleText.text = arguments?.getString(ARG_TITLE).orEmpty()
            .ifBlank { getString(R.string.generated_catalogue_title) }
        setupThumbnails()
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.previousButton.setOnClickListener { showPreviousImage() }
        binding.nextButton.setOnClickListener { showNextImage() }
        binding.expandButton.setOnClickListener { openExpandedView() }
        binding.downloadOneButton.setOnClickListener { downloadCurrentImage() }
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

        return listOf(PreviewImageUiModel(imageRes = R.drawable.ic_gallery))
    }

    private fun updateViewerUi() = with(binding) {
        val currentImage = previewImages.getOrNull(selectedIndex) ?: return
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
        thumbnailRecyclerView.isVisible = shouldShowThumbnails() && hasMultipleImages
        imageCountChip.isVisible = hasMultipleImages
        imageCountText.text = getString(
            R.string.generated_image_count,
            selectedIndex + 1,
            previewImages.size
        )
    }

    private fun updatePreviewContainerHeight() {
        val screenHeightDp = resources.configuration.screenHeightDp
        val screenWidthDp = resources.configuration.screenWidthDp
        val isTablet = screenWidthDp >= 600
        val showingThumbnails = shouldShowThumbnails() && previewImages.size > 1

        val targetHeightDp = when {
            isTablet && showingThumbnails -> (screenHeightDp * 0.6f).toInt()
            isTablet -> (screenHeightDp * 0.72f).toInt()
            showingThumbnails -> (screenHeightDp * 0.46f).toInt()
            else -> (screenHeightDp * 0.56f).toInt()
        }.coerceIn(
            if (isTablet) 420 else 260,
            if (isTablet) 760 else 500
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

    private fun shouldShowThumbnails(): Boolean {
        return arguments?.getBoolean(ARG_SHOW_THUMBNAILS, true) ?: true
    }

    private fun openExpandedView() {
        val imageUrls = previewImages.mapNotNull { it.imageUrl?.takeIf(String::isNotBlank) }
        if (imageUrls.isEmpty()) return
        ExpandedImageDialogFragment
            .newInstance(ArrayList(imageUrls), selectedIndex.coerceIn(0, imageUrls.lastIndex))
            .show(childFragmentManager, ExpandedImageDialogFragment.TAG)
    }

    private fun downloadCurrentImage() {
        val imageUrl = previewImages.getOrNull(selectedIndex)?.imageUrl.orEmpty()
        if (imageUrl.isBlank()) {
            Snackbar.make(binding.root, getString(R.string.download_image_message), Snackbar.LENGTH_SHORT).show()
            return
        }

        val request = DownloadManager.Request(Uri.parse(imageUrl))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setTitle(buildDownloadTitle())
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "${buildDownloadTitle()}.jpg"
            )

        val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        Snackbar.make(binding.root, getString(R.string.download_image_message), Snackbar.LENGTH_SHORT).show()
    }

    private fun buildDownloadTitle(): String {
        val baseTitle = binding.titleText.text?.toString().orEmpty().ifBlank { "asset_image" }
        return "${baseTitle.replace("\\s+".toRegex(), "_")}_${selectedIndex + 1}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_IMAGE_URLS = "arg_image_urls"
        private const val ARG_SHOW_THUMBNAILS = "arg_show_thumbnails"
        const val ARG_SOURCE = "arg_source"
        const val SOURCE_ASSETS = "assets"
        const val SOURCE_CATALOGUES = "catalogues"

        fun createArgs(
            title: String,
            imageUrls: ArrayList<String>,
            showThumbnails: Boolean,
            source: String
        ): Bundle {
            return Bundle().apply {
                putString(ARG_TITLE, title)
                putStringArrayList(ARG_IMAGE_URLS, imageUrls)
                putBoolean(ARG_SHOW_THUMBNAILS, showThumbnails)
                putString(ARG_SOURCE, source)
            }
        }
    }
}
