package com.nice.cataloguevastra.ui.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import coil.load
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.DialogExpandedImageBinding

class ExpandedImageDialogFragment : DialogFragment() {

    private var _binding: DialogExpandedImageBinding? = null
    private val binding get() = _binding!!
    private var imageUrls: List<String> = emptyList()
    private var selectedIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogExpandedImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imageUrls = arguments?.getStringArrayList(ARG_IMAGE_URLS)
            ?.mapNotNull { it.trim().takeIf(String::isNotBlank) }
            .orEmpty()
        selectedIndex = arguments?.getInt(ARG_SELECTED_INDEX, 0)
            ?.coerceIn(0, (imageUrls.size - 1).coerceAtLeast(0))
            ?: 0
        binding.closeButton.setOnClickListener { dismiss() }
        binding.previousButton.setOnClickListener { showPreviousImage() }
        binding.nextButton.setOnClickListener { showNextImage() }
        renderCurrentImage()
    }

    private fun renderCurrentImage() = with(binding) {
        val imageUrl = imageUrls.getOrNull(selectedIndex).orEmpty()
        expandedImage.load(imageUrl) {
            error(R.drawable.model_img)
            crossfade(false)
        }
        val hasMultipleImages = imageUrls.size > 1
        previousButton.isVisible = hasMultipleImages
        nextButton.isVisible = hasMultipleImages
    }

    private fun showPreviousImage() {
        if (imageUrls.size <= 1) return
        selectedIndex = if (selectedIndex == 0) imageUrls.lastIndex else selectedIndex - 1
        renderCurrentImage()
    }

    private fun showNextImage() {
        if (imageUrls.size <= 1) return
        selectedIndex = if (selectedIndex == imageUrls.lastIndex) 0 else selectedIndex + 1
        renderCurrentImage()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_IMAGE_URLS = "arg_image_urls"
        private const val ARG_SELECTED_INDEX = "arg_selected_index"
        const val TAG = "ExpandedImageDialogFragment"

        fun newInstance(imageUrls: ArrayList<String>, selectedIndex: Int): ExpandedImageDialogFragment {
            return ExpandedImageDialogFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_IMAGE_URLS, imageUrls)
                    putInt(ARG_SELECTED_INDEX, selectedIndex)
                }
            }
        }
    }
}
