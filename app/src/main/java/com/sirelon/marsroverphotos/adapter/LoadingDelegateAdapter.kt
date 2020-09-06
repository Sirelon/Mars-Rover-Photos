package com.sirelon.marsroverphotos.adapter

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import android.view.ViewGroup
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.models.ViewType

/**
 * @author romanishin
 * @since 31.10.16 on 11:21
 */
class LoadingDelegateAdapter : ViewTypeDelegateAdapter {

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder = LoadingViewHolder(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: ViewType, payloads: List<Any>?) {

    }

    class LoadingViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.item_loading)) {
        init {
            if (itemView.layoutParams is StaggeredGridLayoutManager.LayoutParams) {
                (itemView.layoutParams as StaggeredGridLayoutManager.LayoutParams).isFullSpan = true
            }
        }
    }
}