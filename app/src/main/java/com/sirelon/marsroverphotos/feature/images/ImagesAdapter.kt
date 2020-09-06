package com.sirelon.marsroverphotos.feature.images

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.adapter.diffutils.ItemDiffCallback
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.extensions.loadImage
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.storage.MarsImageDiffCallback

///**
// * Created on 31.08.2020 23:21 for Mars-Rover-Photos.
// */
class ImagesAdapter(private val callback: ImagesAdapterClickListener) :
    PagingDataAdapter<MarsImage, MarsPhotoViewHolder>(MarsImageDiffCallback) {

    init {
        setHasStableIds(true)
    }

    override fun onBindViewHolder(holder: MarsPhotoViewHolder, position: Int) {
        val image = getItem(position) ?: return
        holder.bind(image)

        holder.favBtn.setOnClickListener {
            callback.updateFavorite(image)
        }
        RoverApplication.APP.dataManger.trackClick("photo")
        holder.itemView.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                callback.openPhoto(image, it)
            } else {
                callback.openPhoto(image)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MarsPhotoViewHolder(parent)
}

interface ImagesAdapterClickListener {
    fun updateFavorite(image: MarsImage)
    fun openPhoto(image: MarsImage, vararg sharedElements: View)
}

class MarsPhotoViewHolder(parent: ViewGroup) :
    RecyclerView.ViewHolder(parent.inflate(R.layout.item_favorite_photo)) {

    private val photo: ImageView = itemView.findViewById(R.id.photo)
    val favBtn: ImageView = itemView.findViewById(R.id.favBtn)

    fun bind(item: MarsImage) {
        photo.loadImage(item.imageUrl)

        favBtn.isSelected = item.favorite
    }
}