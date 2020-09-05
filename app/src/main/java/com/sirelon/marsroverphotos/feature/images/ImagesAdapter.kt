package com.sirelon.marsroverphotos.feature.images

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.sirelon.marsroverphotos.adapter.diffutils.ItemDiffCallback
import com.sirelon.marsroverphotos.feature.favorite.FavoriteDelegateAdapter
import com.sirelon.marsroverphotos.storage.MarsImage

///**
// * Created on 31.08.2020 23:21 for Mars-Rover-Photos.
// */
class ImagesAdapter :
    PagingDataAdapter<MarsImage, FavoriteDelegateAdapter.MarsPhotoViewHolder>(ItemDiffCallback<MarsImage>()) {

    override fun onBindViewHolder(
        holder: FavoriteDelegateAdapter.MarsPhotoViewHolder,
        position: Int
    ) {
        val image = getItem(position) ?: return
        holder.bind(image) {

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        FavoriteDelegateAdapter.MarsPhotoViewHolder(parent)
}