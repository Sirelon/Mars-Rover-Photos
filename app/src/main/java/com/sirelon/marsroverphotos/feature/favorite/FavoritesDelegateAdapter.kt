package com.sirelon.marsroverphotos.feature.favorite

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.adapter.ViewTypeDelegateAdapter
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.extensions.loadImage
import com.sirelon.marsroverphotos.models.OnModelChooseListener
import com.sirelon.marsroverphotos.models.ViewType
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_favorites.view.*

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
        RecyclerView.ViewHolder(parent.inflate(R.layout.item_favorites)), LayoutContainer {

        override val containerView: View = itemView

        fun bind(item: FavoriteItem) = with(this) {
            itemView.favoritePhoto.loadImage(item.image.imageUrl)

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