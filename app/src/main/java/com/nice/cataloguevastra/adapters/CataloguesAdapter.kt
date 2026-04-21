package com.nice.cataloguevastra.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.ItemCatalogueCardBinding
import com.nice.cataloguevastra.model.CatalogueCardUiModel

class CataloguesAdapter(
    private val onItemClick: (CatalogueCardUiModel) -> Unit,
    private val onDeleteClick: (CatalogueCardUiModel) -> Unit,
    private val onSelectionClick: (CatalogueCardUiModel) -> Unit
) : ListAdapter<CatalogueCardUiModel, CataloguesAdapter.CatalogueViewHolder>(DiffCallback) {

    private var selectedIds: Set<String> = emptySet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatalogueViewHolder {
        val binding = ItemCatalogueCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CatalogueViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CatalogueViewHolder, position: Int) {
        holder.bind(getItem(position), selectedIds.contains(getItem(position).id))
    }

    fun updateSelection(newSelectedIds: Set<String>) {
        selectedIds = newSelectedIds.toSet()
        notifyDataSetChanged()
    }

    inner class CatalogueViewHolder(
        private val binding: ItemCatalogueCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CatalogueCardUiModel, isSelected: Boolean) = with(binding) {
            previewImage.setImageResource(item.previewImageRes)
            titleText.text = item.title
            subtitleText.text = item.subtitle
            selectionIcon.setImageResource(
                if (isSelected) {
                    R.drawable.ic_catalogue_select_checked
                } else {
                    R.drawable.ic_catalogue_select_unchecked
                }
            )

            val context = root.context
            catalogueCard.strokeColor = ContextCompat.getColor(
                context,
                if (isSelected) R.color.primaryColor else R.color.strokeLight
            )
            catalogueCard.strokeWidth = context.resources.getDimensionPixelSize(
                if (isSelected) R.dimen._2sdp else R.dimen._1sdp
            )

            bindThumbnails(item.thumbnails)

            root.setOnClickListener { onItemClick(item) }
            selectionIcon.setOnClickListener { onSelectionClick(item) }
            deleteButton.setOnClickListener { onDeleteClick(item) }
        }

        private fun bindThumbnails(thumbnails: List<Int>) {
            val context = binding.root.context
            val size = context.resources.getDimensionPixelSize(R.dimen._28sdp)
            val spacing = context.resources.getDimensionPixelSize(R.dimen._4sdp)
            val previewThumbnails = thumbnails.take(3)

            binding.thumbnailStrip.removeAllViews()
            previewThumbnails.forEachIndexed { index, thumbnailRes ->
                val cardView = MaterialCardView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        if (index < previewThumbnails.lastIndex) {
                            marginEnd = spacing
                        }
                    }
                    radius = context.resources.getDimension(R.dimen._8sdp)
                    strokeWidth = context.resources.getDimensionPixelSize(R.dimen._1sdp)
                    strokeColor = ContextCompat.getColor(context, R.color.white)
                    cardElevation = 0f
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                }

                val imageView = ImageView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setImageResource(thumbnailRes)
                    setPadding(context.resources.getDimensionPixelSize(R.dimen._1sdp))
                }

                cardView.addView(imageView)
                binding.thumbnailStrip.addView(cardView)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<CatalogueCardUiModel>() {
        override fun areItemsTheSame(
            oldItem: CatalogueCardUiModel,
            newItem: CatalogueCardUiModel
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: CatalogueCardUiModel,
            newItem: CatalogueCardUiModel
        ): Boolean = oldItem == newItem
    }
}