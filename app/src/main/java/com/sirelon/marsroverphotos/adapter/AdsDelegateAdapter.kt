package com.sirelon.marsroverphotos.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.gms.ads.AdRequest
import com.sirelon.marsroverphotos.BuildConfig
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
        if (BuildConfig.DEBUG) {
            adRequest = AdRequest.Builder()
                // My Samsung
                .addTestDevice("FD38E8F5CAC22C44E56CD3AD33ADDD1B")
                .build()
        } else {
            adRequest = AdRequest.Builder().build()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AdvertizingViewHolder(parent)

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        item: ViewType,
        payloads: List<Any>?
    ) {
        holder.itemView.ad_item_photo.loadAd(adRequest)
    }

    class AdvertizingViewHolder(parent: ViewGroup) :
        RecyclerView.ViewHolder(parent.inflate(R.layout.add_item_photo)) {
        init {
            if (itemView.layoutParams is StaggeredGridLayoutManager.LayoutParams) {
                (itemView.layoutParams as StaggeredGridLayoutManager.LayoutParams).isFullSpan = true
            }
        }
    }
}
