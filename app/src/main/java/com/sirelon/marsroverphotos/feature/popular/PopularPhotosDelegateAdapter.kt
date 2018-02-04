package com.sirelon.marsroverphotos.feature.popular

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.adapter.ViewTypeDelegateAdapter
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
class PopularPhotosDelegateAdapter(val callback: OnModelChooseListener) : ViewTypeDelegateAdapter {

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder = PopularPhotoViewHolder(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: ViewType, payloads: MutableList<Any>?) {
        val marsPhotoViewHolder = holder as PopularPhotoViewHolder
        marsPhotoViewHolder.bind(item as MarsPhoto)
        marsPhotoViewHolder.itemView.photo.setOnClickListener {
            callback.onModelChoose(item)
        }
    }

    class PopularPhotoViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.item_popular_photo)) {

        fun bind(item: MarsPhoto) = with(itemView) {
            photo.loadImage(item.imageUrl)
            cameraName.text = item.camera.fullName
        }
    }

}