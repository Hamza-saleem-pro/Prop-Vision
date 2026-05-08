package com.example.propvision

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class ImagePagerAdapter(private val context: Context, private val images: List<String>) : RecyclerView.Adapter<ImagePagerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_pager_image, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = images.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uriStr = images[position]
        try {
            holder.image.load(Uri.parse(uriStr)) {
                crossfade(true)
                placeholder(R.drawable.img1)
                error(R.drawable.img1)
            }
        } catch (e: Exception) {
            holder.image.setImageResource(R.drawable.img1)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.pagerImage)
    }
}

