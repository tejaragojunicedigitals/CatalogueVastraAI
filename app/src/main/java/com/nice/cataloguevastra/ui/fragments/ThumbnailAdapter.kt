package com.nice.cataloguevastra.ui.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import coil.load
import androidx.recyclerview.widget.RecyclerView
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.ItemThumbnailBinding

class ThumbnailAdapter(
    private var images: List<PreviewImageUiModel>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<ThumbnailAdapter.VH>() {

    var selectedPosition = 0
        private set

    inner class VH(val binding: ItemThumbnailBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemThumbnailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val context = holder.itemView.context
        val item = images[position]
        if (!item.imageUrl.isNullOrBlank()) {
            holder.binding.imgThumb.load(item.imageUrl) {
                placeholder(item.imageRes ?: R.drawable.ic_gallery)
                error(item.imageRes ?: R.drawable.ic_gallery)
                crossfade(true)
            }
        } else {
            holder.binding.imgThumb.setImageResource(item.imageRes ?: R.drawable.ic_gallery)
        }

        if (position == selectedPosition) {
            holder.binding.cardThumb.strokeWidth =
                context.resources.getDimensionPixelSize(R.dimen._2sdp)
            holder.binding.cardThumb.strokeColor =
                ContextCompat.getColor(context, R.color.primaryColor)
        } else {
            holder.binding.cardThumb.strokeWidth =
                context.resources.getDimensionPixelSize(R.dimen._1sdp)
            holder.binding.cardThumb.strokeColor =
                ContextCompat.getColor(context, R.color.strokeLight)
        }

        holder.binding.root.setOnClickListener {
            updateSelectedPosition(position)
            onClick(position)
        }
    }

    override fun getItemCount(): Int = images.size

    fun updateImages(newImages: List<PreviewImageUiModel>) {
        images = newImages
        selectedPosition = 0
        notifyDataSetChanged()
    }

    fun updateSelectedPosition(position: Int) {
        if (position == selectedPosition || position !in images.indices) return
        val previous = selectedPosition
        selectedPosition = position
        notifyItemChanged(previous)
        notifyItemChanged(selectedPosition)
    }
}

data class PreviewImageUiModel(
    val imageUrl: String? = null,
    val imageRes: Int? = null
)
