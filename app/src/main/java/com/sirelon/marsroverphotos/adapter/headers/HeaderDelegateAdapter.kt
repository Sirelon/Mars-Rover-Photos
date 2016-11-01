package com.sirelon.marsroverphotos.adapter.headers

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.ViewGroup
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.adapter.ViewTypeDelegateAdapter
import com.sirelon.marsroverphotos.inflate
import com.sirelon.marsroverphotos.models.ViewType

/**
 * @author romanishin
 * @since 01.11.16 on 16:16
 */
class HeaderDelegateAdapter : ViewTypeDelegateAdapter {

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder = HeaderViewHolder(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: ViewType, payloads: MutableList<Any>?) {

    }

    class HeaderViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.item_photo_header)){
        init {
            if (itemView.layoutParams is StaggeredGridLayoutManager.LayoutParams){
                (itemView?.layoutParams as StaggeredGridLayoutManager.LayoutParams).isFullSpan = true
            }
//            else if (itemView.layoutParams is GridLayoutManager.LayoutParams){
//                (itemView?.layoutParams as GridLayoutManager.LayoutParams).= true
//            }

        }
    }
}