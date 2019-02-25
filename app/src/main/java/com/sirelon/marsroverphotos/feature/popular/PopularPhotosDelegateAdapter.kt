package com.sirelon.marsroverphotos.feature.popular

import android.annotation.SuppressLint
import android.os.Build
import android.view.ViewGroup
import androidx.core.util.Pair
import androidx.recyclerview.widget.RecyclerView
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.adapter.ViewTypeDelegateAdapter
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.extensions.loadImage
import com.sirelon.marsroverphotos.feature.firebase.FirebasePhoto
import com.sirelon.marsroverphotos.models.OnModelChooseListener
import com.sirelon.marsroverphotos.models.ViewType
import kotlinx.android.synthetic.main.item_popular_photo.view.*

/**
 * @author romanishin
 * @since 31.10.16 on 11:41
 */
class PopularPhotosDelegateAdapter(val callback: OnModelChooseListener) :
    ViewTypeDelegateAdapter {

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        PopularPhotoViewHolder(parent)

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder, item: ViewType,
        payloads: List<Any>?
    ) {
        val marsPhotoViewHolder = holder as PopularPhotoViewHolder
        marsPhotoViewHolder.bind(item as FirebasePhoto)
        marsPhotoViewHolder.itemView.photo.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                callback.onModelChoose(item, Pair.create(it, it.transitionName))
            } else {
                callback.onModelChoose(item)
            }
        }
    }

    class PopularPhotoViewHolder(parent: ViewGroup) :
        RecyclerView.ViewHolder(parent.inflate(R.layout.item_popular_photo)) {

        @SuppressLint("SetTextI18n")
        fun bind(item: FirebasePhoto) = with(itemView) {
            photo.loadImage(item.imageUrl)
            txtSaveCounter.text = "${item.saveCounter}"
            txtScaleCounter.text = "${item.scaleCounter}"
            txtSeeCounter.text = "${item.seeCounter}"
            txtShareCounter.text = "${item.shareCounter}"
        }
    }

}