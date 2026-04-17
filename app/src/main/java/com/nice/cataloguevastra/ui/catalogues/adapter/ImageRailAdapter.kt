package com.nice.cataloguevastra.ui.catalogues.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.ItemUploadCardBinding
import com.nice.cataloguevastra.databinding.ItemVisualCardBinding
import com.nice.cataloguevastra.ui.catalogues.model.RailItemUiModel

class ImageRailAdapter(
    private val onVisualClicked: (RailItemUiModel.Visual) -> Unit,
    private val onUploadClicked: (RailItemUiModel.Upload) -> Unit
) : ListAdapter<RailItemUiModel, RecyclerView.ViewHolder>(RailDiffCallback) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is RailItemUiModel.Upload -> VIEW_TYPE_UPLOAD
            is RailItemUiModel.Visual -> VIEW_TYPE_VISUAL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_VISUAL -> VisualViewHolder(
                ItemVisualCardBinding.inflate(inflater, parent, false),
                onVisualClicked
            )

            else -> UploadViewHolder(
                ItemUploadCardBinding.inflate(inflater, parent, false),
                onUploadClicked
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is RailItemUiModel.Upload -> (holder as UploadViewHolder).bind(item)
            is RailItemUiModel.Visual -> (holder as VisualViewHolder).bind(item)
        }
    }

    class VisualViewHolder(
        private val binding: ItemVisualCardBinding,
        private val onVisualClicked: (RailItemUiModel.Visual) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RailItemUiModel.Visual) = with(binding) {
            previewImage.setImageResource(item.imageRes)
            previewCard.updateSelection(item.isSelected)
            root.setOnClickListener { onVisualClicked(item) }
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

    class UploadViewHolder(
        private val binding: ItemUploadCardBinding,
        private val onUploadClicked: (RailItemUiModel.Upload) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RailItemUiModel.Upload) = with(binding) {
            uploadTitle.text = item.title
            uploadSubtitle.text = item.subtitle
            root.setOnClickListener { onUploadClicked(item) }
        }
    }

    private object RailDiffCallback : DiffUtil.ItemCallback<RailItemUiModel>() {
        override fun areItemsTheSame(oldItem: RailItemUiModel, newItem: RailItemUiModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RailItemUiModel, newItem: RailItemUiModel): Boolean {
            return oldItem == newItem
        }
    }

    private companion object {
        const val VIEW_TYPE_VISUAL = 1
        const val VIEW_TYPE_UPLOAD = 2
    }
}
