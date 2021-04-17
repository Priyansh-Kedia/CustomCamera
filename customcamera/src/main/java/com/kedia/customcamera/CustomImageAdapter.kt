package com.kedia.customcamera

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class CustomImageAdapter(
    private val context: Context,
    private val list: MutableList<Bitmap?>,
    private val onClick: CustomAdapterClick? = null
) : RecyclerView.Adapter<CustomImageAdapter.CustomViewHolder>() {

    private var lastPosition: Int = -1
    private var showCancelOption = true

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CustomImageAdapter.CustomViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return CustomViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun addData(bitmap: Bitmap?) {
        list.add(0,bitmap)
        notifyItemInserted(0)
    }

    fun removeItem(position: Int) {
        list.removeAt(position)
        notifyItemRemoved(position)
    }

    fun clearList() {
        this.list.clear()
        notifyDataSetChanged()
    }

    fun showDeleteImage(showCancelOption: Boolean) {
        this.showCancelOption = showCancelOption
    }

    override fun onBindViewHolder(holder: CustomImageAdapter.CustomViewHolder, position: Int) {
        holder.bind(list[position])
        setAnimation(holder.itemView, position)
    }

    private fun setAnimation(viewToAnimate: View, position: Int) {
        if (position > lastPosition) {
            val animation =
                AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left)
            viewToAnimate.startAnimation(animation)
            lastPosition = position
        }
    }

    inner class CustomViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        private val imageView: ImageView = itemView.findViewById(R.id.image)
        private val cancelImage: ImageView = itemView.findViewById(R.id.cancelImage)

        fun bind(item: Bitmap?) {
            Glide.with(context).load(item).into(imageView)

            cancelImage.isVisible = showCancelOption

            cancelImage.setOnClickListener {
                onClick?.onDeleteImageClicked(adapterPosition, item)
            }
        }

    }

    interface CustomAdapterClick {
        fun onDeleteImageClicked(adapterPosition: Int, bitmap: Bitmap?)
    }
}