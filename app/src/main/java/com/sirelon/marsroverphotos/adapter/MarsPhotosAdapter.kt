package com.sirelon.marsroverphotos.adapter

import android.support.v4.util.SparseArrayCompat
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.sirelon.marsroverphotos.models.ViewType
import java.util.*

/**
 * @author romanishin
 * @since 31.10.16 on 11:32
 */
class MarsPhotosAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: ArrayList<ViewType>
    private var delegates = SparseArrayCompat<ViewTypeDelegateAdapter>()
    private val loadingItem = object : ViewType {
        override fun getViewType(): Int = AdapterConstants.LOADING
    }

    init {
        delegates.put(AdapterConstants.LOADING, LoadingDelegateAdapter())
        items = ArrayList()
        items.add(loadingItem)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return delegates.get(viewType).onCreateViewHolder(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        delegates.get(getItemViewType(position)).onBindViewHolder(holder, items[position])
    }

    override fun getItemViewType(position: Int): Int {
        return this.items[position].getViewType()
    }

    override fun getItemCount(): Int = items.size
}