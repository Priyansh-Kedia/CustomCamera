package com.kedia.customcamera

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class CustomImageAdapter(private val context: Context, private val list: MutableList<Bitmap?>) : RecyclerView.Adapter<CustomImageAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CustomImageAdapter.ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_image, parent, false))
    }

    private var lastPosition: Int = -1

    override fun getItemCount(): Int {
        return list.size
    }

    fun addData(bitmap: Bitmap?) {
        list.add(0,bitmap)
        notifyItemInserted(0)
    }

    override fun onBindViewHolder(holder: CustomImageAdapter.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            holder.bind(list[position])
            setAnimation(holder.itemView, position)
        }
    }

    private fun setAnimation(viewToAnimate: View, position: Int) {
        if (position > lastPosition) {
            val animation =
                AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left)
            viewToAnimate.startAnimation(animation)
            lastPosition = position
        }
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        private val imageView: ImageView = itemView.findViewById(R.id.image)

        fun bind(item: Bitmap?) {
            Glide.with(context).load(item).into(imageView)
        }

    }
}