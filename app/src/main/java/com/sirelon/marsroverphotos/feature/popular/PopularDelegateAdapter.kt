package com.sirelon.marsroverphotos.feature.popular

import android.graphics.Typeface
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.adapter.ViewTypeDelegateAdapter
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.extensions.spannable
import com.sirelon.marsroverphotos.models.OnModelChooseListener
import com.sirelon.marsroverphotos.models.ViewType
import kotlinx.android.synthetic.main.item_popular.view.*

/**
 * @author romanishin
 * @since 31.10.16 on 18:48
 */
class PopularDelegateAdapter(val callback: OnModelChooseListener) : ViewTypeDelegateAdapter {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: ViewType, payloads: MutableList<Any>?) {
        (holder as PopularViewHolder).bind(item as PopularItem, payloads)
        (holder as PopularViewHolder).itemView.setOnClickListener { callback.onModelChoose(item) }
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder = PopularViewHolder(parent)

    class PopularViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.item_popular)) {

        fun bind(item: PopularItem, payloads: MutableList<Any>?) = with(itemView) {
            itemView.totalPhotos.text =
                    spannable {
                        typeface(Typeface.BOLD) {
                            +"Total photos: "
                        }
                        +"${item.totalPhotos}"
                    }.toCharSequence()
        }
    }

}