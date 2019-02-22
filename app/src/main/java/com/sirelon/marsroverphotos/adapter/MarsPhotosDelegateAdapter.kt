package com.sirelon.marsroverphotos.adapter

import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.R.id.cameraName
import com.sirelon.marsroverphotos.R.id.photo
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.extensions.loadImage
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.OnModelChooseListener
import com.sirelon.marsroverphotos.models.ViewType
import kotlinx.android.synthetic.main.item_mars_photo.view.*

/**
 * @author romanishin
 * @since 31.10.16 on 11:41
 */
class MarsPhotosDelegateAdapter(val callback: OnModelChooseListener) : ViewTypeDelegateAdapter {

    override fun onCreateViewHolder(parent: ViewGroup): androidx.recyclerview.widget.RecyclerView.ViewHolder = MarsPhotoViewHolder(parent)

    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, item: ViewType, payloads: List<Any>?) {
        val marsPhotoViewHolder = holder as MarsPhotoViewHolder
        marsPhotoViewHolder.bind(item as MarsPhoto)
        marsPhotoViewHolder.itemView.photo.setOnClickListener {
            callback.onModelChoose(item)
        }
    }

    class MarsPhotoViewHolder(parent: ViewGroup) : androidx.recyclerview.widget.RecyclerView.ViewHolder(parent.inflate(R.layout.item_mars_photo)) {

        fun bind(item: MarsPhoto) = with(itemView) {
            photo.loadImage(item.imageUrl)
            cameraName.text = item.camera.fullName
        }
    }

}