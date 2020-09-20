package com.kedia.customcamera

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class CustomImageAdapter(private val context: Context, private val list: MutableList<Bitmap?>) : RecyclerView.Adapter<CustomImageAdapter.Viewholder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CustomImageAdapter.Viewholder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false)
        val viewholder = Viewholder(view)
        return viewholder
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun addData(bitmap: Bitmap?) {
        list.add(bitmap)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: CustomImageAdapter.Viewholder, position: Int) {
        if (holder is Viewholder) {
            holder.bind(list[position])
        }
    }

    inner class Viewholder(itemView: View): RecyclerView.ViewHolder(itemView) {

        private val imageView: ImageView = itemView.findViewById(R.id.image)

        fun bind(item: Bitmap?) {
            imageView.setImageBitmap(item)
        }

    }
}