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

    override fun onCreateViewHolder(parent: ViewGroup): androidx.recyclerview.widget.RecyclerView.ViewHolder = LoadingViewHolder(parent)

    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, item: ViewType, payloads: List<Any>?) {

    }

    class LoadingViewHolder(parent: ViewGroup) : androidx.recyclerview.widget.RecyclerView.ViewHolder(parent.inflate(R.layout.item_loading)) {
        init {
            if (itemView.layoutParams is androidx.recyclerview.widget.StaggeredGridLayoutManager.LayoutParams) {
                (itemView.layoutParams as androidx.recyclerview.widget.StaggeredGridLayoutManager.LayoutParams).isFullSpan = true
            }
        }
    }
}