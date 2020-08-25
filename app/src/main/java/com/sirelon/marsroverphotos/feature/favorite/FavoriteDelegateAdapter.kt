package com.sirelon.marsroverphotos.feature.favorite

import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.core.util.Pair
import androidx.recyclerview.widget.RecyclerView
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.adapter.ViewTypeDelegateAdapter
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.extensions.loadImage
import com.sirelon.marsroverphotos.models.OnModelChooseListener
import com.sirelon.marsroverphotos.models.ViewType
import com.sirelon.marsroverphotos.storage.MarsImage
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_favorite_photo.view.*
import kotlinx.android.synthetic.main.item_mars_photo.view.photo

/**
 * @author romanishin
 * @since 31.10.16 on 11:41
 */
class FavoriteDelegateAdapter(
    private val callback: OnModelChooseListener<MarsImage>,
    private val favoriteCallback: ((photo: MarsImage) -> Unit)
) :
    ViewTypeDelegateAdapter {

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        MarsPhotoViewHolder(parent)

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        item: ViewType,
        payloads: List<Any>?
    ) {
        val marsPhotoViewHolder = holder as MarsPhotoViewHolder
        marsPhotoViewHolder.bind(item as MarsImage, favoriteCallback)
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
        RecyclerView.ViewHolder(parent.inflate(R.layout.item_favorite_photo)), LayoutContainer {

        fun bind(item: MarsImage, favoriteCallback: ((photo: MarsImage) -> Unit)) = with(itemView) {
            photo.loadImage(item.imageUrl)

            favBtn.isSelected = item.favorite
            favBtn.setOnClickListener { favoriteCallback(item) }
        }

        override val containerView: View = itemView
    }

}