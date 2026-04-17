package com.nice.cataloguevastra.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.FragmentPlaceholderBinding

class PlaceholderFragment : Fragment() {

    private var _binding: FragmentPlaceholderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceholderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val content = when (findNavController().currentDestination?.id) {
            R.id.studioFragment -> PlaceholderContent(
                getString(R.string.placeholder_studio_title),
                getString(R.string.placeholder_studio_desc)
            )

            R.id.assetsFragment -> PlaceholderContent(
                getString(R.string.placeholder_assets_title),
                getString(R.string.placeholder_assets_desc)
            )

            R.id.cataloguesFragment -> PlaceholderContent(
                getString(R.string.placeholder_catalogues_title),
                getString(R.string.placeholder_catalogues_desc)
            )

            R.id.pricingFragment -> PlaceholderContent(
                getString(R.string.placeholder_pricing_title),
                getString(R.string.placeholder_pricing_desc)
            )

            R.id.accountFragment -> PlaceholderContent(
                getString(R.string.placeholder_account_title),
                getString(R.string.placeholder_account_desc)
            )

            else -> PlaceholderContent("", "")
        }

        binding.placeholderTitle.text = content.title
        binding.placeholderDescription.text = content.description
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class PlaceholderContent(
        val title: String,
        val description: String
    )
}
