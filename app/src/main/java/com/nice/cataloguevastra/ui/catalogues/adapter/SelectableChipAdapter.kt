package com.nice.cataloguevastra.ui.catalogues.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.ItemChipOptionBinding
import com.nice.cataloguevastra.ui.catalogues.model.ChipUiModel

class SelectableChipAdapter(
    private val onChipClicked: (ChipUiModel) -> Unit
) : ListAdapter<ChipUiModel, SelectableChipAdapter.ChipViewHolder>(ChipDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val binding = ItemChipOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChipViewHolder(binding, onChipClicked)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChipViewHolder(
        private val binding: ItemChipOptionBinding,
        private val onChipClicked: (ChipUiModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChipUiModel) = with(binding) {
            chipLabel.text = item.label
            root.updateSelectedStyle(item.isSelected)
            root.setOnClickListener { onChipClicked(item) }
        }

        private fun MaterialCardView.updateSelectedStyle(isSelected: Boolean) {
            val context = context
            val background = if (isSelected) {
                ContextCompat.getColor(context, R.color.primaryContainer)
            } else {
                ContextCompat.getColor(context, R.color.white)
            }
            val stroke = if (isSelected) {
                ContextCompat.getColor(context, R.color.primaryColor)
            } else {
                ContextCompat.getColor(context, R.color.strokeLight)
            }
            val textColor = if (isSelected) {
                ContextCompat.getColor(context, R.color.primaryColor)
            } else {
                ContextCompat.getColor(context, R.color.textSecondary)
            }

            setCardBackgroundColor(background)
            strokeColor = stroke
            strokeWidth = context.resources.getDimensionPixelSize(R.dimen._1sdp)
            binding.chipLabel.setTextColor(textColor)
        }
    }

    private object ChipDiffCallback : DiffUtil.ItemCallback<ChipUiModel>() {
        override fun areItemsTheSame(oldItem: ChipUiModel, newItem: ChipUiModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChipUiModel, newItem: ChipUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
