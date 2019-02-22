package com.sirelon.marsroverphotos.adapter.headers

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import android.view.View
import android.view.ViewGroup
import com.sirelon.marsroverphotos.adapter.ViewTypeDelegateAdapter
import com.sirelon.marsroverphotos.models.ViewType

/**
 * @author romanishin
 * @since 01.11.16 on 16:16
 */
class HeaderDelegateAdapter(val view: View) : ViewTypeDelegateAdapter {

    override fun onCreateViewHolder(parent: ViewGroup): androidx.recyclerview.widget.RecyclerView.ViewHolder = HeaderViewHolder(view)

    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, item: ViewType, payloads: List<Any>?) {

    }

    class HeaderViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView){
        init {
            if (itemView.layoutParams is androidx.recyclerview.widget.StaggeredGridLayoutManager.LayoutParams){
                (itemView.layoutParams as androidx.recyclerview.widget.StaggeredGridLayoutManager.LayoutParams).isFullSpan = true
            }
//            else if (itemView.layoutParams is GridLayoutManager.LayoutParams){
//                (itemView?.layoutParams as GridLayoutManager.LayoutParams).= true
//            }

        }
    }
}