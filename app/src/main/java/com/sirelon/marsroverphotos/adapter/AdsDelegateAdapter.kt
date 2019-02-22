package com.sirelon.marsroverphotos.adapter

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import android.view.ViewGroup
import com.google.android.gms.ads.AdRequest
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.models.ViewType
import kotlinx.android.synthetic.main.add_item_photo.view.*

/**
 * @author romanishin
 * @since 04.11.16 on 11:50
 */
class AdsDelegateAdapter : ViewTypeDelegateAdapter {

    val adRequest: AdRequest

    init {
        adRequest = AdRequest.Builder()
                // Nexus HUAWEI
                .addTestDevice("62BAA18402F4E51A4A00784F96F0702C")
                .build()
    }

    override fun onCreateViewHolder(parent: ViewGroup): androidx.recyclerview.widget.RecyclerView.ViewHolder = AdvertizingViewHolder(parent)

    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, item: ViewType, payloads: List<Any>?) {
        holder.itemView.ad_item_photo.loadAd(adRequest)
    }

    class AdvertizingViewHolder(parent: ViewGroup) : androidx.recyclerview.widget.RecyclerView.ViewHolder(parent.inflate(R.layout.add_item_photo)) {
        init {
            if (itemView.layoutParams is androidx.recyclerview.widget.StaggeredGridLayoutManager.LayoutParams) {
                (itemView.layoutParams as androidx.recyclerview.widget.StaggeredGridLayoutManager.LayoutParams).isFullSpan = true
            }
        }
    }
}
