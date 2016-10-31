package com.sirelon.marsroverphotos.adapter

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.ViewGroup
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.inflate
import com.sirelon.marsroverphotos.loadImage
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.ViewType
import kotlinx.android.synthetic.main.item_mars_photo.view.*

/**
 * @author romanishin
 * @since 31.10.16 on 11:41
 */
class MarsPhotosDelegateAdapter : ViewTypeDelegateAdapter {

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder = MarsPhotoViewHolder(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: ViewType) {
        val marsPhotoViewHolder = holder as MarsPhotoViewHolder
        marsPhotoViewHolder.bind(item as MarsPhoto)
    }

    class MarsPhotoViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.item_mars_photo)) {

        fun bind(item: MarsPhoto) = with(itemView) {
            photo.loadImage(item.imageUrl)
            Log.d("Sirelon", "" + item)
            name.text = item.name
        }
    }

}