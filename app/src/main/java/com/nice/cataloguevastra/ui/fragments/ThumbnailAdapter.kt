package com.nice.cataloguevastra.ui.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nice.cataloguevastra.R
import com.nice.cataloguevastra.databinding.ItemThumbnailBinding

class ThumbnailAdapter(
    private val images: List<Int>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<ThumbnailAdapter.VH>() {

    var selectedPosition = 0

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
        holder.binding.imgThumb.setImageResource(images[position])

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
            val previous = selectedPosition
            selectedPosition = position
            notifyItemChanged(previous)
            notifyItemChanged(selectedPosition)
            onClick(position)
        }
    }

    override fun getItemCount(): Int = images.size
}
