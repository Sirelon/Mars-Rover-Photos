package com.sirelon.marsroverphotos.feature.popular

import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.Keep
import androidx.recyclerview.widget.RecyclerView
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.adapter.ViewTypeDelegateAdapter
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.extensions.spannable
import com.sirelon.marsroverphotos.models.OnModelChooseListener
import com.sirelon.marsroverphotos.models.ViewType

/**
 * @author romanishin
 * @since 31.10.16 on 18:48
 */
@Keep
class PopularDelegateAdapter(private val callback: OnModelChooseListener<ViewType>) :
    ViewTypeDelegateAdapter {

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        item: ViewType,
        payloads: List<Any>?
    ) {
        (holder as PopularViewHolder).bind(item as PopularItem)
        holder.itemView.setOnClickListener {
            RoverApplication.APP.dataManger.trackClick("popular")
            callback.onModelChoose(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        PopularViewHolder(parent)

    @Keep
    class PopularViewHolder(parent: ViewGroup) :
        RecyclerView.ViewHolder(parent.inflate(R.layout.item_popular)) {

        val totalPhotos = parent.findViewById<TextView>(R.id.totalPhotos)

        fun bind(item: PopularItem) = with(itemView) {
            // for now it it invisible
            totalPhotos.visibility = View.GONE
            totalPhotos.text =
                spannable {
                    typeface(Typeface.BOLD) {
                        +"Total photos: "
                    }
                    +"${item.totalPhotos}"
                }.toCharSequence()
        }
    }

}