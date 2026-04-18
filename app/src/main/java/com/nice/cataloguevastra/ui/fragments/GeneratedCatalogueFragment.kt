package com.nice.cataloguevastra.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.FragmentGeneratedCatalogueBinding

class GeneratedCatalogueFragment : Fragment() {

    private var _binding: FragmentGeneratedCatalogueBinding? = null
    private val binding get() = _binding!!
    private lateinit var thumbnailAdapter: ThumbnailAdapter

    private val previewImages = listOf(
        R.drawable.model,
        R.drawable.model,
        R.drawable.model
    )

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
        setupThumbnails()
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.downloadAllButton.setOnClickListener {
            showDummyMessage(getString(R.string.download_all_message))
        }
        binding.downloadOneButton.setOnClickListener {
            showDummyMessage(getString(R.string.download_image_message))
        }
        binding.expandButton.setOnClickListener {
            showDummyMessage(getString(R.string.expand_preview_message))
        }
    }

    private fun setupThumbnails() = with(binding) {
        thumbnailAdapter = ThumbnailAdapter(previewImages) { position ->
            mainPreviewImage.setImageResource(previewImages[position])
        }
        thumbnailRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        thumbnailRecyclerView.adapter = thumbnailAdapter
        mainPreviewImage.setImageResource(previewImages.first())
    }

    private fun showDummyMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
