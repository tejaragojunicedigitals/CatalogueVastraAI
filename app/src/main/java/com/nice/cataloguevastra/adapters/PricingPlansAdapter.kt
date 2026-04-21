package com.nice.cataloguevastra.adapters

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.ItemPricingPlanBinding
import com.nice.cataloguevastra.model.PricingPlanUiModel

class PricingPlansAdapter(
    private val onActionClick: (PricingPlanUiModel) -> Unit,
    private val onToggleDetailsClick: (PricingPlanUiModel) -> Unit
) : ListAdapter<PricingPlanUiModel, PricingPlansAdapter.PricingPlanViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PricingPlanViewHolder {
        val binding = ItemPricingPlanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PricingPlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PricingPlanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PricingPlanViewHolder(
        private val binding: ItemPricingPlanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PricingPlanUiModel) = with(binding) {
            val context = root.context
            val isHighlighted = item.isHighlighted

            planTitle.text = item.title
            planSubtitle.text = item.subtitle
            priceText.text = item.priceLabel
            creditsText.text = item.creditsLabel
            actionButton.text = item.actionLabel
            detailsToggleButton.text = context.getString(
                if (item.isExpanded) R.string.pricing_hide_details else R.string.pricing_view_details
            )
            detailsContainer.isVisible = item.isExpanded

            planBadge.text = item.badgeLabel
            planBadge.visibility = if (item.badgeLabel.isNullOrBlank()) View.GONE else View.VISIBLE

            headerContainer.background = ContextCompat.getDrawable(
                context,
                if (isHighlighted) R.drawable.bg_pricing_header_highlight
                else R.drawable.bg_pricing_header_soft
            )

            val headerTitleColor = ContextCompat.getColor(
                context,
                if (isHighlighted) R.color.white else R.color.textPrimary
            )
            val headerBodyColor = ContextCompat.getColor(
                context,
                if (isHighlighted) R.color.white else R.color.textSecondary
            )
            val cardStrokeColor = ContextCompat.getColor(
                context,
                if (isHighlighted) R.color.primaryColor else R.color.strokeLight
            )

            planCard.strokeColor = cardStrokeColor
            planCard.strokeWidth = context.resources.getDimensionPixelSize(
                if (isHighlighted) R.dimen._2sdp else R.dimen._1sdp
            )

            planTitle.setTextColor(headerTitleColor)
            planSubtitle.setTextColor(headerBodyColor)
            priceText.setTextColor(headerTitleColor)
            creditsText.setTextColor(headerBodyColor)

            actionButton.applyHighlightState(isHighlighted)
            detailsToggleButton.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isHighlighted) R.color.white else R.color.bottomNavInactive
                )
            )

            bindItems(
                quickHighlightsContainer,
                item.quickHighlights,
                compact = true,
                textColorRes = if (isHighlighted) R.color.white else R.color.textPrimary,
                iconColorRes = if (isHighlighted) R.color.white else R.color.primaryColor
            )
            bindItems(snapshotContainer, item.snapshotItems)
            bindItems(outputContainer, item.outputItems)
            bindItems(featuresContainer, item.featureItems)

            actionButton.setOnClickListener { onActionClick(item) }
            detailsToggleButton.setOnClickListener { onToggleDetailsClick(item) }
        }

        private fun bindItems(
            container: LinearLayout,
            items: List<String>,
            compact: Boolean = false,
            textColorRes: Int = R.color.ash,
            iconColorRes: Int = R.color.primaryColor
        ) {
            val context = binding.root.context
            val horizontalPadding = context.resources.getDimensionPixelSize(
                if (compact) R.dimen._4sdp else R.dimen._12sdp
            )
            val verticalPadding = context.resources.getDimensionPixelSize(
                if (compact) R.dimen._6sdp else R.dimen._8sdp
            )

            container.removeAllViews()
            items.forEach { text ->
                val itemView = LayoutInflater.from(context)
                    .inflate(R.layout.item_pricing_bullet, container, false) as TextView
                itemView.text = text
                itemView.maxLines = if (compact) 1 else Int.MAX_VALUE
                itemView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_pricing_check,
                    0,
                    0,
                    0
                )
                itemView.compoundDrawablePadding =
                    context.resources.getDimensionPixelSize(R.dimen._8sdp)
                itemView.setPaddingRelative(horizontalPadding, verticalPadding, 0, 0)
                itemView.setTextColor(ContextCompat.getColor(context, textColorRes))
                itemView.compoundDrawablesRelative.forEach { drawable ->
                    drawable?.mutate()
                    drawable?.let {
                        DrawableCompat.setTint(
                            it,
                            ContextCompat.getColor(context, iconColorRes)
                        )
                    }
                }
                container.addView(itemView)
            }
        }

        private fun MaterialButton.applyHighlightState(isHighlighted: Boolean) {
            val context = this.context
            val fillDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = context.resources.getDimension(R.dimen.input_corner_radius)
                setColor(ContextCompat.getColor(context, R.color.white))
                if (!isHighlighted) {
                    setStroke(
                        context.resources.getDimensionPixelSize(R.dimen._1sdp),
                        ContextCompat.getColor(context, R.color.grey)
                    )
                }
            }
            background = fillDrawable
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isHighlighted) R.color.primaryColor else R.color.bottomNavInactive
                )
            )
            strokeWidth = 0
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PricingPlanUiModel>() {
        override fun areItemsTheSame(
            oldItem: PricingPlanUiModel,
            newItem: PricingPlanUiModel
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: PricingPlanUiModel,
            newItem: PricingPlanUiModel
        ): Boolean = oldItem == newItem
    }
}