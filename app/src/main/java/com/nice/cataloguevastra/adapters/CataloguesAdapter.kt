package com.nice.cataloguevastra.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import coil.load
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.ItemCatalogueCardBinding
import com.nice.cataloguevastra.model.CatalogueCardUiModel
import com.nice.cataloguevastra.model.CatalogueImageUiModel

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

    override fun onBindViewHolder(
        holder: CatalogueViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_SELECTION)) {
            holder.updateSelection(selectedIds.contains(getItem(position).id))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun updateSelection(newSelectedIds: Set<String>) {
        if (selectedIds == newSelectedIds) return
        val previousSelectedIds = selectedIds
        selectedIds = newSelectedIds.toSet()
        val changedIds = previousSelectedIds xor selectedIds
        changedIds.forEach { changedId ->
            val position = currentList.indexOfFirst { it.id == changedId }
            if (position != RecyclerView.NO_POSITION) {
                notifyItemChanged(position, PAYLOAD_SELECTION)
            }
        }
    }

    inner class CatalogueViewHolder(
        private val binding: ItemCatalogueCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CatalogueCardUiModel, isSelected: Boolean) = with(binding) {
            if (!item.previewImageUrl.isNullOrBlank()) {
                previewImage.load(item.previewImageUrl) {
                    placeholder(item.previewImageRes)
                    error(item.previewImageRes)
                    crossfade(true)
                }
            } else {
                previewImage.setImageResource(item.previewImageRes)
            }
            titleText.text = item.title
            subtitleText.text = item.subtitle
            updateSelection(isSelected)
            bindThumbnails(item.thumbnails)

            root.setOnClickListener { onItemClick(item) }
            selectionIcon.setOnClickListener { onSelectionClick(item) }
            deleteButton.setOnClickListener { onDeleteClick(item) }
        }

        fun updateSelection(isSelected: Boolean) = with(binding) {
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
        }

        private fun bindThumbnails(thumbnails: List<CatalogueImageUiModel>) {
            val context = binding.root.context
            val size = context.resources.getDimensionPixelSize(R.dimen._28sdp)
            val spacing = context.resources.getDimensionPixelSize(R.dimen._4sdp)
            val previewThumbnails = if (thumbnails.size <= MAX_GRID_THUMBNAILS) {
                thumbnails
            } else {
                thumbnails.take(MAX_GRID_THUMBNAILS - 1) + thumbnails.last()
            }

            binding.thumbnailStrip.removeAllViews()
            previewThumbnails.forEachIndexed { index, thumbnail ->
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
                    setPadding(context.resources.getDimensionPixelSize(R.dimen._1sdp))
                    if (!thumbnail.url.isNullOrBlank()) {
                        load(thumbnail.url) {
                            placeholder(thumbnail.imageRes ?: R.drawable.model_img)
                            error(thumbnail.imageRes ?: R.drawable.model_img)
                            crossfade(true)
                        }
                    } else {
                        setImageResource(thumbnail.imageRes ?: R.drawable.model_img)
                    }
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

    private companion object {
        const val PAYLOAD_SELECTION = "payload_selection"
        const val MAX_GRID_THUMBNAILS = 4
    }
}

private infix fun <T> Set<T>.xor(other: Set<T>): Set<T> {
    return (this - other) + (other - this)
}
