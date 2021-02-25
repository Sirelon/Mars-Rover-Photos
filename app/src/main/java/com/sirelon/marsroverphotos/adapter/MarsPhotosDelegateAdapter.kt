package com.sirelon.marsroverphotos.adapter

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.util.Pair
import androidx.recyclerview.widget.RecyclerView
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.extensions.loadImage
import com.sirelon.marsroverphotos.extensions.textOrHide
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.OnModelChooseListener
import com.sirelon.marsroverphotos.models.ViewType
import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * @author romanishin
 * @since 31.10.16 on 11:41
 */
class MarsPhotosDelegateAdapter(val callback: OnModelChooseListener<MarsImage>) :
    ViewTypeDelegateAdapter {

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        MarsPhotoViewHolder(parent)

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        item: ViewType,
        payloads: List<Any>?
    ) {
        val marsPhotoViewHolder = holder as MarsPhotoViewHolder
        marsPhotoViewHolder.bind(item as MarsImage)
        marsPhotoViewHolder.itemView.setOnClickListener {
            RoverApplication.APP.dataManger.trackClick("photo")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                callback.onModelChoose(item, Pair.create(it, it.transitionName))
            } else {
                callback.onModelChoose(item)
            }
        }
    }

    class MarsPhotoViewHolder(parent: ViewGroup) :
        RecyclerView.ViewHolder(parent.inflate(R.layout.item_mars_photo)) {

        private val photo = itemView.findViewById<ImageView>(R.id.photo)
        private val cameraName = itemView.findViewById<TextView>(R.id.cameraName)
        fun bind(item: MarsImage) = with(itemView) {
            photo.loadImage(item.imageUrl)
            val name = item.name

            cameraName.textOrHide(if (name.isNullOrBlank()) item.camera?.fullName else name)
        }
    }

}