package com.nice.cataloguevastra.ui.catalogues.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.ItemModelGridBinding
import com.nice.cataloguevastra.ui.catalogues.model.ModelSheetItemUiModel

class SelectionGridAdapter(
    private val onModelClicked: (ModelSheetItemUiModel) -> Unit
) : ListAdapter<ModelSheetItemUiModel, SelectionGridAdapter.SelectionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectionViewHolder {
        val binding = ItemModelGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SelectionViewHolder(binding, onModelClicked)
    }

    override fun onBindViewHolder(holder: SelectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SelectionViewHolder(
        private val binding: ItemModelGridBinding,
        private val onModelClicked: (ModelSheetItemUiModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ModelSheetItemUiModel) = with(binding) {
            modelImage.setImageResource(item.imageRes)
            modelName.text = item.label
            root.updateSelection(item.isSelected)
            root.setOnClickListener { onModelClicked(item) }
        }

        private fun MaterialCardView.updateSelection(isSelected: Boolean) {
            val context = context
            strokeColor = if (isSelected) {
                ContextCompat.getColor(context, R.color.primaryColor)
            } else {
                ContextCompat.getColor(context, R.color.strokeLight)
            }
            strokeWidth = context.resources.getDimensionPixelSize(
                if (isSelected) R.dimen._2sdp else R.dimen._1sdp
            )
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ModelSheetItemUiModel>() {
        override fun areItemsTheSame(
            oldItem: ModelSheetItemUiModel,
            newItem: ModelSheetItemUiModel
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ModelSheetItemUiModel,
            newItem: ModelSheetItemUiModel
        ): Boolean = oldItem == newItem
    }
}
