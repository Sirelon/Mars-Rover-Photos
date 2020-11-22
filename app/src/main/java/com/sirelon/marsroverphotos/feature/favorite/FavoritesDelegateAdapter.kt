package com.sirelon.marsroverphotos.feature.favorite

import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.adapter.ViewTypeDelegateAdapter
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.extensions.loadImage
import com.sirelon.marsroverphotos.models.OnModelChooseListener
import com.sirelon.marsroverphotos.models.ViewType

/**
 * Created on 22.08.2020 20:10 for Mars-Rover-Photos.
 */
class FavoritesDelegateAdapter(private val callback: OnModelChooseListener<ViewType>) :
    ViewTypeDelegateAdapter {

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        item: ViewType,
        payloads: List<Any>?
    ) {
        (holder as FavoriteViewHolder).bind(item as FavoriteItem)
        holder.itemView.setOnClickListener {
            RoverApplication.APP.dataManger.trackClick("favorite")
            callback.onModelChoose(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FavoriteViewHolder(parent)

    class FavoriteViewHolder(parent: ViewGroup) :
        RecyclerView.ViewHolder(parent.inflate(R.layout.item_favorites)) {

        val favoritePhoto = itemView.findViewById<ImageView>(R.id.favoritePhoto)

        fun bind(item: FavoriteItem) = with(this) {
            val imageUrl = item.image?.imageUrl
            if (imageUrl != null) {
                favoritePhoto.loadImage(imageUrl)
            } else {
                favoritePhoto.setImageResource(R.drawable.popular)
            }

            // for now it it invisible
//            itemView.totalPhotos.visibility = View.GONE
//            itemView.totalPhotos.text =
//                spannable {
//                    typeface(Typeface.BOLD) {
//                        +"Total photos: "
//                    }
//                    +"${item.totalPhotos}"
//                }.toCharSequence()
        }
    }

}