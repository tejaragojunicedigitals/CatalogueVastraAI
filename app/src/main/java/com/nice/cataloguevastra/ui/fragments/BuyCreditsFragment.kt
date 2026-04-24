package com.nice.cataloguevastra.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.nice.cataloguevastra.CatalogueVastraApp
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.FragmentBuyCreditsBinding
import java.text.NumberFormat
import java.util.Locale

class BuyCreditsFragment : Fragment() {

    private var _binding: FragmentBuyCreditsBinding? = null
    private val binding get() = _binding!!

    private val sessionManager by lazy {
        (requireActivity().application as CatalogueVastraApp).appContainer.sessionManager
    }

    private val currentCredits: Int
        get() = sessionManager.getCreditsBalance()

    private val requiredCredits: Int
        get() = arguments?.getInt(ARG_REQUIRED_CREDITS, 0) ?: 0

    private val selectedPoseCount: Int
        get() = arguments?.getInt(ARG_SELECTED_POSE_COUNT, 0) ?: 0

    private val creditsShortfall: Int
        get() = (requiredCredits - currentCredits).coerceAtLeast(0)

    private val packs = listOf(
        CreditPack("starter", getString(R.string.buy_credits_pack_starter), 2500, 2500),
        CreditPack("growth", getString(R.string.buy_credits_pack_growth), 5000, 5000),
        CreditPack("pro", getString(R.string.buy_credits_pack_pro), 10000, 10000)
    )

    private var selectedPack: CreditPack? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBuyCreditsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectedPack = packs.firstOrNull { it.credits >= creditsShortfall } ?: packs.last()
        setupStaticContent()
        setupPackSelection()
        setupActions()
        renderSelectedPack()
    }

    private fun setupStaticContent() = with(binding) {
        balanceValueText.text = getString(
            R.string.buy_credits_balance_format,
            formatNumber(currentCredits)
        )
        generationNeedText.text = if (selectedPoseCount > 0) {
            getString(
                R.string.buy_credits_generation_need_with_poses,
                formatNumber(requiredCredits),
                formatNumber(selectedPoseCount)
            )
        } else {
            getString(
                R.string.buy_credits_generation_need,
                formatNumber(requiredCredits)
            )
        }
        purchaseHintText.text = if (creditsShortfall > 0) {
            getString(
                R.string.buy_credits_shortfall_message,
                formatNumber(creditsShortfall)
            )
        } else {
            getString(R.string.buy_credits_direct_purchase_message)
        }

        starterPackTitle.text = packs[0].title
        starterPackMeta.text = getString(
            R.string.buy_credits_pack_meta,
            formatNumber(packs[0].credits),
            formatCurrency(packs[0].priceInr)
        )
        growthPackTitle.text = packs[1].title
        growthPackMeta.text = getString(
            R.string.buy_credits_pack_meta,
            formatNumber(packs[1].credits),
            formatCurrency(packs[1].priceInr)
        )
        proPackTitle.text = packs[2].title
        proPackMeta.text = getString(
            R.string.buy_credits_pack_meta,
            formatNumber(packs[2].credits),
            formatCurrency(packs[2].priceInr)
        )
    }

    private fun setupPackSelection() = with(binding) {
        starterPackCard.setOnClickListener { updateSelectedPack(packs[0]) }
        growthPackCard.setOnClickListener { updateSelectedPack(packs[1]) }
        proPackCard.setOnClickListener { updateSelectedPack(packs[2]) }
    }

    private fun setupActions() = with(binding) {
        resetPackButton.setOnClickListener {
            updateSelectedPack(packs.firstOrNull { it.credits >= creditsShortfall } ?: packs.last())
        }
        saveDetailsButton.setOnClickListener {
            Snackbar.make(root, getString(R.string.buy_credits_save_details_message), Snackbar.LENGTH_SHORT).show()
        }
        payNowButton.setOnClickListener {
            val pack = selectedPack ?: return@setOnClickListener
            Snackbar.make(
                root,
                getString(R.string.buy_credits_pay_message, formatCurrency(pack.priceInr)),
                Snackbar.LENGTH_SHORT
            ).show()
        }
        requestInvoiceButton.setOnClickListener {
            Snackbar.make(root, getString(R.string.buy_credits_invoice_message), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun updateSelectedPack(pack: CreditPack) {
        selectedPack = pack
        renderSelectedPack()
    }

    private fun renderSelectedPack() = with(binding) {
        val pack = selectedPack ?: return@with
        updatePackCard(starterPackCard, pack.id == packs[0].id)
        updatePackCard(growthPackCard, pack.id == packs[1].id)
        updatePackCard(proPackCard, pack.id == packs[2].id)

        selectedPackNameValue.text = pack.title
        selectedCreditsValue.text = getString(
            R.string.buy_credits_summary_credits_format,
            formatNumber(pack.credits)
        )
        totalAmountValue.text = formatCurrency(pack.priceInr)
        topUpImpactValue.text = getString(
            R.string.buy_credits_post_topup_format,
            formatNumber(currentCredits + pack.credits)
        )
        payNowButton.text = getString(
            R.string.buy_credits_pay_now_format,
            formatCurrency(pack.priceInr)
        )
    }

    private fun updatePackCard(card: MaterialCardView, isSelected: Boolean) {
        val context = requireContext()
        card.strokeColor = ContextCompat.getColor(
            context,
            if (isSelected) R.color.primaryColor else R.color.strokeLight
        )
        card.setCardBackgroundColor(
            ContextCompat.getColor(
                context,
                if (isSelected) R.color.primaryContainer else R.color.white
            )
        )
        card.strokeWidth = resources.getDimensionPixelSize(
            if (isSelected) R.dimen._2sdp else R.dimen._1sdp
        )
    }

    private fun formatNumber(value: Int): String {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("en-IN")).format(value)
    }

    private fun formatCurrency(value: Int): String {
        return "\u20B9${formatNumber(value)}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class CreditPack(
        val id: String,
        val title: String,
        val credits: Int,
        val priceInr: Int
    )

    companion object {
        private const val ARG_REQUIRED_CREDITS = "required_credits"
        private const val ARG_SELECTED_POSE_COUNT = "selected_pose_count"

        fun createArgs(requiredCredits: Int, selectedPoseCount: Int): Bundle {
            return Bundle().apply {
                putInt(ARG_REQUIRED_CREDITS, requiredCredits)
                putInt(ARG_SELECTED_POSE_COUNT, selectedPoseCount)
            }
        }
    }
}
